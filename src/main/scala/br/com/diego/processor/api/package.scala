package br.com.diego.processor


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

  case class OutcomeWsMessage[T](uuid: String = UUID.randomUUID().toString, action: String, message: T) extends CborSerializable

  case class IncomeWsMessage(uuid: String, action: String, message: String) extends CborSerializable

}
