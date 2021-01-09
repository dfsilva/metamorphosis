package br.com.diego.processor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, SpawnProtocol}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.jdbc.db.SlickExtension
import akka.util.Timeout
import br.com.diego.processor.actors.ManagerAgentsActor.StartSubscribers
import br.com.diego.processor.actors.{AgentActor, ManagerAgentsActor}
import br.com.diego.processor.api.{Routes, Server}
import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

import scala.util.{Failure, Success, Try}

object Main extends App {
  implicit val system = ActorSystem[SpawnProtocol.Command](Guardian(), "NatsMessageSystem", ConfigFactory.load)
  implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("server.askTimeout"))
  implicit val scheduler = system.scheduler
  implicit val executionContext = system.executionContext
  implicit val classicSystem = system.classicSystem

}

object Guardian {
  private val log = LoggerFactory.getLogger(Guardian.getClass)

  def apply(): Behavior[SpawnProtocol.Command] = {
    Behaviors.setup { context =>

      val httpPort = context.system.settings.config.getInt("server.http.port")

      AgentActor.init(context.system)
      ManagerAgentsActor.init(context.system)

      ClusterSharding(context.system).entityRefFor(ManagerAgentsActor.EntityKey, ManagerAgentsActor._ID) ! StartSubscribers()

      val routes = Routes()
      new Server(routes.routes, httpPort, context.system).start()

      val database = SlickExtension(context.system).database(context.system.settings.config.getConfig("jdbc-journal"))

      val flyway = Flyway.configure()
        .dataSource(database.database.source.asInstanceOf[HikariCPJdbcDataSource].ds).load()

      Try(flyway.migrate()) match {
        case Success(_) => log.error("Migration success")
        case Failure(e) =>
          log.error("Migration failed", e)
      }

      SpawnProtocol()
    }
  }
}
