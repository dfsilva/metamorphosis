package br.com.diego.processor.actors

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.api.{IncomeWsMessage, OutcomeWsMessage}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID

object WsUserActor {

  private val log: Logger = LoggerFactory.getLogger(WsUserActor.getClass)

  val TopicName: String = "notify_user"

  sealed trait Command extends CborSerializable

  case class Connect(actorRef: ActorRef[Command]) extends Command

  case object Disconnected extends Command

  case class Fail(ex: Throwable) extends Command

  final case class IncomingMessage(message: String) extends Command

  final case class OutcommingMessage(message: OutcomeWsMessage) extends Command

  def apply(actorRef: ActorRef[WsUserActor.Command],
            topic: ActorRef[Topic.Command[WsUserActor.OutcommingMessage]],
            processorManager: ActorRef[AgentManagerActor.Command]): Behavior[WsUserActor.Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage[WsUserActor.Command] {
      case WsUserActor.OutcommingMessage(message) =>
        actorRef ! WsUserActor.OutcommingMessage(message)
        WsUserActor(actorRef, topic, processorManager)
      case WsUserActor.IncomingMessage(message) => {
        decode[IncomeWsMessage](message) match {
          case Right(msg: IncomeWsMessage) => {
            msg.action match {
              case "get-processes" => processorManager ! AgentManagerActor.NotifyAgentDetails
            }
          }
          case Left(error) => {
            actorRef ! OutcommingMessage(message = OutcomeWsMessage(message = Map("message" -> s"Wroong message: ${error.getMessage()}").asJson, action = "error"))
          }
        }
        WsUserActor(actorRef, topic, processorManager)
      }
      case WsUserActor.Disconnected => {
        topic ! Topic.unsubscribe(context.self)
        Behaviors.stopped
      }
    }
  }

  def apply(processorManager: ActorRef[AgentManagerActor.Command]): Behavior[WsUserActor.Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage[WsUserActor.Command] {
      case WsUserActor.Connect(actorRef) =>
        actorRef ! OutcommingMessage(message = OutcomeWsMessage(message = Map("message" -> "Ws Connected}").asJson, action = "connected"))
        val topic: ActorRef[Topic.Command[WsUserActor.OutcommingMessage]] = context.spawn(Topic[WsUserActor.OutcommingMessage](TopicName), TopicName)
        topic ! Topic.Subscribe(context.self)
        WsUserActor(actorRef, topic, processorManager)
    }
  }
}


