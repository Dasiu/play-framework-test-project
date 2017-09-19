package authentication

import java.security.{KeyPair, KeyPairGenerator}

import articles.config.{ArticlePopulator, Articles}
import articles.controllers.ArticlePage
import articles.controllers.ArticlePageJsonMappings._
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jwt.{JWT, JWTParser, SignedJWT}
import com.sun.org.apache.xml.internal.security.encryption.EncryptionMethod
import org.pac4j.core.credentials.TokenCredentials
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.jwt.config.encryption.SecretEncryptionConfiguration
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.jwt.profile.JwtGenerator
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlayCacheSessionStore
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.FakeRequest
import play.mvc.Http
import testhelpers.RealWorldWithServerBaseTest

class AuthenticationTest extends RealWorldWithServerBaseTest {
  val apiPath: String = "articles"

  def articlePopulator(implicit testComponents: AppWithTestComponents): ArticlePopulator = {
    testComponents.articlePopulator
  }

  def jwt(implicit testComponents: AppWithTestComponents): ArticlePopulator = {
    testComponents.articlePopulator
  }

  implicit def wsClient(implicit testComponents: AppWithTestComponents): WSClient = testComponents.wsClient

  "GET articles" should {

    "return single article and article count" in {
      val jwtGenerator = new JwtGenerator[CommonProfile](new SecretSignatureConfiguration("12345678901234567890123456789012"))
//      val expectedBody: JsValue = Json.toJson(ArticlePage(List(persistedArticle), 1L))

      // when
      val profile = new CommonProfile()
      profile.setId("login")
//      profile.addRole("usr")
//      profile.addPermission("per")

      val token = jwtGenerator.generate(profile)

//      testComponents.sessionStore

      //  private val secretEncryptionConfiguration = new SecretEncryptionConfiguration("12345678901234567890123456789012")
      val jwtAuthenticator = new JwtAuthenticator(new SecretSignatureConfiguration("12345678901234567890123456789012"))
      val asdf: JWT = JWTParser.parse(token)
      //  eyJhbGciOiJIUzI1NiJ9.eyIkaW50X3Blcm1zIjpbXSwic3ViIjoib3JnLnBhYzRqLmNvcmUucHJvZmlsZS5Db21tb25Qcm9maWxlI2xvZ2luIiwiJGludF9yb2xlcyI6W119.V2ZDjRojxN65PkoKYL66veHfb5JMNkK7-ExLbZ2WPxM
      //  eyJhbGciOiJIUzI1NiJ9.eyIkaW50X3Blcm1zIjpbXSwic3ViIjoib3JnLnBhYzRqLmNvcmUucHJvZmlsZS5Db21tb25Qcm9maWxlI2xvZ2luIiwiJGludF9yb2xlcyI6W119.V2ZDjRojxN65PkoKYL66veHfb5JMNkK7-ExLbZ2WPxM
      val signedJwt: SignedJWT = asdf.asInstanceOf[SignedJWT]
      val res = new SecretSignatureConfiguration("12345678901234567890123456789012").verify(signedJwt)
      //    val webContext = new PlayWebContext(request, playCacheSessionStore)
      val client = new HeaderClient(Http.HeaderNames.AUTHORIZATION, "Bearer", jwtAuthenticator)


      val response: WSResponse = await(wsUrl(s"/auth/test")
          .withHttpHeaders(Http.HeaderNames.AUTHORIZATION -> s"Bearer $token")
//        .addQueryStringParameters("limit" -> "5", "offset" -> "0")
        .get())

      // then
      response.status.mustBe(OK)
//      response.body.mustBe(expectedBody.toString())

      //      val SECRET = "secretadfsadfsdfasfdsfasffsfdasffasdf"
//      val jwtAuthenticator = new JwtAuthenticator()
//
//      val signatureConfig = new SecretSignatureConfiguration(SECRET)
//      jwtAuthenticator.addSignatureConfiguration(signatureConfig);
//
//      jwtAuthenticator.addEncryptionConfiguration(new SecretEncryptionConfiguration(SECRET));
//
//      import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
//      import org.pac4j.jwt.profile.JwtGenerator
////      new JwtGenerator[_ <: CommonProfile](new SecretSignatureConfiguration(SecurityModule.JWT_SALT))
//      val jwtGenerator = new JwtGenerator[RealWorldProfile](signatureConfig)
//      val profile = new RealWorldProfile
//      val token = jwtGenerator.generate(profile)
//      val profile2 = jwtAuthenticator.validateToken(token);
//
//      profile2.toString
//
//      val client = new HeaderClient(Http.HeaderNames.AUTHORIZATION, jwtAuthenticator)
//      val request = FakeRequest()
////      new PlayCacheSessionStore()
////      new PlayWebContext(request, )
////      client.getCredentials()
////      client.getUserProfile()
    }

  }
}

class RealWorldProfile extends CommonProfile