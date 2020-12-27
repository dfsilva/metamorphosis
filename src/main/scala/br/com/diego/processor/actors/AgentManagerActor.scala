package br.com.diego.processor.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import akka.util.Timeout
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.actors.AgentActor.ResponseCreated
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

  final case class State(agents: Seq[ScriptAgent] = Seq()) extends CborSerializable

  sealed trait Command extends CborSerializable

  final case class Start() extends Command

  final case class AddAgent(title: String, description: String, script: String, from: String, to: String, replyTo: ActorRef[StatusReply[ActorResponse[ScriptAgent]]]) extends Command

  final case class ProcessMessageResponse(response: AgentActor.Command) extends Command

  final case class Show(replyTo: ActorRef[StatusReply[ActorResponse[Seq[ScriptAgent]]]]) extends Command

  final object NotifyAgentDetails extends Command

  sealed trait Event extends CborSerializable

  final case class ProcessorAdded(processor: ScriptAgent) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command](_ID)

  def init(system: ActorSystem[_]): ActorRef[Command] = {
    ClusterSingleton(system).init(
      SingletonActor(Behaviors.supervise(AgentManagerActor()).onFailure[Exception](SupervisorStrategy.restart), _ID)
    )
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
      case NotifyAgentDetails => {
        state.agents.foreach(pw => {
          val entityRef = sharding.entityRefFor(AgentActor.EntityKey, pw.uuid)
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
        }
    }
  }

  def _start(state: State, sharding: ClusterSharding): Effect[Event, State] = {
    state.agents.foreach(pw => {
      val entityRef = sharding.entityRefFor(AgentActor.EntityKey, pw.uuid)
      entityRef ! AgentActor.Start()
    })
    Effect.none
  }

  private def handlerEvent(state: State, event: Event): State = {
    event match {
      case ProcessorAdded(agent) => state.copy(agents = state.agents :+ agent)
    }
  }

}

