package br.com.diego.processor.nats

import akka.actor.typed.ActorRef
import br.com.diego.processor.actors.AgentActor
import br.com.diego.processor.nats.NatsPublisher.log
import io.nats.streaming.{AckHandler, Message, StreamingConnection, SubscriptionOptions}
import org.slf4j.LoggerFactory

object NatsPublisher{
  private val log = LoggerFactory.getLogger(NatsPublisher.getClass)

  def apply(connection: StreamingConnection, queue: String, content: String): String
  = new NatsPublisher(connection, queue, content).publish
}

class NatsPublisher(connection: StreamingConnection, queue: String, content: String) {

 def publish ={
   log.info(s"Publicando na fila na fila $queue")
   connection.publish(queue, content.getBytes, (nuid: String, ex: Exception) => {

   })
 }
}
