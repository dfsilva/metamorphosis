package br.com.diego.processor.actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import br.com.diego.processor.CborSerializable

import scala.concurrent.duration._

object ProcessorManager {

  val _ID = "processor_manager"

  object Status {
    val Active = "A"
    val Inactive = "I"
  }

  final case class Processor(id: String, status: String = Status.Active) extends CborSerializable

  final case class State(processors: Seq[Processor] = Seq()) extends CborSerializable {

  }

  object State {
    val empty = State()
  }

  sealed trait Command extends CborSerializable

  final case class Add(code: String, from: String, to: String, replyTo: ActorRef[StatusReply[Response]]) extends Command

  final case class Response(message: String, code: Int) extends CborSerializable

  sealed trait Event extends CborSerializable {
  }

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ProcessorManager")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContent =>
      ProcessorManager()
    })
  }

  def apply(): Behavior[Command] = {
    EventSourcedBehavior[Command, Event, State](
      PersistenceId("ProcessorManager", _ID),
      State(),
      (state, command) => processCommand(state, command),
      (state, event) => handlerEvent(state, event))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 5, keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
  }

  private def processCommand(state: State, command: Command): Effect[Event, State] = {
    Effect.none
  }

  private def handlerEvent(state: State, event: Event) = {
    state
  }

}

