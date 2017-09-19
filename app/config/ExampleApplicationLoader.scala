package config

import java.util.UUID
import java.util.concurrent.Callable

import _root_.controllers.AssetsComponents
import akka.stream.Materializer
import articles.ArticleComponents
import authentication.oauth2.TestController
import authentication.{AuthenticatedAction, AuthenticationsComponents}
import be.objectify.deadbolt.scala.ActionBuilders
import com.nimbusds.jose.JWSAlgorithm
import com.softwaremill.macwire.wire
import org.pac4j.core.profile.CommonProfile
import org.pac4j.jwt.config.encryption.SecretEncryptionConfiguration
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.jwt.profile.JwtGenerator
import org.pac4j.play.filters.SecurityFilter
import org.pac4j.play.store.PlayCacheSessionStore
import play.api.ApplicationLoader.Context
import play.api._
import play.api.cache.{AsyncCacheApi, DefaultSyncCacheApi}
import play.api.cache.ehcache.EhCacheComponents
import play.api.db.evolutions.{DynamicEvolutions, EvolutionsComponents}
import play.api.db.slick._
import play.api.db.slick.evolutions.SlickEvolutionsComponents
import play.api.i18n._
import play.api.inject.{Injector, SimpleInjector}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc._
import play.api.routing.Router
import play.cache.{DefaultAsyncCacheApi, DefaultSyncCacheApi, SyncCacheApi}
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSFilter
import slick.basic.{BasicProfile, DatabaseConfig}
import users.UsersComponents

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.reflect.ClassTag
import play.api.routing.sird._

class ExampleApplicationLoader extends ApplicationLoader {
  def load(context: Context): Application = new ExampleComponents(context).application
}

class ExampleComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with SlickComponents
  with SlickEvolutionsComponents
  with AssetsComponents
  with I18nComponents
  with HttpFiltersComponents
  with EvolutionsComponents
  with AhcWSComponents
  with AuthenticationsComponents
  with UsersComponents
  with ArticleComponents
  with EhCacheComponents {

  override lazy val slickApi: SlickApi =
    new DefaultSlickApi(environment, configuration, applicationLifecycle)(executionContext)

  override lazy val databaseConfigProvider: DatabaseConfigProvider = new DatabaseConfigProvider {
    def get[P <: BasicProfile]: DatabaseConfig[P] = slickApi.dbConfig[P](DbName("default"))
  }

  override lazy val dynamicEvolutions: DynamicEvolutions = new DynamicEvolutions

  def onStart(): Unit = {
    // applicationEvolutions is a val and requires evaluation
    applicationEvolutions
  }

  onStart()

  // set up logger
  LoggerConfigurator(context.environment.classLoader).foreach {
    _.configure(context.environment, context.initialConfiguration, Map.empty)
  }

  lazy val actionBuilders: ActionBuilders = createActionBuilders(playBodyParsers)

  val authenticationRoutesTest: Router.Routes = {
    case GET(p"/auth/test") => testController.accessToken
  }

  protected lazy val routes: PartialFunction[RequestHeader, Handler] = userRoutes
    .orElse(authenticationRoutes)
    .orElse(articleRoutes)
    .orElse(authenticationRoutesTest)

  override lazy val router: Router = Router.from(routes)

  // implicit executionContext and materializer are defined in BuiltInComponents
//  lazy val loggingFilter: LoggingFilter = new LoggingFilter()
//  val corsFilter = CORSFilter
  // todo cors filter
  private lazy val bodyParser: BodyParsers.Default = wire[BodyParsers.Default]
  lazy val authenticatedAction: AuthenticatedAction = wire[AuthenticatedAction]
//  lazy val syncCacheApi: play.cache.SyncCacheApi = new SyncCacheApiAdapter(defaultCacheApi)

  override lazy val defaultCacheApi: AsyncCacheApi = cacheApi(UUID.randomUUID().toString)

  // todo clean up
//  override lazy val cacheApi: play.cache.SyncCacheApi = x

  private val defaultAsyncCacheApi = new DefaultAsyncCacheApi(defaultCacheApi)
  val x: play.cache.DefaultSyncCacheApi = new play.cache.DefaultSyncCacheApi(defaultAsyncCacheApi)
  lazy val sessionStore: PlayCacheSessionStore = new PlayCacheSessionStore(x)

//  lazy val securityFilter: SecurityFilter = new SecurityFilter(materializer, configuration, sessionStore, )
  // gzipFilter is defined in GzipFilterComponents
  // todo
//  override lazy val httpFilters = Seq(securityFilter)

  val SECRET = "secretadfsadfsdfasfdsfasffsfdasffasdf"
  val signatureConfig = new SecretSignatureConfiguration("12345678901234567890123456789012")
//  private val secretEncryptionConfiguration = new SecretEncryptionConfiguration("12345678901234567890123456789012")
  override lazy val jwtGenerator: JwtGenerator[CommonProfile] = new JwtGenerator(signatureConfig)
  lazy val jwtAuthenticator: JwtAuthenticator = new JwtAuthenticator(signatureConfig)
  //  jwtAuthenticator.addSignatureConfiguration(signatureConfig, secretEncryptionConfiguration);
  //

  lazy val testController: TestController = wire[TestController]
}

//class TestSecurityFilter(materializer: Materializer, ) extends Filter {
//  override implicit def mat: Materializer = materializer
//
//  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
//  }
//}

//class SyncCacheApiAdapter(cacheApi: AsyncCacheApi)(implicit ex: ExecutionContext) extends play.cache.SyncCacheApi {
//  private val defaultExpirationDuration = DurationInt(10).seconds
//
//  override def set(key: String, value: scala.Any, expiration: Int): Unit = {
//    val eventualDone = cacheApi.set(key, value, DurationInt(expiration).seconds)
//    Await.result(eventualDone, defaultExpirationDuration)
//  }
//
//  override def set(key: String, value: scala.Any): Unit = set(key, value, defaultExpirationDuration.toSeconds.toInt)
//
//  override def getOrElseUpdate[T](key: String, block: Callable[T], expiration: Int): T = {
//    implicit val classTag = ClassTag[T](classOf[T])
//    val eventualT = cacheApi.getOrElseUpdate(key, DurationInt(expiration).seconds)(Future(block.call()))
//    Await.result(eventualT, defaultExpirationDuration)
//  }
//
//  override def getOrElseUpdate[T](key: String, block: Callable[T]): T =
//    getOrElseUpdate(key, block, defaultExpirationDuration.toSeconds.toInt)
//
//  override def get[T](key: String): T = {
//    val eventualT = cacheApi.get(key)(implicitly[ClassTag[T]])
//    Await.result(eventualT, defaultExpirationDuration).get
//  }
//
//  override def remove(key: String): Unit = {
//    val eventualDone = cacheApi.remove(key)
//    Await.result(eventualDone, defaultExpirationDuration)
//  }
//
//}