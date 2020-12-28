package br.com.diego.processor.nats

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import br.com.diego.processor.actors.AgentActor
import br.com.diego.processor.domains.TopicMessage
import br.com.diego.processor.nats.NatsSubscriber.log
import io.nats.streaming.{Message, StreamingConnection, SubscriptionOptions}
import org.slf4j.LoggerFactory

object NatsSubscriber {
  private val log = LoggerFactory.getLogger(NatsSubscriber.getClass)

  def apply(connection: StreamingConnection, queue: String, uuid: String, system: ActorSystem[_]): NatsSubscriber
  = new NatsSubscriber(connection, queue, uuid, system)
}

class NatsSubscriber(connection: StreamingConnection, queue: String, uuid: String, system: ActorSystem[_]) {
  log.info(s"Subscrevendo na fila $queue uid $uuid")
  connection.subscribe(queue, (msg: Message) => {
    log.info(s"Recebeu mensagem $msg na fila $queue")
    val entityRef = ClusterSharding(system).entityRefFor(AgentActor.EntityKey, uuid)
    entityRef ! AgentActor.Process(TopicMessage(id = msg.getSequence.toString, content = new String(msg.getData)))
    msg.ack()
  }, new SubscriptionOptions.Builder().durableName(s"durable_$uuid").manualAcks().build())
}
