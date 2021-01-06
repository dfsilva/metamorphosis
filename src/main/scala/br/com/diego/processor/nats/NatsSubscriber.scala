package br.com.diego.processor.nats

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Props, SpawnProtocol}
import br.com.diego.processor.actors.ReceiveMessageActor
import br.com.diego.processor.nats.NatsSubscriber.log
import io.nats.streaming.{Message, StreamingConnection, Subscription, SubscriptionOptions}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object NatsSubscriber {
  private val log = LoggerFactory.getLogger(NatsSubscriber.getClass)
  private var subscribers = Map[String, Subscription]()

  def apply(connection: StreamingConnection,
            queue: String,
            uuid: String): NatsSubscriber = {
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

class NatsSubscriber(connection: StreamingConnection, queue: String, uuid: String) {
  import br.com.diego.processor.Main._
  log.info(s"Subscrevendo na fila $queue uid $uuid")
  val subscription = connection.subscribe(queue, (msg: Message) => {
    log.info(s"Recebeu mensagem $msg na fila $queue")
    val wsUserFut: Future[ActorRef[ReceiveMessageActor.Command]] = system.ask(SpawnProtocol.Spawn(behavior = ReceiveMessageActor(uuid), name = "", props = Props.empty, _))
    val receiveMessageActor = Await.result(wsUserFut, Duration.Inf)
    receiveMessageActor ! ReceiveMessageActor.ReceiveMessage(msg)
  }, new SubscriptionOptions.Builder().durableName(s"durable_$uuid").manualAcks().build())
}
