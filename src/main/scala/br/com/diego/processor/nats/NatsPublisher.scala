package br.com.diego.processor.nats

import br.com.diego.processor.nats.NatsPublisher.log
import br.com.diego.silva.nats.NatsStreamConnectionWrapper
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.jdk.FutureConverters._

object NatsPublisher {
  private val log = LoggerFactory.getLogger(NatsPublisher.getClass)

  def apply(connection: NatsStreamConnectionWrapper, queue: String, content: String): Future[String]
  = new NatsPublisher(connection, queue, content).publish
}

class NatsPublisher(connection: NatsStreamConnectionWrapper, queue: String, content: String) {

  def publish:Future[String] = {
    log.info(s"Publicando na fila na fila $queue")
    connection.publishAsync(queue, content.getBytes).asScala
  }
}
