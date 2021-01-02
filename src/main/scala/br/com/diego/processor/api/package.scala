package br.com.diego.processor

import io.circe.Json

import java.util.UUID

package object api {

  case class AddUpdateAgent(title: String,
                            description: Option[String],
                            dataScript: String,
                            ifscript: Option[String],
                            from: String,
                            to: Option[String],
                            to2: Option[String],
                            agentType: String,
                            ordered: Boolean) extends CborSerializable


  case class PublishNats(topic: String, content: String) extends CborSerializable

  case class OutcomeWsMessage(uuid: String = UUID.randomUUID().toString, action: String, message: Json) extends CborSerializable

  case class IncomeWsMessage(uuid: String, action: String, message: String) extends CborSerializable

}
