package br.com.diego.processor.actors


import akka.actor.typed.pubsub.Topic
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.jdbc.db.SlickExtension
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import br.com.diego.processor.{CborSerializable, Metamorphosis}
import br.com.diego.processor.api.OutcomeWsMessage
import br.com.diego.processor.domains.{ActorResponse, AgentState, TopicMessage}
import br.com.diego.processor.nats.{NatsExtension, NatsSubscriber}
import br.com.diego.silva.nats.NatsStreamConnectionWrapper
import br.com.diego.processor.repo.{DeliveredMessage, DeliveredMessagesRepo}
import io.nats.streaming.StreamingConnection
import org.slf4j.LoggerFactory

import scala.collection.immutable.Queue
import scala.concurrent.Await
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

  final case class AddToProcess(message: TopicMessage, replyTo: ActorRef[Command]) extends Command

  final case class AddToProcessResponse() extends Command

  final case class ProcessMessages() extends Command

  final case class WaitCommand() extends Command

  final case class ProcessMessage(message: TopicMessage) extends Command

  final case class ProcessedSuccessResponse(msg: TopicMessage) extends Command

  final case class ProcessedFailureResponse(msg: TopicMessage, error: Throwable) extends Command

  final case class SendDetails() extends Command


  sealed trait Event extends CborSerializable

  final case class Created(agent: AgentState) extends Event

  final case class Updated(agent: AgentState) extends Event

  final case class CleanProcessing() extends Event

  final case class AddedToProcess(msg: TopicMessage) extends Event

  final case class Processing(msg: TopicMessage) extends Event

  final case class ProcessedFailure(msg: TopicMessage, error: Throwable) extends Event

  final case class ProcessedSuccess(msg: TopicMessage) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Processor")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContent =>
      Behaviors
        .supervise(AgentActor(entityContent.entityId, NatsExtension(system).connection()))
        .onFailure[Exception](SupervisorStrategy.restart)
    })
  }

  def apply(agentId: String, streamingConnection: NatsStreamConnectionWrapper): Behavior[Command] = {
    log.info("Creating agent {}..........", agentId)
    Behaviors.setup[Command] { context =>
      val wsUserTopic: ActorRef[Topic.Command[WsUserActor.TopicMessage]] = context.spawn(Topic[WsUserActor.TopicMessage](WsUserActor.TopicName), WsUserActor.TopicName)
      EventSourcedBehavior[Command, Event, State](
        PersistenceId("Processor", agentId),
        State.empty,
        (state, command) => handlerCommands(agentId, state, command, streamingConnection, context, wsUserTopic),
        (state, event) => handlerEvent(state, event))
        .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 5, keepNSnapshots = 3))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
    }
  }

  private def handlerCommands(uuid: String, state: State, command: Command,
                              streamingConnection: NatsStreamConnectionWrapper,
                              context: ActorContext[Command],
                              wsUserTopic: ActorRef[Topic.Command[WsUserActor.TopicMessage]]): Effect[Event, State] = {
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
          sendStateToUser(wsUserTopic, updated.agent)
          NatsSubscriber(streamingConnection, state.agent.from, uuid)(Metamorphosis.system)
          ProcessMessages()
        })
      }

      case AddToProcess(message, replyTo) => {
        log.info(s"Adicionando mensagem para processamento $message com codigo ${state.agent.dataScript}")
        Effect.persist(AddedToProcess(message))
          .thenReply(replyTo)(updated => {
            sendStateToUser(wsUserTopic, updated.agent)
            context.self ! ProcessMessages()
            AddToProcessResponse()
          })
      }

      case ProcessMessages() => {
        log.info(s"Processsing messages agent ${state.agent.title}")
        log.info(s"waiting ${state.agent.waiting.length}, processing ${state.agent.processing.length}")
        if (!state.agent.ordered || state.agent.processing.isEmpty) {
          (state.agent.error ++ state.agent.waiting).headOption match {
            case Some(message) => {
              Effect.persist(Processing(message)).thenReply(context.self)(updated => {
                sendStateToUser(wsUserTopic, updated.agent)
                ProcessMessage(message)
              })
            }
            case _ => Effect.none
          }
        } else {
          Effect.none
        }
      }

      case ProcessMessage(message) => {
        log.info(s"Processing message $message")
        context.spawn(ProcessMessageActor(streamingConnection), s"process-message-${message.id}") ! ProcessMessageActor.ProcessMessageStep1(message, state.agent.dataScript, state.agent.ifscript, state.agent.to, state.agent.to2, uuid)
        if (!state.agent.ordered) {
          log.info(s"Agente não ordenado $message")
          Effect.reply(context.self)(ProcessMessages())
        } else
          Effect.reply(context.self)(WaitCommand())
      }

      case WaitCommand() => {
        Effect.none
      }

      case SendDetails() => {
        log.info("Enviando detalhes para o usuario")
        wsUserTopic ! Topic.Publish(WsUserActor.TopicMessage(OutcomeWsMessage[AgentState](message = state.agent, action = "set-agent-detail")))
        Effect.none
      }

      case ProcessedSuccessResponse(message) => {
        Effect.persist(ProcessedSuccess(message))
          .thenReply(context.self)(updated => {
            val database = SlickExtension(context.system).database(context.system.settings.config.getConfig("jdbc-journal")).database
            Await.result(database.run(DeliveredMessagesRepo.add(DeliveredMessage(
              id = message.id,
              content = message.content,
              ifResult = message.ifResult.getOrElse(""),
              result = message.result.getOrElse(""),
              created = message.created,
              processed = message.processed.getOrElse(0),
              deliveredTo = message.deliveredTo,
              fromQueue = state.agent.from,
              agent = state.agent.uuid.get
            ))), 5.seconds)
            sendStateToUser(wsUserTopic, updated.agent)
            ProcessMessages()
          })
      }

      case ProcessedFailureResponse(message, error) => {
        Effect.persist(ProcessedFailure(message.copy(result = Some(error.getMessage)), error))
          .thenReply(wsUserTopic)(updated => {
            Topic.Publish(WsUserActor.TopicMessage(OutcomeWsMessage[AgentState](message = updated.agent, action = "set-agent-detail")))
          })
      }

    }
  }

  def sendStateToUser(wsUserTopic: ActorRef[Topic.Command[WsUserActor.TopicMessage]], json: AgentState): Unit = {
    wsUserTopic ! Topic.Publish(WsUserActor.TopicMessage(OutcomeWsMessage[AgentState](message = json, action = "set-agent-detail")))
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

      case ProcessedSuccess(message) => state.copy(state.agent.copy(
        success = state.agent.success :+ message.id,
        error = state.agent.error.filter(_.id != message.id),
        processing = state.agent.processing.filter(_.id != message.id)))

      case ProcessedFailure(message, _) => state.copy(state.agent.copy(
        error = state.agent.error :+ message,
        processing = state.agent.processing.filter(_.id != message.id)))
    }
  }

}

