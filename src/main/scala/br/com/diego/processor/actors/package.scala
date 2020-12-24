package br.com.diego.processor

package object domains {

  object Processor {
    val empty = Processor(code = "", from = "", to = "")
  }

  final case class Processor(code: String, from: String, to: String) extends CborSerializable
  final case class QueueMessage(message: String) extends CborSerializable
  final case class ActorResponse(message: String) extends CborSerializable

}
