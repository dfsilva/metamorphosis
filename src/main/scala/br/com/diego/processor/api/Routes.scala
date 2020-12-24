package br.com.diego.processor.api

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.pattern.StatusReply
import akka.util.Timeout
import br.com.diego.processor.actors.ProcessorActor
import br.com.diego.processor.models.MessageRequest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.Future

class Routes(system: ActorSystem[_]) extends FailFastCirceSupport with CirceJsonProtocol {

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
        cors() {
          pathPrefix("api") {
            concat(
              pathPrefix("processor") {
                concat(
                  get {
                    pathPrefix(Segment) { id: String =>
                      val entityRef = sharding.entityRefFor(ProcessorActor.EntityKey, id)
                      onSuccess(entityRef.ask(ProcessorActor.GetProcessor)) { response =>
                        complete(response)
                      }
                    }
                  },
                  post {
                    entity(as[MessageRequest]) { data =>
                      val entityRef = sharding.entityRefFor(ProcessorActor.EntityKey, s"${data.fromQueue}_${data.toQueue}")
                      val reply: Future[StatusReply[ProcessorActor.Response]] = entityRef.ask(ProcessorActor.SetProcessor(data.code, data.fromQueue, data.toQueue, _))
                      onSuccess(reply) {
                        case StatusReply.Success(response: ProcessorActor.Response) => complete(StatusCodes.OK -> response.message)
                        case StatusReply.Error(reason) => complete(StatusCodes.BadRequest -> reason)
                      }
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
}
