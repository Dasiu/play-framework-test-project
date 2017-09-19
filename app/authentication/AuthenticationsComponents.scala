package authentication

import authentication.controllers.AuthenticationController
import authentication.deadbolt.config.{DeadboltHandlerCache, OAuthDeadboltHandler}
import authentication.oauth2.{AccessTokenRepo, AuthenticationConfig, OAuth2Controller, PlayWithFoodAuthorizationHandler}
import authentication.repositories.SecurityUserRepo
import authentication.services.api.{PasswordValidator, SecurityUserCreator, SecurityUserProvider}
import authentication.services.{PasswordValidatorImpl, SecurityUserService}
import be.objectify.deadbolt.scala.cache._
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltComponents}
import com.softwaremill.macwire.wire
import commons.CommonsComponents
import commons.config.WithControllerComponents
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.profile.CommonProfile
import org.pac4j.jwt.profile.JwtGenerator
import play.api.mvc.PlayBodyParsers
import play.api.routing.Router
import play.api.routing.sird._
import play.cache.SyncCacheApi

trait AuthenticationsComponents extends CommonsComponents with DeadboltComponents with WithControllerComponents {
  lazy val passwordValidator: PasswordValidator = wire[PasswordValidatorImpl]
  lazy val securityUserCreator: SecurityUserCreator = wire[SecurityUserService]
  lazy val securityUserProvider: SecurityUserProvider = wire[SecurityUserService]
  lazy val securityUserRepo: SecurityUserRepo = wire[SecurityUserRepo]

  lazy val oAuth2Controller: OAuth2Controller = wire[OAuth2Controller]

  lazy val usernamePasswordAuthenticator: Authenticator[UsernamePasswordCredentials] = wire[HttpBasicAuthenticator]

  // todo rename
  def x: SyncCacheApi
  def jwtGenerator: JwtGenerator[CommonProfile]
  lazy val authenticationController: AuthenticationController = wire[AuthenticationController]

  lazy val playWithFoodAuthorizationHandler: PlayWithFoodAuthorizationHandler = wire[PlayWithFoodAuthorizationHandler]
  lazy val authenticationConfig: AuthenticationConfig = wire[AuthenticationConfig]
  lazy val accessTokenRepo: AccessTokenRepo = wire[AccessTokenRepo]

  override lazy val patternCache: PatternCache = wire[DefaultPatternCache]
  override lazy val compositeCache: CompositeCache = wire[DefaultCompositeCache]
  override lazy val handlers: HandlerCache = wire[DeadboltHandlerCache]
  lazy val oAuthDeadboltHandler: OAuthDeadboltHandler = wire[OAuthDeadboltHandler]

  protected def createActionBuilders: PlayBodyParsers => ActionBuilders = parsers => actionBuilders(deadboltActions(parsers))

  val authenticationRoutes: Router.Routes = {
    case GET(p"/authenticate") => authenticationController.authenticate
  }
}