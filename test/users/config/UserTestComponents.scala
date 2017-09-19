package users.config

import java.nio.charset.StandardCharsets
import java.util.Base64

import articles.models.{Article, ArticleId}
import authentication.AuthenticationsComponents
import authentication.controllers.BearerTokenResponse
import authentication.models.SecurityUser
import authentication.models.api.{NewSecurityUser, PlainTextPassword}
import authentication.repositories.SecurityUserRepo
import commons.models.Login
import commons.repositories.ActionRunner
import org.scalatestplus.play.PortNumber
import play.api.http.HeaderNames
import play.api.libs.ws.{WSClient, WSRequest}
import testhelpers.TestUtils
import users.UsersComponents
import users.models.{User, UserRegistration}
import users.repositories.UserRepo
import users.services.UserRegistrationService

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait UserTestComponents {
  // todo users components
  // todo rename 2
  _: AuthenticationsComponents with UsersComponents =>

  lazy val userRegistrationTestHelper: UserRegistrationTestHelper =
    new UserRegistrationTestHelper(userRegistrationService, "/authenticate", actionRunner)
}

class UserRegistrationTestHelper(userRegistrationService: UserRegistrationService,
                                authenticatePath: String,
                                 implicit private val actionRunner: ActionRunner) {

  def register(userRegistration: UserRegistration): User = {
    require(userRegistration != null)

    val action = userRegistrationService.register(userRegistration)
    TestUtils.runAndAwaitResult(action)(actionRunner, new DurationInt(1).seconds)
  }

  def authenticate(login: Login, password: PlainTextPassword)(implicit portNumber:
  PortNumber, wsClient: WSClient): String = {
    val rawLogin = login.value
    val rawPassword = password.value
    val rawString = s"$rawLogin:$rawPassword"

    val loginAndPasswordEncoded64 = Base64.getEncoder.encodeToString(rawString.getBytes(StandardCharsets.UTF_8))

    val response = Await.result(wsUrl(authenticatePath)
      .withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Basic $loginAndPasswordEncoded64")
      .get(), DurationInt(1).seconds)

    response.json.as[BearerTokenResponse].token
  }

  private def wsUrl(url: String)(implicit portNumber: PortNumber, wsClient: WSClient): WSRequest = doCall(url, wsClient, portNumber)

  private def doCall(url: String, wsClient: WSClient, portNumber: PortNumber) = {
    wsClient.url("http://localhost:" + portNumber.value + url)
  }
}

object UserRegistrations {
  val patrycjaRegistration = UserRegistration(Login("petycja"), PlainTextPassword("a valid password"))
}