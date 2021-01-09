package br.com.diego.processor.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.domains.TopicMessage
import br.com.diego.processor.nats.NatsPublisher
import br.com.diego.processor.proccess.RuntimeProcessor
import io.nats.streaming.StreamingConnection
import org.slf4j.LoggerFactory

import java.util.Date
import scala.util.{Failure, Success}

object ProcessMessageActor {

  private val log = LoggerFactory.getLogger(ProcessMessageActor.getClass)

  sealed trait Command extends CborSerializable

  final case class ProcessMessageStep1(msg: TopicMessage,
                                       script: String,
                                       ifscript: Option[String],
                                       to: Option[String],
                                       to2: Option[String],
                                       replyTo: ActorRef[Command]) extends Command

  final case class ProcessMessageStep2(msg: TopicMessage,
                                       script: String,
                                       to: Option[String],
                                       to2: Option[String],
                                       replyTo: ActorRef[Command]) extends Command

  final case class ProcessedSuccess(msg: TopicMessage) extends Command
  final case class ProcessedFailure(msg: TopicMessage, error: Throwable) extends Command


  def apply(streamingConnection: StreamingConnection): Behavior[ProcessMessageActor.Command] = Behaviors.setup { context =>

    Behaviors.receiveMessage[ProcessMessageActor.Command] {
      case ProcessMessageStep1(message, script, ifscript, to, to2, replyTo) => {
        log.info(s"Processing message ${message.id}")
        ifscript match {
          case Some(ifscriptvalue) => {
            RuntimeProcessor(ifscriptvalue, message.content).process match {
              case Success(result) => {
                log.info(s"IF script processado com sucesso $result")
                context.self ! ProcessMessageStep2(message.copy(ifResult = Some(result)), script, to, to2, replyTo)
                Behaviors.same
              }
              case Failure(error) => {
                log.error(s"IF script processado com falha ${error.getMessage}")
                replyTo ! ProcessedFailure(message.copy(ifResult = Some(error.getMessage)), error)
                Behaviors.stopped
              }
            }
          }
          case _ => {
            context.self ! ProcessMessageStep2(message, script, to, to2, replyTo)
            Behaviors.same
          }
        }
      }

      case ProcessMessageStep2(message, script, to, to2, replyTo) => {
        RuntimeProcessor(script, message.content).process match {
          case Success(result) => {
            log.info(s"Processado com sucesso $result")
            val msg = message.copy(result = Some(result), deliveredTo = _getDeliverTo(message, to, to2), processed = Some(new Date().getTime))

            if(msg.deliveredTo.isDefined){
              NatsPublisher(streamingConnection, msg.deliveredTo.get, result)
            }

            replyTo ! ProcessedSuccess(msg)
            Behaviors.stopped
          }
          case Failure(error) => {
            log.error(s"Processado com falha ${error.getMessage}")
            replyTo ! ProcessedFailure(message, error)
            Behaviors.stopped
          }
        }

      }
    }
  }

  private def _getDeliverTo(topicMessage: TopicMessage, to: Option[String], to2: Option[String]): Option[String] = {
    topicMessage.ifResult match {
      case Some(ifValue) => {
        if (ifValue.equalsIgnoreCase("true") || ifValue.equalsIgnoreCase("1")) {
          to
        } else {
          to2
        }
      }
      case None => to
    }
  }

}
