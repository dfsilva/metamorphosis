package br.com.diego.processor.repo

import br.com.diego.processor.repo.PostgresProfile.api._
import slick.lifted.ProvenShape

case class DeliveredMessage(id: String,
                            content: String,
                            ifResult: String,
                            result: String,
                            created: Long,
                            processed: Long,
                            deliveredTo: Option[String],
                            fromQueue: String,
                            agent: String)

class DeliveredMessageTable(tag: Tag) extends Table[DeliveredMessage](tag, "delivered_messages") {

  def id: Rep[String] = column[String]("id", O.PrimaryKey)

  def content: Rep[String] = column[String]("content")

  def ifResult: Rep[String] = column[String]("if_result")

  def result: Rep[String] = column[String]("result")

  def created: Rep[Long] = column[Long]("created")

  def processed: Rep[Long] = column[Long]("processed")

  def deliveredTo: Rep[Option[String]] = column[Option[String]]("delivered_to")

  def fromQueue: Rep[String] = column[String]("from_queue")

  def agent: Rep[String] = column[String]("agent")

  def * : ProvenShape[DeliveredMessage] = (id, content, ifResult, result, created, processed, deliveredTo, fromQueue, agent) <> (DeliveredMessage.tupled, DeliveredMessage.unapply)

}

object DeliveredMessagesRepo {
  val deliveredMessages = TableQuery[DeliveredMessageTable]

  def add(deliveredMessage: DeliveredMessage): DBIO[DeliveredMessage] = {
    deliveredMessages returning deliveredMessages += deliveredMessage
  }

  def loadByAgent(agentId: String): DBIO[Seq[DeliveredMessage]] = {
    deliveredMessages.filter(_.agent === agentId).sortBy(_.processed.desc).result
  }
}