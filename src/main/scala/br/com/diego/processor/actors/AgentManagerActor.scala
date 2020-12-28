package br.com.diego.processor.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import akka.util.Timeout
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.actors.AgentActor.{ResponseCreated, ResponseUpdated}
import br.com.diego.processor.domains.{ActorResponse, ScriptAgent}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

object AgentManagerActor {

  private val log = LoggerFactory.getLogger(AgentManagerActor.getClass)

  implicit val AskTimeout: Timeout = Timeout(5.seconds)

  val _ID = "ProcessorManager"

  object State {
    val empty = State()
  }

  final case class State(agents: Seq[String] = Seq()) extends CborSerializable

  sealed trait Command extends CborSerializable

  final case class Start() extends Command

  final case class AddAgent(title: String, description: String, script: String, from: String, to: String, replyTo: ActorRef[StatusReply[ActorResponse[ScriptAgent]]]) extends Command

  final case class UpdateAgent(uuid: String, title: String, description: String, script: String, to: String, replyTo: ActorRef[StatusReply[ActorResponse[ScriptAgent]]]) extends Command

  final case class ProcessMessageResponse(response: AgentActor.Command) extends Command

  final case class Show(replyTo: ActorRef[StatusReply[ActorResponse[Seq[String]]]]) extends Command

  final object NotifyAgentDetails extends Command

  sealed trait Event extends CborSerializable

  final case class ProcessorAdded(processor: ScriptAgent) extends Event

  final case class AgentUpdated(agent: ScriptAgent) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command](_ID)

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContent =>
      Behaviors.supervise(AgentManagerActor()).onFailure[Exception](SupervisorStrategy.restart)
    })
  }

  def apply(): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      val processMessageActor: ActorRef[AgentActor.Command] = context.messageAdapter(rsp => ProcessMessageResponse(rsp))

      EventSourcedBehavior[Command, Event, State](
        PersistenceId("ProcessorManager", _ID),
        State(),
        (state, command) => processCommand(state, command, context, processMessageActor),
        (state, event) => handlerEvent(state, event))
        .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 5, keepNSnapshots = 3))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))

    }
  }

  private def processCommand(state: State, command: Command, context: ActorContext[Command],
                             response: ActorRef[AgentActor.Command]): Effect[Event, State] = {
    val sharding = ClusterSharding(context.system)
    command match {
      case Start() => {
        log.info(s"Inicializando Manager ${state.agents}")
        _start(state, sharding)
      }
      case Show(replyTo) => {
        replyTo ! StatusReply.success(ActorResponse(state.agents))
        Effect.none
      }
      case AddAgent(title, description, code, from, to, replyTo) => {
        val uuid = java.util.UUID.randomUUID.toString
        val entityRef = sharding.entityRefFor(AgentActor.EntityKey, uuid)
        entityRef ! AgentActor.Create(agent = ScriptAgent.empty
          .copy(uuid = uuid, title = title, description = description, code = code, from = from, to = to), response, replyTo)
        Effect.none
      }
      case UpdateAgent(uuid, title, description, code, to, replyTo) => {
        val entityRef = sharding.entityRefFor(AgentActor.EntityKey, uuid)
        entityRef ! AgentActor.Update(agent = ScriptAgent.empty
          .copy(uuid = uuid, title = title, description = description, code = code, to = to), response, replyTo)
        Effect.none
      }
      case NotifyAgentDetails => {
        state.agents.foreach(pw => {
          val entityRef = sharding.entityRefFor(AgentActor.EntityKey, pw)
          entityRef ! AgentActor.SendCurrentDetails()
        })
        Effect.none
      }
      case messageResponse: ProcessMessageResponse =>
        messageResponse.response match {
          case ResponseCreated(uuid, scriptAgent, replyTo) => {
            Effect.persist(ProcessorAdded(scriptAgent))
              .thenReply(replyTo)(updated => {
                val entityRef = sharding.entityRefFor(AgentActor.EntityKey, uuid)
                entityRef ! AgentActor.Start()
                StatusReply.success(ActorResponse[ScriptAgent](scriptAgent))
              })
          }
          case ResponseUpdated(uuid, agentUpdated, replyTo) => {
            Effect.persist(AgentUpdated(agentUpdated))
              .thenReply(replyTo)(updated => {
                StatusReply.success(ActorResponse[ScriptAgent](agentUpdated))
              })
          }
        }
    }
  }

  def _start(state: State, sharding: ClusterSharding): Effect[Event, State] = {
    state.agents.foreach(pw => {
      val entityRef = sharding.entityRefFor(AgentActor.EntityKey, pw)
      entityRef ! AgentActor.Start()
    })
    Effect.none
  }

  private def handlerEvent(state: State, event: Event): State = {
    event match {
      case ProcessorAdded(agent) => state.copy(agents = state.agents :+ agent.uuid)
      case AgentUpdated(agent) => state.copy(agents = state.agents.filter(_ != agent.uuid) :+ agent.uuid)
    }
  }

}

