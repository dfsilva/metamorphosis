package br.com.diego.processor.nats

import akka.actor.typed.{ActorSystem, Props, SpawnProtocol}
import akka.parboiled2.RuleTrace.Optional
import br.com.diego.processor.actors.ReceiveMessageActor
import br.com.diego.processor.nats.NatsSubscriber.log
import br.com.diego.silva.nats.NatsStreamConnectionWrapper
import io.nats.streaming.{Message, StreamingConnection, Subscription, SubscriptionOptions}
import org.slf4j.LoggerFactory

object NatsSubscriber {
  private val log = LoggerFactory.getLogger(NatsSubscriber.getClass)
  private var subscribers = Map[String, Subscription]()

  def apply(connection: NatsStreamConnectionWrapper,
            queue: String,
            uuid: String)(implicit system: ActorSystem[SpawnProtocol.Command]): NatsSubscriber = {
    if (subscribers.contains(uuid)) {
      subscribers(uuid).unsubscribe()
      val subscriber = new NatsSubscriber(connection, queue, uuid)
      subscribers = (subscribers + (uuid -> subscriber.subscription))
      subscriber
    } else {
      val subscriber = new NatsSubscriber(connection, queue, uuid)
      subscribers = (subscribers + (uuid -> subscriber.subscription))
      subscriber
    }
  }
}

class NatsSubscriber(connection: NatsStreamConnectionWrapper, queue: String, uuid: String)(implicit system: ActorSystem[SpawnProtocol.Command]) {

  log.info(s"Subscrevendo na fila $queue uid $uuid")
  val subscription = connection.subscribe(queue, (msg: Message) => {
    log.info(s"Recebeu mensagem $msg na fila $queue")
    system.tell(SpawnProtocol.Spawn(behavior = ReceiveMessageActor(uuid, msg), name = "", props = Props.empty, system.ignoreRef))
  }, java.util.Optional.of(s"durable_$uuid"))
}
