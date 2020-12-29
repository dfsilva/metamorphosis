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
      waiting = Queue(),
      processing = Map(),
      error = Queue(),
      success = Map()
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
                              waiting: Queue[TopicMessage] = Queue(),
                              processing: Map[String, TopicMessage] = Map(),
                              error: Queue[TopicMessage] = Queue(),
                              success: Map[String, TopicMessage] = Map()
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
