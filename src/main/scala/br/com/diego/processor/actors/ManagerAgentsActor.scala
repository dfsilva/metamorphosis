package br.com.diego.processor.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.actors.AgentActor.{ResponseCreated, ResponseUpdated}
import br.com.diego.processor.domains.{ActorResponse, AgentState}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

object ManagerAgentsActor {

  private val log = LoggerFactory.getLogger(ManagerAgentsActor.getClass)

  val _ID = "ManagerAgents"

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command](_ID)

  final case class State(agents: Seq[String] = Seq()) extends CborSerializable

  sealed trait Command extends CborSerializable
  final case class AgentResponse(response: AgentActor.Command) extends Command

  final case class StartSubscribers() extends Command
  final case class AddUpdateAgent(agentState: AgentState,
                                  replyTo: ActorRef[StatusReply[ActorResponse[AgentState]]]) extends Command

  final object SendAgentsDetails extends Command


  sealed trait Event extends CborSerializable
  final case class AgentAdded(agent: AgentState) extends Event
  final case class AgentUpdated(agent: AgentState) extends Event


  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContent =>
      Behaviors.supervise(ManagerAgentsActor()).onFailure[Exception](SupervisorStrategy.restart)
    })
  }

  def apply(): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      val agentResponseAdapter: ActorRef[AgentActor.Command] = context.messageAdapter(rsp => AgentResponse(rsp))

      EventSourcedBehavior[Command, Event, State](
        PersistenceId(_ID, _ID),
        State(),
        (state, command) => processCommand(state, command, context, agentResponseAdapter),
        (state, event) => handlerEvent(state, event))
        .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 5, keepNSnapshots = 3))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
    }
  }

  private def processCommand(state: State, command: Command, context: ActorContext[Command],
                             agentResponse: ActorRef[AgentActor.Command]): Effect[Event, State] = {
    command match {
      case StartSubscribers() => {
        log.info(s"Inicializando Manager ${state.agents}")
        _start(state, context)
      }

      case AddUpdateAgent(agent, replyTo) => {
        agent.uuid match {
          case Some(uuid) => {
            val entityRef = ClusterSharding(context.system).entityRefFor(AgentActor.EntityKey, uuid)
            entityRef ! AgentActor.Update(agent = agent, agentResponse, replyTo)
          }
          case None => {
            val uuid = java.util.UUID.randomUUID.toString
            val entityRef = ClusterSharding(context.system).entityRefFor(AgentActor.EntityKey, uuid)
            entityRef ! AgentActor.Create(agent = agent.copy(uuid = Some(uuid)), agentResponse, replyTo)
          }
        }
        Effect.none
      }

      case SendAgentsDetails => {
        state.agents.foreach(pw => {
          val entityRef = ClusterSharding(context.system).entityRefFor(AgentActor.EntityKey, pw)
          entityRef ! AgentActor.SendDetails()
        })
        Effect.none
      }

      case messageResponse: AgentResponse =>
        messageResponse.response match {
          case ResponseCreated(uuid, agent, replyTo) => {
            Effect.persist(AgentAdded(agent))
              .thenReply(replyTo)(updated => {
                val entityRef = ClusterSharding(context.system).entityRefFor(AgentActor.EntityKey, uuid)
                entityRef ! AgentActor.StartSubscriber()
                StatusReply.success(ActorResponse[AgentState](agent))
              })
          }
          case ResponseUpdated(uuid, agentUpdated, replyTo) => {
            Effect.persist(AgentUpdated(agentUpdated))
              .thenReply(replyTo)(updated => {
                StatusReply.success(ActorResponse[AgentState](agentUpdated))
              })
          }
        }
    }
  }

  def _start(state: State, context: ActorContext[ManagerAgentsActor.Command]): Effect[Event, State] = {
    state.agents.foreach(pw => {
      val entityRef = ClusterSharding(context.system).entityRefFor(AgentActor.EntityKey, pw)
      entityRef ! AgentActor.StartSubscriber()
    })
    Effect.none
  }

  private def handlerEvent(state: State, event: Event): State = {
    event match {
      case AgentAdded(agent) => state.copy(agents = state.agents :+ agent.uuid.get)
      case AgentUpdated(agent) => state.copy(agents = state.agents.filter(_ != agent.uuid.get) :+ agent.uuid.get)
    }
  }

}

