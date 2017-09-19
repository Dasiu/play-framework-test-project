package authentication

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader
import articles.models.Article
import authentication.models.api.{NewSecurityUser, PlainTextPassword}
import authentication.oauth2.AuthenticationConfig
import be.objectify.deadbolt.scala.ActionBuilders
import com.softwaremill.macwire.wire
import commons.models.Login
import commons.utils.DbioUtils
import config.{SecurityUserPopulator, SecurityUsers}
import julienrf.json.derived
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.sird._
import play.api.libs.json._
import testhelpers.RealWorldWithServerBaseTest

import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider.OAuth2Provider
import julienrf.json.derived
import play.api.http.HeaderNames
import play.shaded.ahc.io.netty.handler.codec.spdy.SpdyHeaders.HttpNames
import users.config.{UserRegistrationTestHelper, UserRegistrations}

//sealed abstract class ExceptionCode {
//  val message: String = ""
//}

//object ExceptionCode {

//  implicit val jsonWrites: Writes[ExceptionCode] = (o: ExceptionCode) => {
//    val exceptionCode = JsString(o.getClass.getSimpleName)
//    val message = JsString(o.message)
//
//    JsObject(List("exceptionCode" -> exceptionCode, "message" -> message))
//  }
//  implicit val jsonWrites: Format[ExceptionCode] = derived.flat.oformat((__ \ "type").format[String])
//}

//case object MissingCredentialsCode extends ExceptionCode {
//  override val message: String = "Provide Jwt token through Http header authorization"
//}

//case class HttpExceptionResponse(code: ExceptionCode, message: String = "")
//object HttpExceptionResponse {
//  implicit val jsonWrites: Format[HttpExceptionResponse] = Json.format[HttpExceptionResponse]
//}

class JwtAuthenticationIntegrationTest extends RealWorldWithServerBaseTest {

  val fakeApiPath: String = "test"

  val accessTokenJsonAttrName: String = "access_token"

  def securityUserPopulator(implicit testComponents: AppWithTestComponents): SecurityUserPopulator =
    testComponents.securityUserPopulator

  implicit def wsClient(implicit testComponents: AppWithTestComponents): WSClient = testComponents.wsClient

  def userRegistrationTestHelper(implicit testComponents: AppWithTestComponents): UserRegistrationTestHelper =
    testComponents.userRegistrationTestHelper

  "authentication" should {

    "allow everyone to public API" in {
      // when
      val response: WSResponse = await(wsUrl(s"/$fakeApiPath/public").get())

      // then
      response.status.mustBe(OK)
    }

    "block request without jwt token" in {
      // when
      val response: WSResponse = await(wsUrl(s"/$fakeApiPath/authenticationRequired").get())

      // then
      response.status.mustBe(UNAUTHORIZED)
      response.json.as[HttpExceptionResponse].code.mustBe(MissingOrInvalidCredentials)
    }

    "block request with invalid jwt token" in {
      // given
      val token = "invalidJwtToken"

      // when
      val response: WSResponse = await(wsUrl(s"/$fakeApiPath/authenticationRequired")
        .addHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $token")
        .get())

      // then
      response.status.mustBe(UNAUTHORIZED)
      response.json.as[HttpExceptionResponse].code.mustBe(MissingOrInvalidCredentials)
    }

    "allow authenticated user to secured API" in {
      // given
      val registration = UserRegistrations.patrycjaRegistration
      val user = userRegistrationTestHelper.register(registration)
      val token = userRegistrationTestHelper.authenticate(registration.login, registration.password)

      // when
      val response: WSResponse = await(wsUrl(s"/$fakeApiPath/authenticationRequired")
        .addHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $token")
        .get())

      // then
      response.status.mustBe(OK)
      response.json.as[AuthenticatedUser].login.mustBe(user.login)
    }

    // todo expire token after some time

    "block expired jwt token" in {
      // todo
    }

    // todo block random token

//    "allow authenticated user" in {
      // todo security user populator

      // given
//      runAndAwaitResult(securityUserCreator.create(securityUserToRegister))(c)

//      val accessToken = authenticate(securityUserToRegister, authenticationConfig)(wsClient)

//       when
//      val response: WSResponse = await(wsUrl(s"/$fakeApiPath/testScopeRequired")
//        .addHttpHeaders("Authorization" -> s"Bearer $accessToken")
//        .get())
//
//       then
//      response.status.mustBe(OK)
//    }

//    "reuse already issued and still valid token" in {
//      val c = components
//
//      // given
//      runAndAwaitResult(securityUserCreator.create(securityUserToRegister))(c)
//
//      // when
//      val accessToken = authenticate(securityUserToRegister, authenticationConfig)
//      val accessToken2 = authenticate(securityUserToRegister, authenticationConfig)
//
//      // then
//      accessToken.mustEqual(accessToken2)
//    }

//    "not allow expired token" in {
//      val c = components
//
//      // given
//      runAndAwaitResult(securityUserCreator.create(securityUserToRegister))(c)
//
//      val accessToken = authenticate(securityUserToRegister, authenticationConfig)
//      expireAccessToken(accessToken, c)
//
//      // when
//      val response: WSResponse = await(wsUrl(s"/$fakeApiPath/testScopeRequired")
//        .addHttpHeaders("Authorization" -> s"Bearer $accessToken")
//        .get())
//
//      // then
//      response.status.mustBe(UNAUTHORIZED)
//    }
  }

//  private def expireAccessToken(accessToken: String, components: ExampleComponents) = {
//    val expiredTokenDateTime = LocalDateTime.of(2016, 1, 1, 1, 1, 1)
//    val updateCreatedDateTime = components.accessTokenRepo.byToken(accessToken)
//      .flatMap(DbioUtils.optionToDbio(_))
//      .map(_.copy(createdAt = expiredTokenDateTime))
//      .flatMap(accessToken => components.accessTokenRepo.update(accessToken))
//
//    runAndAwaitResult(updateCreatedDateTime)(components)
//  }

  override def components: RealWorldWithTestConfig = new RealWorldWithTestConfig {

    lazy val authenticationTestController: AuthenticationTestController = wire[AuthenticationTestController]

    override lazy val router: Router = {
      val testControllerRoutes: PartialFunction[RequestHeader, Handler] = {
        case GET(p"/test/public") => authenticationTestController.public
        case GET(p"/test/authenticationRequired") => authenticationTestController.authenticated
      }

      Router.from(routes.orElse(testControllerRoutes))
    }
  }

  class AuthenticationTestController(authenticatedAction: AuthenticatedAction,
                                     components: ControllerComponents,
                                     implicit private val ex: ExecutionContext)
    extends AbstractController(components) {

    def public: Action[AnyContent] = Action { _ =>
      Results.Ok
    }

    def authenticated: Action[AnyContent] = authenticatedAction { request =>
      Ok(Json.toJson(request.user))
    }

  }
}