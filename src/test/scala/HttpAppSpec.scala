import com.sun.net.httpserver.HttpServer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

class HttpAppSpec extends AnyFunSuite with BeforeAndAfterAll {
  private val client = HttpClient.newHttpClient()
  private var server: HttpServer = _
  private var baseUrl: String = _

  override protected def beforeAll(): Unit = {
    server = HttpApp.start(0)
    baseUrl = s"http://localhost:${server.getAddress.getPort}"
  }

  override protected def afterAll(): Unit = {
    if (server != null) server.stop(0)
  }

  private def get(path: String): HttpResponse[String] =
    client.send(
      HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
      HttpResponse.BodyHandlers.ofString()
    )

  private def post(path: String, body: String): HttpResponse[String] =
    client.send(
      HttpRequest
        .newBuilder(URI.create(baseUrl + path))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build(),
      HttpResponse.BodyHandlers.ofString()
    )

  test("health endpoint returns ok") {
    val res = get("/health")
    assert(res.statusCode() == 200)
    assert(res.body() == "ok")
  }

  test("add endpoint returns the sum of a and b") {
    val res = get("/add?a=2&b=3")
    assert(res.statusCode() == 200)
    assert(res.body() == "5")
  }

  test("multiply endpoint returns the product of a and b") {
    val res = get("/multiply?a=4&b=3")
    assert(res.statusCode() == 200)
    assert(res.body() == "12")
  }

  test("subtract endpoint returns the difference of a and b") {
    val res = get("/subtract?a=7&b=3")
    assert(res.statusCode() == 200)
    assert(res.body() == "4")
  }

  test("divide endpoint returns the quotient of a and b") {
    val res = get("/divide?a=12&b=4")
    assert(res.statusCode() == 200)
    assert(res.body() == "3")
  }

  test("percent endpoint returns the percentage of a") {
    val res = get("/percent?a=200&b=15")
    assert(res.statusCode() == 200)
    assert(res.body() == "30")
  }

  test("divide endpoint returns 500 when dividing by zero") {
    assert(get("/divide?a=12&b=0").statusCode() == 500)
  }

  test("add endpoint rejects missing parameters with 400") {
    assert(get("/add?a=2").statusCode() == 400)
  }

  test("add endpoint rejects non-integer parameters with 400") {
    assert(get("/add?a=x&b=3").statusCode() == 400)
  }

  test("unknown path returns 404") {
    assert(get("/nope").statusCode() == 404)
  }

  test("reactivate endpoint activates an account with password and OTP") {
    val res = post("/account/reactivate", "account=1002&password=hunter2&otp=654321")
    assert(res.statusCode() == 200)
    assert(res.body() == "Account 1002 is active")
  }

  test("reactivate endpoint rejects an incorrect password with 401") {
    val res = post("/account/reactivate", "account=1001&password=wrong&otp=123456")
    assert(res.statusCode() == 401)
    assert(res.body() == "Incorrect password")
  }

  test("reactivate endpoint rejects an incorrect OTP with 401") {
    val res = post("/account/reactivate", "account=1001&password=secret123&otp=000000")
    assert(res.statusCode() == 401)
    assert(res.body() == "Incorrect OTP")
  }

  test("reactivate endpoint returns 404 for an unknown account") {
    val res = post("/account/reactivate", "account=9999&password=secret123&otp=123456")
    assert(res.statusCode() == 404)
  }

  test("reactivate endpoint rejects missing body parameters with 400") {
    val res = post("/account/reactivate", "account=1001&password=secret123")
    assert(res.statusCode() == 400)
  }

  test("reactivate endpoint rejects non-POST requests with 405") {
    assert(get("/account/reactivate").statusCode() == 405)
  }

  test("account status reports whether an account is active") {
    post("/account/reactivate", "account=1002&password=hunter2&otp=654321")
    val res = get("/account/status?account=1002")
    assert(res.statusCode() == 200)
    assert(res.body() == "active")
  }
}
