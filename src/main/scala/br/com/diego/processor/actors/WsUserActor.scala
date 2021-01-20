package br.com.diego.processor.actors

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.api.{IncomeWsMessage, OutcomeWsMessage}
import br.com.diego.processor.domains.AgentState
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.slf4j.LoggerFactory

object WsUserActor {

  private val log = LoggerFactory.getLogger(WsUserActor.getClass)
  val TopicName: String = "notify_user"

  sealed trait Command extends CborSerializable

  case class Connect(actorRef: ActorRef[Command]) extends Command

  case object Disconnected extends Command

  case class Fail(ex: Throwable) extends Command

  final case class IncomingMessage(message: String) extends Command

  final case class OutcommingMessage(wsMessage: OutcomeWsMessage[Json]) extends Command

  final case class TopicMessage(wsMessage: OutcomeWsMessage[AgentState]) extends Command

  def apply(actorRef: ActorRef[WsUserActor.Command],
            topic: ActorRef[Topic.Command[WsUserActor.TopicMessage]]): Behavior[WsUserActor.Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage[WsUserActor.Command] {
      case WsUserActor.TopicMessage(message) =>
        actorRef ! WsUserActor.OutcommingMessage(OutcomeWsMessage(uuid = message.uuid, action = message.action, message = message.message.asJson))
        WsUserActor(actorRef, topic)
      case WsUserActor.IncomingMessage(message) => {
        decode[IncomeWsMessage](message) match {
          case Right(msg: IncomeWsMessage) => {
            msg.action match {
              case "get-processes" => ClusterSharding(context.system).entityRefFor(ManagerAgentsActor.EntityKey, ManagerAgentsActor._ID) ! ManagerAgentsActor.SendAgentsDetails
              case "listem-topic" =>
            }
          }
          case Left(error) => {
            actorRef ! OutcommingMessage(OutcomeWsMessage(message = Map("message" -> s"Wroong message: ${error.getMessage()}").asJson, action = "error"))
          }
        }
        WsUserActor(actorRef, topic)
      }
      case WsUserActor.Disconnected => {
        log.info("Desconectando wesocket.........")
        topic ! Topic.unsubscribe(context.self)
        Behaviors.stopped
      }
    }
  }

  def apply(): Behavior[WsUserActor.Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage[WsUserActor.Command] {
      case WsUserActor.Connect(actorRef) =>
        actorRef ! OutcommingMessage(OutcomeWsMessage(message = Map("message" -> "Ws Connected}").asJson, action = "connected"))
        val topic: ActorRef[Topic.Command[WsUserActor.TopicMessage]] = context.spawn(Topic[WsUserActor.TopicMessage](TopicName), TopicName)
        topic ! Topic.Subscribe(context.self)
        WsUserActor(actorRef, topic)
    }
  }
}




