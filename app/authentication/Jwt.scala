package authentication

import authentication.models.SecurityUser
import commons.models.Login
import julienrf.json.derived
import org.pac4j.core.credentials.TokenCredentials
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlayCacheSessionStore
import play.api.libs.json.{Format, Json}
import play.api.mvc.Results._
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._
import play.cache.SyncCacheApi
import play.mvc.Http

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class AuthenticatedUser(login: Login)

object AuthenticatedUser {
  implicit val jsonFormat: Format[AuthenticatedUser] = Json.format[AuthenticatedUser]
}

class SecurityUserRequest[A](securityUser: AuthenticatedUser, request: Request[A])
  extends AuthenticatedRequest[A, AuthenticatedUser](securityUser, request)

class AuthenticatedAction(override val parser: BodyParsers.Default,
                          cacheApi: SyncCacheApi,
                          jwtAuthenticator: JwtAuthenticator)(implicit ec: ExecutionContext)
  extends ActionBuilder[SecurityUserRequest, AnyContent] {

  private val playCacheSessionStore = new PlayCacheSessionStore(cacheApi)
  private val prefixSpaceIsCrucialHere = "Bearer "
  private val client = new HeaderClient(Http.HeaderNames.AUTHORIZATION, prefixSpaceIsCrucialHere, jwtAuthenticator)

  private def authenticate(requestHeader: RequestHeader) = {
    val webContext = new PlayWebContext(requestHeader, playCacheSessionStore)

    // todo validate jwt token and then check expiration date
    Option(client.getCredentials(webContext))
      .toRight(MissingOrInvalidCredentials)
      .map(client.getUserProfile(_, webContext))
      .map(profile => AuthenticatedUser(Login(profile.getId)))
  }

  override protected def executionContext: ExecutionContext = ec

  private def onUnauthorized(exceptionCode: ExceptionCode, requestHeader: RequestHeader) = {
    val response = HttpExceptionResponse(exceptionCode)
    Unauthorized(Json.toJson(response))
  }

  override def invokeBlock[A](request: Request[A], block: (SecurityUserRequest[A]) => Future[Result]): Future[Result] = {
    authenticate(request) match {
      case Right(securityUser) => block(new SecurityUserRequest(securityUser, request))
      case Left(code) => Future.successful(onUnauthorized(code, request))
    }
      //      .map(securityUser => block(new SecurityUserRequest(securityUser, request)))
//      .fold(exception => Future.successful(onUnauthorized(exception, request)),
//        securityUser => block(new SecurityUserRequest(securityUser, request)))
  }
}

sealed abstract class ExceptionCode {
  val message: String = ""
}

case object MissingUserCode extends ExceptionCode
case object InvalidPasswordCode extends ExceptionCode

object ExceptionCode {

  //  implicit val jsonWrites: Writes[ExceptionCode] = (o: ExceptionCode) => {
  //    val exceptionCode = JsString(o.getClass.getSimpleName)
  //    val message = JsString(o.message)
  //
  //    JsObject(List("exceptionCode" -> exceptionCode, "message" -> message))
  //  }
  implicit val jsonWrites: Format[ExceptionCode] = derived.oformat[ExceptionCode]()
}

// todo rename code suffix
case object MissingOrInvalidCredentials extends ExceptionCode {
  override val message: String = "Provide valid Jwt token through Http header Authorization"
}

case object InvalidCredentialsCode extends ExceptionCode

case class HttpExceptionResponse(code: ExceptionCode) {
  val message: String = code.message
}

object HttpExceptionResponse {
  implicit val jsonWrites: Format[HttpExceptionResponse] = Json.format[HttpExceptionResponse]
}

trait WithExceptionCode {
  def exceptionCode: ExceptionCode
}

class MissingCredentialsException extends RuntimeException with WithExceptionCode {
  override def exceptionCode: ExceptionCode = MissingOrInvalidCredentials
}