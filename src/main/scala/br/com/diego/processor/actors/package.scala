package br.com.diego.processor

import scala.collection.immutable.Queue

package object domains {

  object Status {
    val Active = "A"
    val Inactive = "I"
  }

  object ScriptAgent {
    val empty = ScriptAgent(uuid = "", title = "", description = "", code = "", from = "", to = "", status = "A", waiting = Queue(), processing = Map(),
      error = Queue(),
      success = Map())
  }

  final case class ScriptAgent(uuid: String,
                               title: String,
                               description: String,
                               code: String,
                               from: String,
                               to: String,
                               status: String = Status.Active,
                               waiting: Queue[TopicMessage] = Queue(),
                               processing: Map[String, TopicMessage] = Map(),
                               error: Queue[TopicMessage] = Queue(),
                               success: Map[String, TopicMessage] = Map()) extends CborSerializable


  final case class TopicMessage(id: String = "", content: String, result: Option[String] = None) extends CborSerializable

  final case class ActorResponse[T](body: T) extends CborSerializable

}
