package br.com.diego.processor

import io.circe.Json

import java.util.UUID

package object api {

  case class UpdateAgent(uuid: String, title: String, description: Option[String], code: String, to: String) extends CborSerializable

  case class AddAgent(title: String, description: Option[String], code: String, from: String, to: String) extends CborSerializable

  case class PublishNats(topic: String, content: String) extends CborSerializable

  case class OutcomeWsMessage(uuid: String = UUID.randomUUID().toString, action: String, message: Json) extends CborSerializable

  case class IncomeWsMessage(uuid: String, action: String, message: String) extends CborSerializable

}
