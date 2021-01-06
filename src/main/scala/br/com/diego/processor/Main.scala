package br.com.diego.processor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, SpawnProtocol}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import br.com.diego.processor.actors.ManagerAgentsActor.StartSubscribers
import br.com.diego.processor.actors.{AgentActor, ManagerAgentsActor}
import br.com.diego.processor.api.{Routes, Server}
import com.typesafe.config.ConfigFactory

object Main extends App {
  implicit val system = ActorSystem[SpawnProtocol.Command](Guardian(), "NatsMessageSystem", ConfigFactory.load)
  implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("server.askTimeout"))
  implicit val scheduler = system.scheduler
  implicit val executionContext = system.executionContext
  implicit val classicSystem = system.classicSystem
}

object Guardian {
  def apply(): Behavior[SpawnProtocol.Command] = {
    Behaviors.setup { context =>
      implicit val classicSystem = context.system.classicSystem

      val httpPort = context.system.settings.config.getInt("server.http.port")

      AgentActor.init(context.system)
      ManagerAgentsActor.init(context.system)

      ClusterSharding(context.system).entityRefFor(ManagerAgentsActor.EntityKey, ManagerAgentsActor._ID) ! StartSubscribers()

      val routes = Routes()
      new Server(routes.routes, httpPort, context.system).start()

      SpawnProtocol()
    }
  }
}
