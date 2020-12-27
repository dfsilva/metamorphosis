package br.com.diego.processor

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import br.com.diego.processor.actors.{AgentActor, AgentManagerActor, WsUserFactoryActor}
import br.com.diego.processor.api.{Routes, Server}
import com.typesafe.config.ConfigFactory

object Main extends App {
  val system = ActorSystem[Done](Guardian(), "NatsMessageSystem", ConfigFactory.load)
}

object Guardian {
  def apply(): Behavior[Done] = {
    Behaviors.setup[Done] { context =>
      implicit val system = context.system.classicSystem
      val httpPort = context.system.settings.config.getInt("server.http.port")

      AgentActor.init(context.system)
      val processorManager = AgentManagerActor.init(context.system)
      processorManager ! AgentManagerActor.Start()

      val wsConCreatorRef = context.spawn(WsUserFactoryActor(), "wsConCreator")

      val routes = new Routes(context.system, processorManager, wsConCreatorRef)
      new Server(routes.routes, httpPort, context.system).start()
      Behaviors.receiveMessage {
        case Done =>
          Behaviors.stopped
      }
    }
  }
}
