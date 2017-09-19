package authentication.oauth2

import authentication.AuthenticatedAction
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlaySessionStore
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider._

class TestController(authenticatedAction: AuthenticatedAction,
                     components: ControllerComponents,
                     implicit private val ex: ExecutionContext)
  extends AbstractController(components) {

//  override val tokenEndpoint = new PlayWithFoodTokenEndpoint()

  def accessToken: Action[AnyContent] = authenticatedAction { implicit request =>
    val user = request.user
//    request.
//    val playSessionStore: PlaySessionStore = _
//    val webContext = new PlayWebContext(request, playSessionStore)
    BadRequest("")
  }
}