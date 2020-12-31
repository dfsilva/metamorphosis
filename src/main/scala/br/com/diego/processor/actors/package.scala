package br.com.diego.processor

import scala.collection.immutable.Queue

package object domains {

  object Status {
    val Active = "A"
    val Inactive = "I"
  }

  object Types {
    val Default = "D"
    val Conditional = "C"
  }

  object AgentState {
    val empty = AgentState(uuid = None,
      title = "",
      description = None,
      transformerScript = "",
      conditionScript = None,
      from = "",
      to = None,
      to2 = None,
      status = "A",
      agentType = Types.Default,
      queue = Queue(),
      processing = Seq(),
      error = Seq(),
      success = Seq()
    )
  }

  final case class AgentState(uuid: Option[String],
                              title: String,
                              description: Option[String],
                              transformerScript: String,
                              conditionScript: Option[String],
                              from: String,
                              to: Option[String],
                              to2: Option[String],
                              status: String = Status.Active,
                              agentType: String = Types.Default,
                              ordered: Boolean = true,
                              queue: Queue[TopicMessage] = Queue(),
                              processing: Seq[String] = Seq(),
                              error: Seq[String] = Seq(),
                              success: Seq[String] = Seq()
                             ) extends CborSerializable


  final case class TopicMessage(id: String = "",
                                content: String,
                                conditionResult: Option[String] = None,
                                result: Option[String] = None,
                                created: Long,
                                processed: Option[Long] = None,
                                deliveredTo: Option[String] = None) extends CborSerializable

  final case class ActorResponse[T](body: T) extends CborSerializable

}
