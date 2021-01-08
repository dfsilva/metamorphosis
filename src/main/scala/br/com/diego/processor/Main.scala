package br.com.diego.processor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, SpawnProtocol}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.alpakka.cassandra.CassandraSessionSettings
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import akka.util.Timeout
import br.com.diego.processor.actors.ManagerAgentsActor.StartSubscribers
import br.com.diego.processor.actors.{AgentActor, ManagerAgentsActor}
import br.com.diego.processor.api.{Routes, Server}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

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

      //      implicit val classicSystem = context.system.classicSystem

      val httpPort = context.system.settings.config.getInt("server.http.port")

      AgentActor.init(context.system)
      ManagerAgentsActor.init(context.system)

      ClusterSharding(context.system).entityRefFor(ManagerAgentsActor.EntityKey, ManagerAgentsActor._ID) ! StartSubscribers()

      val routes = Routes()
      new Server(routes.routes, httpPort, context.system).start()

      createTables(context.system)

      SpawnProtocol()
    }
  }


  def createTables(system: ActorSystem[_]): Unit = {
    val session = CassandraSessionRegistry(system).sessionFor(CassandraSessionSettings())

    val keyspaceStmt =
      """
      CREATE KEYSPACE IF NOT EXISTS events_processor_tables
      WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
      """

    val successEventsTable =
      """
      CREATE TABLE IF NOT EXISTS events_processor_tables.delivered_messages (
        id text,
        content text,
        ifResult text,
        result text,
        created bigint,
        processed bigint,
        deliveredTo text,
        fromQueue text,
        agent text,
        PRIMARY KEY ((id, created, processed))
      )
      """

    log.info("Created events_processor_tables keyspace")
    session.executeWrite(keyspaceStmt)
    log.info("Created events_processor_tables.delivered_messages table")
    session.executeWrite(successEventsTable)
  }
}
