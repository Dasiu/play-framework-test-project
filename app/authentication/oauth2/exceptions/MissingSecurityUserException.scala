package authentication.oauth2.exceptions

// todo rename arg
private[authentication] class MissingSecurityUserException(token: String) extends RuntimeException(token)