package br.com.diego.processor.repo

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.alpakka.cassandra.CassandraSessionSettings
import akka.stream.alpakka.cassandra.scaladsl.{CassandraSession, CassandraSessionRegistry}
import br.com.diego.processor.domains.TopicMessage

import scala.concurrent.Future

case class DeliveredMessage(id: String, content: String, ifResult: String,
                            result: String,
                            created: Long,
                            processed: Long,
                            deliveredTo: String,
                            fromQueue: String,
                            agent: String)

object DeliveredMessagesRepository {
  def apply(system: ActorSystem[_]): DeliveredMessagesRepository =
    new DeliveredMessagesRepository(CassandraSessionRegistry(system).sessionFor(CassandraSessionSettings()))
}

class DeliveredMessagesRepository(session: CassandraSession) {

  import br.com.diego.processor.Main._

  def insert(agentUid: String, fromTopic: String, topicMessage: TopicMessage): Future[Done] = {
    session.executeWrite(
      """INSERT INTO
        | events_processor_tables.delivered_messages
        | (id,content,ifResult,result,created,processed,deliveredTo,fromQueue,agent)
        | VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""".stripMargin,
      topicMessage.id,
      topicMessage.content,
      topicMessage.ifResult.getOrElse(""),
      topicMessage.result.getOrElse(""),
      Long.box(topicMessage.created),
      Long.box(topicMessage.processed.getOrElse(-1L)),
      topicMessage.deliveredTo.getOrElse(""),
      fromTopic,
      agentUid)
  }

  def listByAgent(uuid: String): Future[Seq[DeliveredMessage]] = {
    session.selectAll(
      """SELECT * FROM events_processor_tables.delivered_messages
        | where agent = ? ALLOW FILTERING;""".stripMargin, uuid)
      .map(rows => rows.map(row => DeliveredMessage(
        row.getString("id"),
        row.getString("content"),
        row.getString("ifResult"),
        row.getString("result"),
        row.getLong("created"),
        row.getLong("processed"),
        row.getString("deliveredTo"),
        row.getString("fromQueue"),
        row.getString("agent")
      )))
  }

}
