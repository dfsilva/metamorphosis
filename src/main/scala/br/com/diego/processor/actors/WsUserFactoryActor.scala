package br.com.diego.processor.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import br.com.diego.processor.CborSerializable

object WsUserFactoryActor {

  sealed trait Command extends CborSerializable

  sealed trait Response extends Command

  final case class CreateWsCon(replyTo: ActorRef[Created]) extends Command

  final case class Created(userActor: ActorRef[WsUserActor.Command]) extends Response

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage[Command] {
        case CreateWsCon(replyTo) =>
          val userWsCon: ActorRef[WsUserActor.Command] = context.spawnAnonymous(WsUserActor())
          replyTo.tell(Created(userWsCon))
          Behaviors.same
      }
    }
}