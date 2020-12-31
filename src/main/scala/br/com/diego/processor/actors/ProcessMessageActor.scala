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

  final case class ProcessMessage(msg: TopicMessage, script: String, replyTo: ActorRef[Command]) extends Command

  final case class ProcessedSuccess(msg: TopicMessage, result: String) extends Command

  final case class ProcessedFailure(msg: TopicMessage, error: Throwable) extends Command


  def apply(): Behavior[ProcessMessageActor.Command] = Behaviors.setup { context =>

    Behaviors.receiveMessage[ProcessMessageActor.Command] {
      case ProcessMessage(message, script, replyTo) => {
        log.info("ProcessMessage")
        RuntimeProcessor(script, message.content).process match {
          case Success(result) => {
            log.info(s"Processado com sucesso $result")
            replyTo ! ProcessedSuccess(msg = message, result = result)
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
  }

}
