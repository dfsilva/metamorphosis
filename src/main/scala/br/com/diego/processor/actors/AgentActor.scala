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
import br.com.diego.processor.domains.{ActorResponse, AgentState, TopicMessage, Types}
import br.com.diego.processor.nats.{NatsConnectionExtension, NatsPublisher, NatsSubscriber}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.nats.streaming.{Message, StreamingConnection}
import org.slf4j.LoggerFactory

import java.util.Date
import scala.collection.immutable.Queue
import scala.concurrent.duration._


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

  final case class WaitCommand() extends Command

  final case class ProcessMessage(message: TopicMessage) extends Command

  final case class SendDetails() extends Command

  final case class ProcessMessageResponse(response: ProcessMessageActor.Command) extends Command

  sealed trait Event extends CborSerializable

  final case class Created(agent: AgentState) extends Event

  final case class Updated(agent: AgentState) extends Event

  final case class CleanProcessing() extends Event

  final case class AddedToProcess(msg: TopicMessage) extends Event

  final case class Processing(msg: TopicMessage) extends Event

  final case class ProcessedFailure(msg: TopicMessage, error: Throwable) extends Event

  final case class ProcessedSuccessfull(msg: TopicMessage) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Processor")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContent =>
      Behaviors
        .supervise(AgentActor(entityContent.entityId, NatsConnectionExtension(system).connection()))
        .onFailure[Exception](SupervisorStrategy.restart)
    })
  }

  def apply(agentId: String, streamingConnection: StreamingConnection): Behavior[Command] = {

    Behaviors.setup[Command] { context =>

      val processActor: ActorRef[ProcessMessageActor.Command] = context.messageAdapter(rsp => ProcessMessageResponse(rsp))
      val wsUserTopic: ActorRef[Topic.Command[WsUserActor.OutcommingMessage]] =
        context.spawn(Topic[WsUserActor.OutcommingMessage](WsUserActor.TopicName), WsUserActor.TopicName)

      EventSourcedBehavior[Command, Event, State](
        PersistenceId("Processor", agentId),
        State.empty,
        (state, command) => handlerCommands(agentId, state, command, streamingConnection, context, wsUserTopic, processActor),
        (state, event) => handlerEvent(state, event))
        .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 5, keepNSnapshots = 3))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
    }
  }

  private def handlerCommands(uuid: String, state: State, command: Command,
                              streamingConnection: StreamingConnection,
                              context: ActorContext[Command],
                              wsUserTopic: ActorRef[Topic.Command[WsUserActor.OutcommingMessage]],
                              processActor: ActorRef[ProcessMessageActor.Command]): Effect[Event, State] = {
    command match {

      case Create(agent, replyTo, replyTo2) =>
        Effect.persist(Created(agent))
          .thenReply(replyTo)(updated => ResponseCreated(uuid, updated.agent, replyTo2))

      case Update(agent, replyTo, replyTo2) =>
        Effect.persist(Updated(agent))
          .thenReply(replyTo)(updated => ResponseUpdated(uuid, updated.agent, replyTo2))

      case StartSubscriber() => {
        log.info(s"Iniciando subscriber $uuid")
        Effect.persist(CleanProcessing()).thenReply(context.self)(updated => {
          sendStateToUser(wsUserTopic, updated.agent.asJson)
          NatsSubscriber(streamingConnection, state.agent.from, uuid)
          ProcessMessages()
        })
      }

      case AddToProcess(message, natsMessage, replyTo) => {
        log.info(s"Adicionando mensagem para processamento $message com codigo ${state.agent.dataScript}")
        Effect.persist(AddedToProcess(message))
          .thenReply(replyTo)(updated => {
            sendStateToUser(wsUserTopic, updated.agent.asJson)
            context.self ! ProcessMessages()
            AddToProcessResponse(natsMessage)
          })
      }

      case ProcessMessages() => {
        if (!state.agent.ordered || state.agent.processing.isEmpty) {
          (state.agent.error ++ state.agent.waiting).headOption match {
            case Some(message) => Effect.reply(context.self)(ProcessMessage(message))
            case _ => Effect.none
          }
        } else {
          Effect.none
        }
      }

      case ProcessMessage(message) => {
        log.info(s"Processando mensagem $message")
        Effect.persist(Processing(message)).thenReply(context.self)(updated => {
          sendStateToUser(wsUserTopic, updated.agent.asJson)
          context.spawn(ProcessMessageActor(), s"process-message-${message.id}") ! ProcessMessageActor.ProcessMessage(message, state.agent.dataScript, state.agent.ifscript, processActor)
          if (!state.agent.ordered) {
            log.info(s"Agente não ordenado $message")
            ProcessMessages()
          } else
            WaitCommand()
        })
      }

      case WaitCommand() => {
        Effect.none
      }

      case SendDetails() => {
        log.info(s"Enviando notificacao para usuairo")
        wsUserTopic ! Topic.Publish(WsUserActor.OutcommingMessage(OutcomeWsMessage(message = state.agent.asJson, action = "set-agent-detail")))
        Effect.none
      }

      case processActor: ProcessMessageResponse =>
        processActor.response match {
          case ProcessMessageActor.ProcessedSuccess(message, result, ifResult) => {
            Effect.persist(ProcessedSuccessfull(message.copy(result = Some(result), ifResult = ifResult, processed = Some(new Date().getTime))))
              .thenReply(context.self)(updated => {
                state.agent.agentType match {
                  case Types.Conditional => {
                    ifResult match {
                      case Some(ifValue) => {
                        if (ifValue.equalsIgnoreCase("true") || ifValue.equalsIgnoreCase("1")) {
                          state.agent.to match {
                            case Some(value) => NatsPublisher(streamingConnection, value, result)
                          }
                        } else {
                          state.agent.to2 match {
                            case Some(value) => NatsPublisher(streamingConnection, value, result)
                          }
                        }
                      }
                      case _ => {
                        state.agent.to match {
                          case Some(value) => NatsPublisher(streamingConnection, value, result)
                        }
                      }
                    }
                  }
                  case _ => {
                    state.agent.to match {
                      case Some(value) => NatsPublisher(streamingConnection, value, result)
                    }
                  }
                }
                sendStateToUser(wsUserTopic, updated.agent.asJson)
                ProcessMessages()
              })
          }
          case ProcessMessageActor.ProcessedFailure(message, error) => {
            Effect.persist(ProcessedFailure(message.copy(result = Some(error.getMessage)), error))
              .thenReply(wsUserTopic)(updated => {
                Topic.Publish(WsUserActor.OutcommingMessage(OutcomeWsMessage(message = updated.agent.asJson, action = "set-agent-detail")))
              })
          }
        }
    }
  }

  def sendStateToUser(wsUserTopic: ActorRef[Topic.Command[WsUserActor.OutcommingMessage]], json: Json): Unit = {
    wsUserTopic ! Topic.Publish(WsUserActor.OutcommingMessage(OutcomeWsMessage(message = json, action = "set-agent-detail")))
  }

  private def handlerEvent(state: State, event: Event): State = {
    event match {
      case Created(agent) => state.copy(agent)
      case Updated(agent) => state.copy(state.agent.copy(
        title = agent.title,
        description = agent.description,
        dataScript = agent.dataScript,
        ifscript = agent.ifscript,
        from = agent.from,
        to = agent.to,
        to2 = agent.to2,
        agentType = agent.agentType,
        ordered = agent.ordered
      ))

      case CleanProcessing() => state.copy(state.agent.copy(
        waiting = (state.agent.processing ++ state.agent.waiting),
        processing = Queue()
      ))

      case AddedToProcess(message) => state.copy(state.agent.copy(
        error = state.agent.error.filter(_.id != message.id),
        waiting = state.agent.waiting :+ message))

      case Processing(message) => state.copy(state.agent.copy(
        processing = state.agent.processing :+ message,
        waiting = state.agent.waiting.filter(_.id != message.id),
        error = state.agent.error.filter(_.id != message.id)))

      case ProcessedSuccessfull(message) => state.copy(state.agent.copy(
        success = state.agent.success :+ message.id,
        error = state.agent.error.filter(_.id != message.id),
        processing = state.agent.processing.filter(_.id != message.id)))

      case ProcessedFailure(message, _) => state.copy(state.agent.copy(
        error = state.agent.error :+ message,
        processing = state.agent.processing.filter(_.id != message.id)))
    }
  }

}

