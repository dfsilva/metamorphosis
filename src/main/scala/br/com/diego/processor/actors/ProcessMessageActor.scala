package br.com.diego.processor.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import br.com.diego.processor.CborSerializable
import br.com.diego.processor.domains.TopicMessage
import br.com.diego.processor.proccess.RuntimeProcessor
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

object ProcessMessageActor {

  private val log = LoggerFactory.getLogger(ProcessMessageActor.getClass)

  sealed trait Command extends CborSerializable

  final case class ProcessMessage(msg: TopicMessage,
                                  script: String,
                                  ifscript: Option[String],
                                  replyTo: ActorRef[Command]) extends Command

  final case class ProcessedSuccess(msg: TopicMessage, result: String, ifResult: Option[String]) extends Command

  final case class ProcessedFailure(msg: TopicMessage, error: Throwable) extends Command


  def apply(): Behavior[ProcessMessageActor.Command] = Behaviors.setup { context =>

    Behaviors.receiveMessage[ProcessMessageActor.Command] {
      case ProcessMessage(message, script, ifscript, replyTo) => {
        log.info(s"Processing message ${message.id}")
        ifscript match {
          case Some(value) => {
            RuntimeProcessor(value, message.content).process match {
              case Success(result) => {
                log.info(s"IF script processado com sucesso $result")
                _processMessage(message, script, Some(result), replyTo)
              }
              case Failure(error) => {
                log.error(s"IF script processado com falha ${error.getMessage}")
                replyTo ! ProcessedFailure(message, error)
                Behaviors.stopped
              }
            }
          }
          case _ => {
            _processMessage(message, script, None, replyTo)
          }
        }
      }
    }
  }

  private def _processMessage(message: TopicMessage, script: String, ifResult: Option[String], replyTo: ActorRef[Command]): Behavior[Command] = {
    RuntimeProcessor(script, message.content).process match {
      case Success(result) => {
        log.info(s"Processado com sucesso $result")
        replyTo ! ProcessedSuccess(msg = message, result = result, ifResult = ifResult)
        Behaviors.stopped
      }
      case Failure(error) => {
        log.error(s"Processado com falha ${error.getMessage}")
        replyTo ! ProcessedFailure(msg = message, error)
        Behaviors.stopped
      }
    }
  }


}
