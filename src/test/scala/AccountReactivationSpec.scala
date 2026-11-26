import org.scalatest.funsuite.AnyFunSuite

class AccountReactivationSpec extends AnyFunSuite {
  test("reactivates an account with the correct password and OTP") {
    assert(AccountReactivation.reactivate("1001", "secret123", "123456") == Right("Account 1001 is active"))
    assert(AccountReactivation.isActive("1001"))
  }

  test("rejects an incorrect password") {
    assert(AccountReactivation.reactivate("1002", "wrong", "654321") == Left("Incorrect password"))
  }

  test("rejects an incorrect OTP") {
    assert(AccountReactivation.reactivate("1002", "hunter2", "000000") == Left("Incorrect OTP"))
  }

  test("rejects an unknown account") {
    assert(AccountReactivation.reactivate("9999", "secret123", "123456") == Left("Unknown account"))
  }
}
