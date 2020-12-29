package br.com.diego.processor.nats


import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import br.com.diego.processor.actors.ReceiveMessageActor
import br.com.diego.processor.nats.NatsSubscriber.log
import io.nats.streaming.{Message, StreamingConnection, SubscriptionOptions}
import org.slf4j.LoggerFactory

object NatsSubscriber {
  private val log = LoggerFactory.getLogger(NatsSubscriber.getClass)

  def apply(connection: StreamingConnection, queue: String, uuid: String, ordered: Boolean, context: ActorContext[_]): NatsSubscriber
  = new NatsSubscriber(connection, queue, uuid, ordered, context)
}

class NatsSubscriber(connection: StreamingConnection, queue: String, uuid: String, ordered: Boolean, context: ActorContext[_]) {
  log.info(s"Subscrevendo na fila $queue uid $uuid")
  connection.subscribe(queue, (msg: Message) => {
    log.info(s"Recebeu mensagem $msg na fila $queue")

    if (ordered) {
      ClusterSharding(context.system).entityRefFor(ReceiveMessageActor.EntityKey, uuid) ! ReceiveMessageActor.ProcessMessage(msg)
    } else {
      context.spawn(ReceiveMessageActor(uuid), s"ReceiveMessage_$uuid") ! ReceiveMessageActor.ProcessMessage(msg)
    }

  }, new SubscriptionOptions.Builder().durableName(s"durable_$uuid").manualAcks().build())
}
