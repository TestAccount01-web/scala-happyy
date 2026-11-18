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

  test("add endpoint rejects missing parameters with 400") {
    assert(get("/add?a=2").statusCode() == 400)
  }

  test("add endpoint rejects non-integer parameters with 400") {
    assert(get("/add?a=x&b=3").statusCode() == 400)
  }

  test("unknown path returns 404") {
    assert(get("/nope").statusCode() == 404)
  }
}
