package br.com.diego.processor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, SpawnProtocol}
import akka.actor.{Address, AddressFromURIString}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, JoinSeedNodes}
import akka.util.Timeout
import br.com.diego.processor.actors.ManagerAgentsActor.StartSubscribers
import br.com.diego.processor.actors.{AgentActor, ManagerAgentsActor}
import br.com.diego.processor.api.{Routes, Server}
import com.typesafe.config.ConfigFactory

object Metamorphosis {
  implicit val system = ActorSystem[SpawnProtocol.Command](Guardian(), "MetamorphosisSystem", ConfigFactory.load)
  implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("server.askTimeout"))
  implicit val scheduler = system.scheduler
  implicit val executionContext = system.executionContext
  implicit val classicSystem = system.classicSystem
}

object Guardian {

  def apply(): Behavior[SpawnProtocol.Command] = {
    Behaviors.setup { context =>
      val httpPort = context.system.settings.config.getInt("server.http.port")
      val seedNodes: Array[Address] =
        sys.env("SEED_NODES").split(",").map(AddressFromURIString.parse)

      Cluster(context.system).manager ! JoinSeedNodes(seedNodes)

      AgentActor.init(context.system)
      ManagerAgentsActor.init(context.system)

      if (seedNodes.length == 1) {
        ClusterSharding(context.system).entityRefFor(ManagerAgentsActor.EntityKey, ManagerAgentsActor._ID) ! StartSubscribers()
      }

      Server(Routes(), httpPort, context.system).start()
      SpawnProtocol()
    }
  }
}
