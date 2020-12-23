package br.com.diego.processor.actors


import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import br.com.diego.processor.CborSerializable

import scala.concurrent.duration._


object ProcessorActor {

  final case class State(code: String = "", from: String = "", to: String = "") extends CborSerializable {
    def setValues(code: String, from: String, to: String) = copy(code = code, from = from, to = to)

    def updateCode(code: String) = copy(code = code)

    def toStatus(): String ={
      return s"$code, $from, $to"
    }
  }

  object State {
    val empty = State()
  }

  sealed trait Command extends CborSerializable

  final case class SetValues(code: String, from: String, to: String, replyTo: ActorRef[StatusReply[Response]]) extends Command

  final case class UpdateCode(code: String, replyTo: ActorRef[StatusReply[Response]]) extends Command

  final case class GetStatus(replyTo: ActorRef[Response]) extends Command

  final case class Response(message: String, code: Int) extends CborSerializable

  sealed trait Event extends CborSerializable {
    def processorId: String
  }

  final case class ValuesSeted(processorId: String, code: String, from: String, to: String) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Processor")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContent =>
      ProcessorActor(entityContent.entityId)
    })
  }

  def apply(processorId: String): Behavior[Command] = {
    EventSourcedBehavior[Command, Event, State](
      PersistenceId("Processor", processorId),
      State(),
      (state, command) => processCommand(processorId, state, command),
      (state, event) => handlerEvent(state, event))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 5, keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
  }

  private def processCommand(processorId: String, state: State, command: Command): Effect[Event, State] = {
    command match {
      case SetValues(code, from, to, replyTo) =>
        Effect.persist(ValuesSeted(processorId, code, from, to))
          .thenReply(replyTo)(updated => StatusReply.success(Response("sucesso", 200)))
      case GetStatus(replyTo) => Effect.reply(replyTo)(Response(state.toStatus(), 200))
    }
  }

  private def handlerEvent(state: State, event: Event) = {
    event match {
      case ValuesSeted(_, code, from, to) => state.setValues(code, from, to)
    }
  }

}

