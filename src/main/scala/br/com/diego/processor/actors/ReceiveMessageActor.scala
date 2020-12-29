package br.com.diego.processor.actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.domains.TopicMessage
import io.nats.streaming.Message

import java.util.Date

object ReceiveMessageActor {

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ReceiveMessageActor")

  sealed trait Command extends CborSerializable

  final case class ProcessMessage(msg: Message) extends Command

  final case class AgentResponse(response: AgentActor.Command) extends Command

  def init(system: ActorSystem[_], agentUuid:String):Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { _ =>
      ReceiveMessageActor(agentUuid)
    })
  }

  def apply(agentUid: String): Behavior[ReceiveMessageActor.Command] = Behaviors.setup { context =>
    val adapter: ActorRef[AgentActor.Command] = context.messageAdapter(rsp => AgentResponse(rsp))
    Behaviors.receiveMessage[ReceiveMessageActor.Command] {
      case ProcessMessage(msg) => {
        val entityRef = ClusterSharding(context.system).entityRefFor(AgentActor.EntityKey, agentUid)
        entityRef ! AgentActor.AddToProcess(TopicMessage(id = msg.getSequence.toString, content = new String(msg.getData), created = new Date().getTime), replyTo = adapter)
        ReceiveMessageActor(agentUid, msg)
      }
    }
  }

  def apply(agentUid: String, msg: Message): Behavior[ReceiveMessageActor.Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage[ReceiveMessageActor.Command] {
      case response: AgentResponse =>
        response.response match {
          case AgentActor.AddToProcessResponse() => {
            msg.ack()
            Behaviors.stopped
          }
        }
    }
  }

}
