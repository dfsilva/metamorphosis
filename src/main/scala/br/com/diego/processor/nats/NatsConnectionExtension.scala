package br.com.diego.processor.nats

import akka.actor.typed.{ActorSystem, Extension, ExtensionId}
import io.nats.client.{Connection, ConnectionListener, Nats}
import io.nats.streaming.{Options, StreamingConnection, StreamingConnectionFactory}

class NatsConnectionExtensionImpl(system: ActorSystem[_], val streamingConnection: StreamingConnection) extends Extension {
  def connection(): StreamingConnection = streamingConnection
}

object NatsConnectionExtension extends ExtensionId[NatsConnectionExtensionImpl] {
  override def createExtension(system: ActorSystem[_]): NatsConnectionExtensionImpl = {
    val config = system.settings.config;
    val options = new io.nats.client.Options.Builder().server(config.getString("nats.url"))
      .maxReconnects(-1)
      .reconnectBufferSize(-1)
      .maxControlLine(1024)
      .connectionListener((conn: Connection, eventType: ConnectionListener.Events) => {
        system.log.debug(eventType.toString)
      }).build()

    val natsConn = Nats.connect(options)

    val streamingConnection = new StreamingConnectionFactory(new Options.Builder()
      .natsConn(natsConn)
      .clusterId(config.getString("nats.cluster.id"))
      .clientId(config.getString("nats.client.id"))
      .build()).createConnection()

    new NatsConnectionExtensionImpl(system, streamingConnection)
  }

  def get(system: ActorSystem[_]): NatsConnectionExtensionImpl = apply(system)
}
