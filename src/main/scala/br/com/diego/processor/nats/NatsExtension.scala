package br.com.diego.processor.nats

import akka.actor.typed.{ActorSystem, Extension, ExtensionId}
import br.com.diego.silva.nats.NatsStreamConnectionWrapper

class NatsExtensionImpl(natsConnection: NatsStreamConnectionWrapper) extends Extension {
  def connection(): NatsStreamConnectionWrapper = natsConnection
}

object NatsExtension extends ExtensionId[NatsExtensionImpl] {
  def createExtension(system: ActorSystem[_]): NatsExtensionImpl = {
    new NatsExtensionImpl(new NatsStreamConnectionWrapper(system.settings.config.getString("nats.url"),
      system.settings.config.getString("nats.cluster.id"),
      system.settings.config.getString("nats.client.id")))
  }

  def get(system: ActorSystem[_]): NatsExtensionImpl = apply(system)
}
