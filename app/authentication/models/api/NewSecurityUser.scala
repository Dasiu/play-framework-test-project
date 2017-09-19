package authentication.models.api

import java.time.LocalDateTime

import authentication.models.{PasswordHash, SecurityUser, SecurityUserId}
import commons.models.Login

case class NewSecurityUser(login: Login, password: PlainTextPassword) {
  def toSecurityUser: SecurityUser = SecurityUser(SecurityUserId(-1),
    login,
    // todo hash password;
    PasswordHash("missing"),
    null,
    null)
}

case class PlainTextPassword(value: String) extends AnyVal