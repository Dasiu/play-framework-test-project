package authentication

import java.nio.charset.StandardCharsets
import java.util.Base64

import authentication.config.{SecurityUserPopulator, SecurityUsers}
import authentication.controllers.BearerTokenResponse
import authentication.models.api.{NewSecurityUser, PlainTextPassword}
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.libs.ws.WSClient
import testhelpers.RealWorldWithServerBaseTest
import users.config.{UserRegistrationTestHelper, UserRegistrations}

class HttpBasicAuthenticateIntegrationTest extends RealWorldWithServerBaseTest {

  val authenticatePath: String = "/authenticate"

  def securityUserPopulator(implicit testComponents: AppWithTestComponents): SecurityUserPopulator =
    testComponents.securityUserPopulator

  def userRegistrationTestHelper(implicit testComponents: AppWithTestComponents): UserRegistrationTestHelper =
    testComponents.userRegistrationTestHelper

  implicit def wsClient(implicit testComponents: AppWithTestComponents): WSClient = testComponents.wsClient

  def basicAuthEncode(newSecurityUser: NewSecurityUser): String = {
    val login = newSecurityUser.login.value
    val password = newSecurityUser.password.value
    val rawString = s"$login:$password"

    Base64.getEncoder.encodeToString(rawString.getBytes(StandardCharsets.UTF_8))
  }

  "Http basic authenticate" should {

    "allow valid user and password" in {
      // given
      val registration = UserRegistrations.patrycjaRegistration
      userRegistrationTestHelper.register(registration)

      val loginAndPasswordEncoded64 = basicAuthEncode(NewSecurityUser(registration.login, registration.password))

      // when
      val response = await(wsUrl(authenticatePath)
        .withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Basic $loginAndPasswordEncoded64")
        .get())

      // then
      response.status.mustBe(OK)
      response.json.as[BearerTokenResponse].token.mustNot(equal(""))
    }

    "block not existing user" in {
      // given
      val loginAndPasswordEncoded64 = basicAuthEncode(SecurityUsers.patrycja)

      // when
      val response = await(wsUrl(authenticatePath)
        .withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Basic $loginAndPasswordEncoded64")
        .get())

      // then
      response.status.mustBe(FORBIDDEN)
      response.json.as[HttpExceptionResponse].code.mustBe(MissingOrInvalidCredentials)
    }

    "block user with invalid password" in {
      // given
      val registration = UserRegistrations.patrycjaRegistration
      userRegistrationTestHelper.register(registration)

      val loginAndPasswordEncoded64 = basicAuthEncode(NewSecurityUser(registration.login,
        PlainTextPassword("invalid pass")))

      // when
      val response = await(wsUrl(authenticatePath)
        .withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Basic $loginAndPasswordEncoded64")
        .get())

      // then
      response.status.mustBe(FORBIDDEN)
      response.json.as[HttpExceptionResponse].code.mustBe(MissingOrInvalidCredentials)
    }
  }
}