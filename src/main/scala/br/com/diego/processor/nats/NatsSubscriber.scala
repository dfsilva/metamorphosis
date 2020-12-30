package br.com.diego.processor.nats


import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import br.com.diego.processor.actors.ReceiveMessageActor
import br.com.diego.processor.nats.NatsSubscriber.log
import io.nats.streaming.{Message, StreamingConnection, SubscriptionOptions}
import org.slf4j.LoggerFactory

object NatsSubscriber {
  private val log = LoggerFactory.getLogger(NatsSubscriber.getClass)

  def apply(connection: StreamingConnection, queue: String, uuid: String, ordered: Boolean, receiveMessageActor:ActorRef[ReceiveMessageActor.Command]): NatsSubscriber
  = new NatsSubscriber(connection, queue, uuid, ordered, receiveMessageActor)
}

class NatsSubscriber(connection: StreamingConnection, queue: String, uuid: String, ordered: Boolean, receiveMessageActor:ActorRef[ReceiveMessageActor.Command]) {
  log.info(s"Subscrevendo na fila $queue uid $uuid")
  connection.subscribe(queue, (msg: Message) => {

    log.info(s"Recebeu mensagem $msg na fila $queue")
    receiveMessageActor ! ReceiveMessageActor.ReceiveMessage(msg)

//    if (ordered) {
//      ClusterSharding(context.system).entityRefFor(ReceiveMessageActor.EntityKey, uuid) ! ReceiveMessageActor.ProcessMessage(msg)
//    } else {
//      val receiveMessageActor:ActorRef[ReceiveMessageActor.Command] = context.spawn(ReceiveMessageActor(uuid), s"ReceiveMessage_$uuid")
//      receiveMessageActor ! ReceiveMessageActor.ProcessMessage(msg)
//    }

  }, new SubscriptionOptions.Builder().durableName(s"durable_$uuid").manualAcks().build())
}
