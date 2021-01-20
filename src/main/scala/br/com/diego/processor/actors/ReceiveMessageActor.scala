package br.com.diego.processor.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.domains.TopicMessage
import io.nats.streaming.Message
import org.slf4j.LoggerFactory

import java.util.Date

object ReceiveMessageActor {

  private val log = LoggerFactory.getLogger(ReceiveMessageActor.getClass)

  sealed trait Command extends CborSerializable

  final case class ReceiveMessage(msg: Message) extends Command

  final case class AgentResponse(response: AgentActor.Command) extends Command

  def apply(agentUid: String, msg: Message): Behavior[ReceiveMessageActor.Command] = Behaviors.setup { context =>
    val adapter: ActorRef[AgentActor.Command] = context.messageAdapter(rsp => AgentResponse(rsp))

    val entityRef = ClusterSharding(context.system).entityRefFor(AgentActor.EntityKey, agentUid)
    entityRef ! AgentActor.AddToProcess(message = TopicMessage(id = msg.getSequence.toString, content = new String(msg.getData), created = new Date().getTime), replyTo = adapter)

    Behaviors.receiveMessage[ReceiveMessageActor.Command] {
      case response: AgentResponse =>
        response.response match {
          case AgentActor.AddToProcessResponse() => {
            log.info(s"Message added ${msg.getSequence}")
            msg.ack()
            Behaviors.stopped
          }
        }
    }.receiveSignal {
      case (context, PostStop) =>
        context.log.info(s"Receive message actor stopped ${agentUid}")
        Behaviors.same
    }
  }

}
