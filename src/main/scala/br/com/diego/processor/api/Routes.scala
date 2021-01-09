package br.com.diego.processor.api

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Props, SpawnProtocol}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.util.FastFuture
import akka.pattern.StatusReply
import akka.persistence.jdbc.db.SlickExtension
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import br.com.diego.processor.actors.WsUserActor._
import br.com.diego.processor.actors.{AgentActor, ManagerAgentsActor, WsUserActor}
import br.com.diego.processor.domains.{ActorResponse, AgentState}
import br.com.diego.processor.nats.{NatsConnectionExtension, NatsPublisher}
import br.com.diego.processor.repo.DeliveredMessagesRepo
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.slf4j.LoggerFactory

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}

object Routes {
  def apply() = new Routes()
}

class Routes() extends FailFastCirceSupport with CirceJsonProtocol {

  import akka.http.scaladsl.server._
  import Directives._
  import br.com.diego.processor.Main._
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
  import br.com.diego.processor.repo.PostgresProfile._
  import io.circe.generic.auto._

  private lazy val log = LoggerFactory.getLogger(getClass)
  private val sharding = ClusterSharding(system)
  private val database = SlickExtension(system).database(system.settings.config.getConfig("jdbc-journal")).database

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
                  pathPrefix(Segment) { id: String =>
                    concat(
                      pathPrefix("messages") {
                        get {
                          complete(database.run(DeliveredMessagesRepo.loadByAgent(id)))
                        }
                      },
                      get {
                        val entityRef = sharding.entityRefFor(AgentActor.EntityKey, id)
                        onSuccess(entityRef.ask(AgentActor.GetDetails)) { response =>
                          complete(response)
                        }
                      }
                    )
                  },
                  pathPrefix(Segment) { uuid =>
                    post {
                      entity(as[AddUpdateAgent]) { data =>
                        val processorManager = sharding.entityRefFor(ManagerAgentsActor.EntityKey, ManagerAgentsActor._ID)
                        val reply: Future[StatusReply[ActorResponse[AgentState]]] =
                          processorManager.ask(ManagerAgentsActor.AddUpdateAgent(
                            AgentState(
                              uuid = Some(uuid),
                              title = data.title,
                              description = data.description,
                              dataScript = data.dataScript,
                              ifscript = data.ifscript,
                              from = data.from,
                              to = data.to,
                              to2 = data.to2,
                              agentType = data.agentType,
                              ordered = data.ordered
                            ), _))

                        onSuccess(reply) {
                          case StatusReply.Success(response: ActorResponse[AgentState]) => complete(StatusCodes.OK -> response.body)
                          case StatusReply.Error(reason) => complete(StatusCodes.BadRequest -> reason)
                        }
                      }
                    }
                  },
                  post {
                    entity(as[AddUpdateAgent]) { data =>
                      val processorManager = sharding.entityRefFor(ManagerAgentsActor.EntityKey, ManagerAgentsActor._ID)

                      val reply: Future[StatusReply[ActorResponse[AgentState]]] = processorManager.ask(ManagerAgentsActor.AddUpdateAgent(
                        AgentState(
                          uuid = None,
                          title = data.title,
                          description = data.description,
                          dataScript = data.dataScript,
                          ifscript = data.ifscript,
                          from = data.from,
                          to = data.to,
                          to2 = data.to2,
                          agentType = data.agentType,
                          ordered = data.ordered
                        ), _))

                      onSuccess(reply) {
                        case StatusReply.Success(response: ActorResponse[AgentState]) => complete(StatusCodes.OK -> response.body)
                        case StatusReply.Error(reason) => complete(StatusCodes.BadRequest -> reason)
                      }
                    }
                  },
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

    val wsUserFut: Future[ActorRef[WsUserActor.Command]] = system.ask(SpawnProtocol.Spawn(behavior = WsUserActor(), name = "", props = Props.empty, _))
    val wsUser = Await.result(wsUserFut, Duration.Inf)

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
          log.info("Disconected")
        }
      }, failureMatcher = {
        case Fail(ex) => ex
      }, bufferSize = 8, overflowStrategy = OverflowStrategy.fail)
        .map {
          case c: OutcommingMessage => {
            log.info("Enviando mensagem {} para usuario", c.message.asJson.noSpaces)
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
