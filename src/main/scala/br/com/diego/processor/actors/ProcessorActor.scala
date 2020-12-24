package br.com.diego.processor.actors


import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.domains.{ActorResponse, Processor, QueueMessage}

import scala.collection.immutable.Queue
import scala.concurrent.duration._


object ProcessorActor {

  final case class State(processor: Processor, errorMessages: Queue[QueueMessage], successMessages: Queue[QueueMessage]) extends CborSerializable {
    def setProcessor(processor: Processor) = copy(processor = processor)
    def updateCode(code: String) = copy(processor = processor.copy(code = code))

    def getCurrent = processor.toString
  }

  object State {
    val empty = State(processor = Processor.empty, errorMessages = Queue.empty, successMessages = Queue.empty)
  }

  sealed trait Command extends CborSerializable

  final case class SetProcessor(processor: Processor, replyTo: ActorRef[StatusReply[ActorResponse]]) extends Command

  final case class UpdateCode(code: String, replyTo: ActorRef[StatusReply[ActorResponse]]) extends Command

  final case class GetProcessor(replyTo: ActorRef[ActorResponse]) extends Command


  sealed trait Event extends CborSerializable {
    def processorId: String
  }

  final case class ProcessorSeted(processorId: String, processor: Processor) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Processor")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContent =>
      ProcessorActor(entityContent.entityId)
    })
  }

  def apply(processorId: String): Behavior[Command] = {
    EventSourcedBehavior[Command, Event, State](
      PersistenceId("Processor", processorId),
      State.empty,
      (state, command) => processCommand(processorId, state, command),
      (state, event) => handlerEvent(state, event))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 5, keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
  }

  private def processCommand(processorId: String, state: State, command: Command): Effect[Event, State] = {
    command match {
      case SetProcessor(processor, replyTo) =>
        Effect.persist(ProcessorSeted(processorId, processor))
          .thenReply(replyTo)(updated => StatusReply.success(ActorResponse("sucesso")))
      case GetProcessor(replyTo) => Effect.reply(replyTo)(ActorResponse(state.getCurrent))
    }
  }

  private def handlerEvent(state: State, event: Event) = {
    event match {
      case ProcessorSeted(_, processor) => state.setProcessor(processor)
    }
  }

}

