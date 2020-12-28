package br.com.diego.processor.actors


import akka.actor.typed.pubsub.Topic
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.api.OutcomeWsMessage
import br.com.diego.processor.domains.{ActorResponse, ScriptAgent, TopicMessage}
import br.com.diego.processor.nats.{NatsConnectionExtension, NatsPublisher, NatsSubscriber}
import br.com.diego.processor.proccess.RuntimeProcessor
import io.circe.generic.auto._
import io.circe.syntax._
import io.nats.streaming.StreamingConnection
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}


object AgentActor {

  private val log = LoggerFactory.getLogger(AgentActor.getClass)

  final case class State(agent: ScriptAgent) extends CborSerializable {
    def setAgent(scriptAgent: ScriptAgent) = copy(agent = scriptAgent)
  }

  object State {
    val empty = State(agent = ScriptAgent.empty)
  }

  sealed trait Command extends CborSerializable

  final case class Create(agent: ScriptAgent, replyTo: ActorRef[Command], replyTo2: ActorRef[StatusReply[ActorResponse[ScriptAgent]]]) extends Command

  final case class Update(agent: ScriptAgent, replyTo: ActorRef[Command], replyTo2: ActorRef[StatusReply[ActorResponse[ScriptAgent]]]) extends Command


  final case class ResponseCreated(uuid: String, agent: ScriptAgent, replyTo: ActorRef[StatusReply[ActorResponse[ScriptAgent]]]) extends Command

  final case class ResponseUpdated(uuid: String, agent: ScriptAgent, replyTo: ActorRef[StatusReply[ActorResponse[ScriptAgent]]]) extends Command


  final case class UpdateScriptCode(code: String, replyTo: ActorRef[StatusReply[ActorResponse[String]]]) extends Command

  final case class GetDetails(replyTo: ActorRef[ActorResponse[String]]) extends Command

  final case class Start() extends Command

  final case class Process(message: TopicMessage) extends Command

  final case class SendCurrentDetails() extends Command

  sealed trait Event extends CborSerializable

  final case class Created(agent: ScriptAgent) extends Event

  final case class Updated(agent: ScriptAgent) extends Event

  final case class ProcessedFailure(msg: TopicMessage, error: Throwable) extends Event

  final case class ProcessedSuccessfull(msg: TopicMessage) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Processor")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContent =>
      Behaviors.supervise(AgentActor(entityContent.entityId, NatsConnectionExtension(system).connection())).onFailure[Exception](SupervisorStrategy.restart)
      //      AgentActor(entityContent.entityId, NatsConnectionExtension(system).connection())
    })
  }

  def apply(processorId: String, streamingConnection: StreamingConnection): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      val wsUserTopic: ActorRef[Topic.Command[WsUserActor.OutcommingMessage]] = context.spawn(Topic[WsUserActor.OutcommingMessage](WsUserActor.TopicName), WsUserActor.TopicName)
      EventSourcedBehavior[Command, Event, State](
        PersistenceId("Processor", processorId),
        State.empty,
        (state, command) => processCommand(processorId, state, command, streamingConnection, context, wsUserTopic),
        (state, event) => handlerEvent(state, event))
        .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 5, keepNSnapshots = 3))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
    }
  }

  private def processCommand(uuid: String, state: State, command: Command,
                             streamingConnection: StreamingConnection,
                             context: ActorContext[Command],
                             wsUserTopic: ActorRef[Topic.Command[WsUserActor.OutcommingMessage]]): Effect[Event, State] = {
    command match {
      case Create(agent, replyTo, replyTo2) =>
        Effect.persist(Created(agent))
          .thenReply(replyTo)(updated => ResponseCreated(uuid, updated.agent, replyTo2))
      case Update(agent, replyTo, replyTo2) =>
        Effect.persist(Updated(agent))
          .thenReply(replyTo)(updated => ResponseUpdated(uuid, updated.agent, replyTo2))
      case GetDetails(replyTo) => Effect.reply(replyTo)(ActorResponse(state.agent.asJson.noSpaces))
      case Start() => {
        log.info(s"Inicializando Subscriber $uuid")
        NatsSubscriber(streamingConnection, state.agent.from, uuid, context.system)
        Effect.none
      }
      case Process(message) => {
        log.info(s"Processando mensagem $message com codigo ${state.agent.code}")
        RuntimeProcessor(state.agent.code, message.content).process match {
          case Success(result) => {
            log.info(s"Processado com sucesso $result")
            Effect.persist(ProcessedSuccessfull(message.copy(result = Some(result))))
              .thenReply(wsUserTopic)(updated => {
                if(!state.agent.to.isBlank)
                  NatsPublisher(streamingConnection, state.agent.to, result)
                Topic.Publish(WsUserActor.OutcommingMessage(OutcomeWsMessage(message = updated.agent.asJson, action = "set-agent-detail")))
              })
          }
          case Failure(error) => {
            log.error(s"Processado com falha ${error.getMessage}")
            Effect.persist(ProcessedFailure(message.copy(result = Some(error.getMessage)), error))
              .thenReply(wsUserTopic)(updated => {
                Topic.Publish(WsUserActor.OutcommingMessage(OutcomeWsMessage(message = updated.agent.asJson, action = "set-agent-detail")))
              })
          }
        }
      }
      case SendCurrentDetails() => {
        log.info(s"Enviando notificacao para usuairo")
        wsUserTopic ! Topic.Publish(WsUserActor.OutcommingMessage(OutcomeWsMessage(message = state.agent.asJson, action = "set-agent-detail")))
        Effect.none
      }
    }
  }

  private def handlerEvent(state: State, event: Event): State = {
    event match {
      case Created(agent) => state.setAgent(agent)
      case Updated(agent) => state.setAgent(state.agent.copy(title = agent.title, description = agent.description, code = agent.code, to = agent.to))
      case ProcessedSuccessfull(message) => state.copy(agent = state.agent.copy(success = state.agent.success + (message.id -> message)))
      case ProcessedFailure(message, error) => state.copy(agent = state.agent.copy(error = (state.agent.error :+ message)))
    }
  }

}

