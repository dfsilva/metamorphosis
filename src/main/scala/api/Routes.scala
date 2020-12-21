package api

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.util.FastFuture
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import models.MessageRequest
import proccess.Processor

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
      cors() {
        pathPrefix("api") {
          concat(
            get {
              path("health") {
                complete(("message" -> "Olá de " + system.address.toString))
              }
            },
            post {
              entity(as[MessageRequest]) { data =>
                complete(FastFuture(Processor(data.code).process))
              }
            }
          )
        }
      }
    }
}
