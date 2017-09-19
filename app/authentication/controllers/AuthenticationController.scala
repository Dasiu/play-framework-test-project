package authentication.controllers

import articles.services.ArticleService
import authentication.models.SecurityUser
import authentication.{ExceptionCode, HttpExceptionResponse, MissingOrInvalidCredentials}
import commons.repositories.ActionRunner
import julienrf.json.derived
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http.client.direct.{DirectBasicAuthClient, HeaderClient}
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.jwt.profile.{JwtGenerator, JwtProfile}
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlayCacheSessionStore
import play.api.libs.json._
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._
import play.cache.SyncCacheApi
import play.mvc.Http

import scala.concurrent.ExecutionContext

case class BearerTokenResponse(token: String) {
  val aType: String = "Bearer"
}

object BearerTokenResponse {
  implicit val jsonReads: Reads[BearerTokenResponse] = Json.reads[BearerTokenResponse]
  implicit val jsonWrites: Writes[BearerTokenResponse] = (tokenResponse: BearerTokenResponse) => {
    JsObject(List(
      "token" -> JsString(tokenResponse.token),
      "type" -> JsString(tokenResponse.aType)
    ))
  }
}

class AuthenticationController(actionRunner: ActionRunner,
                               cacheApi: SyncCacheApi,
                               httpBasicAuthenticator: Authenticator[UsernamePasswordCredentials],
                               components: ControllerComponents,
                               jwtGenerator: JwtGenerator[CommonProfile],
                               implicit private val ec: ExecutionContext) extends AbstractController(components) {

  private val playCacheSessionStore = new PlayCacheSessionStore(cacheApi)
  private val client = new DirectBasicAuthClient(httpBasicAuthenticator)

  def authenticate: Action[AnyContent] = Action { request =>
    val webContext = new PlayWebContext(request, playCacheSessionStore)

    Option(client.getCredentials(webContext))
      .map(credentials => {
        val profile = new JwtProfile()
        profile.setId(credentials.getUsername)

        val jwtToken = jwtGenerator.generate(profile)
        val json = Json.toJson(BearerTokenResponse(jwtToken))
        Ok(json)
      })
      .getOrElse(Forbidden(Json.toJson(HttpExceptionResponse(MissingOrInvalidCredentials))))

  }
}