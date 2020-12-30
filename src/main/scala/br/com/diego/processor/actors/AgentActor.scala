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
import br.com.diego.processor.domains.{ActorResponse, AgentState, TopicMessage}
import br.com.diego.processor.nats.{NatsConnectionExtension, NatsPublisher, NatsSubscriber}
import br.com.diego.processor.proccess.RuntimeProcessor
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.nats.streaming.{Message, StreamingConnection}
import org.slf4j.LoggerFactory

import java.util.Date
import scala.concurrent.duration._
import scala.util.{Failure, Success}


object AgentActor {

  private val log = LoggerFactory.getLogger(AgentActor.getClass)

  final case class State(agent: AgentState) extends CborSerializable

  object State {
    val empty = State(agent = AgentState.empty)
  }

  sealed trait Command extends CborSerializable

  final case class Create(agent: AgentState, replyTo: ActorRef[Command], replyTo2: ActorRef[StatusReply[ActorResponse[AgentState]]]) extends Command

  final case class Update(agent: AgentState, replyTo: ActorRef[Command], replyTo2: ActorRef[StatusReply[ActorResponse[AgentState]]]) extends Command

  final case class ResponseCreated(uuid: String, agent: AgentState, replyTo: ActorRef[StatusReply[ActorResponse[AgentState]]]) extends Command

  final case class ResponseUpdated(uuid: String, agent: AgentState, replyTo: ActorRef[StatusReply[ActorResponse[AgentState]]]) extends Command

  final case class UpdateScriptCode(code: String, replyTo: ActorRef[StatusReply[ActorResponse[String]]]) extends Command

  final case class GetDetails(replyTo: ActorRef[ActorResponse[String]]) extends Command

  final case class StartSubscriber() extends Command

  final case class AddToProcess(message: TopicMessage, natsMessage: Message, replyTo: ActorRef[Command]) extends Command

  final case class AddToProcessResponse(natsMessage: Message) extends Command

  final case class ProcessMessages() extends Command

  final case class ProcessMessage(message: TopicMessage) extends Command

  final case class SendDetails() extends Command

  sealed trait Event extends CborSerializable

  final case class Created(agent: AgentState) extends Event

  final case class Updated(agent: AgentState) extends Event

  final case class AddedToProcess(msg: TopicMessage) extends Event

  final case class InProcessing(msg: TopicMessage) extends Event

  final case class ProcessedFailure(msg: TopicMessage, error: Throwable) extends Event

  final case class ProcessedSuccessfull(msg: TopicMessage) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Processor")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContent =>
      Behaviors.supervise(AgentActor(entityContent.entityId, NatsConnectionExtension(system).connection())).onFailure[Exception](SupervisorStrategy.restart)
    })
  }

  def apply(processorId: String, streamingConnection: StreamingConnection): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      val wsUserTopic: ActorRef[Topic.Command[WsUserActor.OutcommingMessage]] =
        context.spawn(Topic[WsUserActor.OutcommingMessage](WsUserActor.TopicName), WsUserActor.TopicName)

      EventSourcedBehavior[Command, Event, State](
        PersistenceId("Processor", processorId),
        State.empty,
        (state, command) => handlerCommands(processorId, state, command, streamingConnection, context, wsUserTopic),
        (state, event) => handlerEvent(state, event))
        .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 5, keepNSnapshots = 3))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
    }
  }

  private def handlerCommands(uuid: String, state: State, command: Command,
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

      case StartSubscriber() => {
        log.info(s"Inicializando Subscriber $uuid")
        NatsSubscriber(streamingConnection, state.agent.from, uuid, state.agent.ordered,
          context.spawn(ReceiveMessageActor(uuid), s"ReceiveMessage_$uuid"))
        context.self ! ProcessMessages()
        Effect.none
      }

      case AddToProcess(message, natsMessage, replyTo) => {
        log.info(s"Adicionando mensagem para processamento $message com codigo ${state.agent.transformerScript}")
        Effect.persist(AddedToProcess(message))
          .thenReply(replyTo)(updated => {
            sendStateToUser(wsUserTopic, updated.agent.asJson)
            context.self ! ProcessMessages()
            AddToProcessResponse(natsMessage)
          })
      }

      case ProcessMessages() => {
        val messagesToProcess = state.agent.error ++ state.agent.processing.values ++ state.agent.waiting
        messagesToProcess match {
          case next +: xl => {
            Effect.persist(InProcessing(next)).thenReply(context.self)(updated => {
              sendStateToUser(wsUserTopic, updated.agent.asJson)
              context.self ! ProcessMessages()
              ProcessMessage(next)
            })
          }
          case _ => {
              Effect.none
          }
        }
      }

      case ProcessMessage(message) => {
        log.info(s"Processando mensagem $message com codigo ${state.agent.transformerScript}")
        RuntimeProcessor(state.agent.transformerScript, message.content).process match {
          case Success(result) => {
            log.info(s"Processado com sucesso $result")
            Effect.persist(ProcessedSuccessfull(message.copy(result = Some(result))))
              .thenReply(context.self)(updated => {
                state.agent.to match {
                  case Some(value) => NatsPublisher(streamingConnection, value, result)
                }
                sendStateToUser(wsUserTopic, updated.agent.asJson)
                ProcessMessages()
              })
          }
          case Failure(error) => {
            log.error(s"Processado com falha ${error.getMessage}")
            Effect.persist(ProcessedFailure(message.copy(result = Some(error.getMessage)), error))
              .thenReply(context.self)(updated => {
                sendStateToUser(wsUserTopic, updated.agent.asJson)
                ProcessMessages()
              })
          }
        }
      }
      case SendDetails() => {
        log.info(s"Enviando notificacao para usuairo")
        wsUserTopic ! Topic.Publish(WsUserActor.OutcommingMessage(OutcomeWsMessage(message = state.agent.asJson, action = "set-agent-detail")))
        Effect.none
      }
    }
  }

  def sendStateToUser(wsUserTopic: ActorRef[Topic.Command[WsUserActor.OutcommingMessage]], json:Json): Unit ={
    wsUserTopic ! Topic.Publish(WsUserActor.OutcommingMessage(OutcomeWsMessage(message = json, action = "set-agent-detail")))
  }

  private def handlerEvent(state: State, event: Event): State = {
    event match {
      case Created(agent) => state.copy(agent)
      case Updated(agent) => state.copy(agent = state.agent.copy(
        title = agent.title,
        description = agent.description,
        transformerScript = agent.transformerScript,
        conditionScript = agent.conditionScript,
        from = agent.from,
        to = agent.to,
        to2 = agent.to2,
        agentType = agent.agentType,
        ordered = agent.ordered
      ))

      case AddedToProcess(topicMessage) => state.copy(agent = state.agent.copy(
        waiting = state.agent.waiting.filter(_.id != topicMessage.id) :+ topicMessage,
        error = state.agent.error.filter(_.id != topicMessage.id)))

      case InProcessing(topicMessage) => state.copy(agent = state.agent.copy(
        waiting = state.agent.waiting.filter(_.id != topicMessage.id),
        processing = state.agent.processing + (topicMessage.id -> topicMessage),
        error = state.agent.error.filter(_.id != topicMessage.id)))

      case ProcessedSuccessfull(message) => state.copy(agent = state.agent.copy(
        success = state.agent.success + (message.id -> message.copy(processed = Some(new Date().getTime))),
        processing = state.agent.processing - message.id))
      case ProcessedFailure(message, error) => state.copy(agent = state.agent.copy(error = (state.agent.error :+ message),
        processing = state.agent.processing - message.id))
    }
  }

}

