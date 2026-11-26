object AccountReactivation {
  final case class Account(id: String, password: String, otp: String, active: Boolean)

  private val accounts: scala.collection.mutable.Map[String, Account] =
    scala.collection.mutable.Map(
      "1001" -> Account("1001", "secret123", "123456", active = false),
      "1002" -> Account("1002", "hunter2", "654321", active = false)
    )

  def reactivate(accountId: String, password: String, otp: String): Either[String, String] =
    accounts.get(accountId) match {
      case None =>
        Left("Unknown account")
      case Some(account) if account.password != password =>
        Left("Incorrect password")
      case Some(account) if account.otp != otp =>
        Left("Incorrect OTP")
      case Some(account) =>
        accounts.update(accountId, account.copy(active = true))
        Right(s"Account $accountId is active")
    }

  def isActive(accountId: String): Boolean =
    accounts.get(accountId).exists(_.active)
}
