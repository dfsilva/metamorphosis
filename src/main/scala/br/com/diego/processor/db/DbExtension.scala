package br.com.diego.processor.db

import akka.actor.typed.{ActorSystem, Extension, ExtensionId}
import akka.persistence.jdbc.db.SlickExtension
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

import java.sql.Connection
import scala.util.{Failure, Success, Try}

class DbExtensionImpl(system: ActorSystem[_], val datasource: HikariDataSource) extends Extension {

  def connection(): Connection = datasource.getConnection
}

object DbExtension extends ExtensionId[DbExtensionImpl] {

  private val log = LoggerFactory.getLogger(DbExtension.getClass)

  override def createExtension(system: ActorSystem[_]): DbExtensionImpl = {

    val db = getDb(system);
    val pool = getDataSource(db);

    val flyway = Flyway.configure().dataSource(pool).load()
    Try(flyway.migrate()) match {
      case Success(_) => log.info("Migration success")
      case Failure(e) =>
        log.error("Migration failed", e)
    }

    system.whenTerminated.andThen(_ => {
      db.close()
    })(system.executionContext)

    new DbExtensionImpl(system, pool)
  }

  private def getDb(system: ActorSystem[_]): Database = {
    SlickExtension(system).database(system.settings.config.getConfig("jdbc-journal")).database
  }

  private def getDataSource(db: Database): HikariDataSource = {
    db.source.asInstanceOf[HikariCPJdbcDataSource].ds
  }

  def get(system: ActorSystem[_]): DbExtensionImpl = apply(system)
}
