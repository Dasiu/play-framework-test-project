package authentication.config

import articles.models.{Article, ArticleId}
import authentication.AuthenticationsComponents
import authentication.models.SecurityUser
import authentication.models.api.{NewSecurityUser, PlainTextPassword}
import authentication.repositories.SecurityUserRepo
import commons.models.Login
import commons.repositories.ActionRunner
import testhelpers.TestUtils
import users.UsersComponents
import users.repositories.UserRepo

import scala.concurrent.duration.DurationInt

trait AuthenticationTestComponents {
  // todo users components
  _: AuthenticationsComponents =>

  lazy val securityUserPopulator: SecurityUserPopulator = new SecurityUserPopulator(securityUserRepo, actionRunner)

}

class SecurityUserPopulator(securityUserRepo: SecurityUserRepo,
                            implicit private val actionRunner: ActionRunner) {

  def save(securityUser: NewSecurityUser): SecurityUser = {
    val action = securityUserRepo.create(securityUser.toSecurityUser)
    TestUtils.runAndAwaitResult(action)(actionRunner, new DurationInt(1).seconds)
  }
}

object SecurityUsers {
  val patrycja: NewSecurityUser = NewSecurityUser(
    Login("petycja"),
    PlainTextPassword("asdf"),
  )
}
