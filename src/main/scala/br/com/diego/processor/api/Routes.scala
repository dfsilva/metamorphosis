package br.com.diego.processor.api

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.util.FastFuture
import akka.pattern.StatusReply
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.util.Timeout
import br.com.diego.processor.actors.WsUserActor._
import br.com.diego.processor.actors.{AgentActor, AgentManagerActor, WsUserActor, WsUserFactoryActor}
import br.com.diego.processor.domains.{ActorResponse, ScriptAgent}
import br.com.diego.processor.nats.{NatsConnectionExtension, NatsPublisher}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class Routes(system: ActorSystem[_], wsConCreator: ActorRef[WsUserFactoryActor.Command])
  extends FailFastCirceSupport with CirceJsonProtocol {

  import akka.http.scaladsl.server._
  import Directives._
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
  import io.circe.generic.auto._

  lazy val log = system.log

  implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("server.askTimeout"))
  implicit val scheduler = system.scheduler
  implicit val executionContext = system.executionContext
  implicit val classicSystem = system.classicSystem

  private val sharding = ClusterSharding(system)

  val errorHandler = ExceptionHandler {
    case ex =>
      extractUri { uri =>
        system.log.error(ex.getMessage, ex)
        complete(HttpResponse(InternalServerError, entity = s"Ocorreu algum erro inesperado: ${ex.getMessage} ao acessar a uri: $uri"))
      }
  }

  val routes: Route =
    handleExceptions(errorHandler) {
      concat(
        path("ws") {
          log.info("criando o websocket")
          handleWebSocketMessages(wsUser())
        },
        cors() {
          pathPrefix("api") {
            concat(
              pathPrefix("agent") {
                concat(
                  get {
                    pathPrefix(Segment) { id: String =>
                      val entityRef = sharding.entityRefFor(AgentActor.EntityKey, id)
                      onSuccess(entityRef.ask(AgentActor.GetDetails)) { response =>
                        complete(response)
                      }
                    }
                  },
                  get {
                    val processorManager = sharding.entityRefFor(AgentManagerActor.EntityKey, AgentManagerActor._ID)
                    val reply: Future[StatusReply[ActorResponse[Seq[String]]]] = processorManager.ask(AgentManagerActor.Show(_))
                    onSuccess(reply) {
                      case StatusReply.Success(response: ActorResponse[Seq[ScriptAgent]]) => complete(StatusCodes.OK -> response.body)
                      case StatusReply.Error(reason) => complete(StatusCodes.BadRequest -> reason)
                    }
                  },
                  post {
                    entity(as[AddAgent]) { data =>
                      val processorManager = sharding.entityRefFor(AgentManagerActor.EntityKey, AgentManagerActor._ID)
                      val reply: Future[StatusReply[ActorResponse[ScriptAgent]]] =
                        processorManager.ask(AgentManagerActor.AddAgent(data.title, data.description.orNull, data.code, data.from, data.to.getOrElse(""), _))
                      onSuccess(reply) {
                        case StatusReply.Success(response: ActorResponse[ScriptAgent]) => complete(StatusCodes.OK -> response.body)
                        case StatusReply.Error(reason) => complete(StatusCodes.BadRequest -> reason)
                      }
                    }
                  },
                  pathPrefix(Segment) { uuid =>
                    post {
                      entity(as[UpdateAgent]) { data =>
                        val processorManager = sharding.entityRefFor(AgentManagerActor.EntityKey, AgentManagerActor._ID)
                        val reply: Future[StatusReply[ActorResponse[ScriptAgent]]] =
                          processorManager.ask(AgentManagerActor.UpdateAgent(data.uuid, data.title, data.description.orNull, data.code, data.to.getOrElse(""), _))
                        onSuccess(reply) {
                          case StatusReply.Success(response: ActorResponse[ScriptAgent]) => complete(StatusCodes.OK -> response.body)
                          case StatusReply.Error(reason) => complete(StatusCodes.BadRequest -> reason)
                        }
                      }
                    }
                  }
                )
              },

              pathPrefix("nats") {
                concat(
                  post {
                    entity(as[PublishNats]) { data =>
                      complete(FastFuture.successful(NatsPublisher(NatsConnectionExtension(system).connection(), data.topic, data.content)))
                    }
                  },
                )
              },
            )
          }
        },
        get {
          (pathEndOrSingleSlash & redirectToTrailingSlashIfMissing(StatusCodes.TemporaryRedirect)) {
            getFromResource("web/index.html")
          } ~ {
            getFromResourceDirectory("web")
          }
        }
      )
    }

  private def wsUser(): Flow[Message, Message, NotUsed] = {

    import io.circe.generic.auto._
    import io.circe.syntax._

    val wsConCreated: WsUserFactoryActor.Created = Await.result(wsConCreator.ask(replyTo => WsUserFactoryActor.CreateWsCon(replyTo)), 2.seconds)
    val wsUser = wsConCreated.userActor

    val sink: Sink[Message, NotUsed] =
      Flow[Message].collect {
        case TextMessage.Strict(string) => {
          log.info("Recebido mensagem de texto {}", string)
          IncomingMessage(string)
        }
      }
        .to(ActorSink.actorRef[WsUserActor.Command](ref = wsUser, onCompleteMessage = Disconnected, onFailureMessage = Fail))

    val source: Source[Message, NotUsed] =
      ActorSource.actorRef[WsUserActor.Command](completionMatcher = {
        case Disconnected => {
          log.debug("Disconected")
        }
      }, failureMatcher = {
        case Fail(ex) => ex
      }, bufferSize = 8, overflowStrategy = OverflowStrategy.fail)
        .map {
          case c: OutcommingMessage => {
            log.debug("Enviando mensagem {} para {}", c.message)
            TextMessage.Strict(c.message.asJson.noSpaces)
          }
          case _ => {
            log.error("Mensagem nao reconhecida {}")
            TextMessage.Strict("Mensagem não reconhecida")
          }
        }
        .mapMaterializedValue({ wsHandler =>
          log.debug("Conectando")
          wsUser ! WsUserActor.Connect(wsHandler)
          NotUsed
        })
        .keepAlive(maxIdle = 10.seconds, () => TextMessage.Strict("{\"message\": \"keep-alive\"}"))

    Flow.fromSinkAndSource(sink, source)
  }
}
