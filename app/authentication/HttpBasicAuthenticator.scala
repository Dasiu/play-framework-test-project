package authentication

import authentication.models.SecurityUser
import authentication.oauth2.exceptions.{InvalidPasswordException, MissingSecurityUserException}
import authentication.repositories.SecurityUserRepo
import commons.models.Login
import commons.repositories.ActionRunner
import commons.utils.DbioUtils
import commons.utils.DbioUtils.optionToDbio
import org.mindrot.jbcrypt.BCrypt
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.exception.CredentialsException
import play.api.Logger
import slick.dbio.DBIO

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Success

class HttpBasicAuthenticator(actionRunner: ActionRunner,
                              securityUserRepo: SecurityUserRepo,
                             implicit private val ec: ExecutionContext) extends Authenticator[UsernamePasswordCredentials] {

  override def validate(credentials: UsernamePasswordCredentials, context: WebContext): Unit = {
    require(credentials != null && context != null)

    val username = credentials.getUsername
    val validateAction = securityUserRepo.byLogin(Login(username))
      .flatMap(optionToDbio(_, new CredentialsException(new MissingSecurityUserException(username))))
      .map(user => {
          if (authenticated(credentials.getPassword, user)) user
          else throw new CredentialsException(new InvalidPasswordException)
        })
//      .
//      .flatMap(user => {
//        if (authenticated(credentials.getPassword, user)) DBIO.successful(user)
//        else DBIO.failed(new InvalidPasswordException)
//      })
//      .filter(authenticated(credentials.getPassword, _)) // todo handle failure

    Await.result(actionRunner.runInTransaction(validateAction), DurationInt(1).minute)
  }

  private def authenticated(givenPassword: String, secUsr: SecurityUser) = {
    BCrypt.checkpw(givenPassword, secUsr.password.value)
  }


}