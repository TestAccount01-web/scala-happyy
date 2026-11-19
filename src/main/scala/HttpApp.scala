import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import java.net.{InetSocketAddress, URLDecoder}
import java.nio.charset.StandardCharsets

object HttpApp {
  def start(port: Int = 8080): HttpServer = {
    val server = HttpServer.create(new InetSocketAddress(port), 0)

    server.createContext("/health", handle { exchange =>
      if (exchange.getRequestMethod != "GET") respond(exchange, 405, "Method Not Allowed")
      else respond(exchange, 200, "ok")
    })

    server.createContext("/add", handle { exchange =>
      calc(exchange)(Calc.add)
    })

    server.createContext("/multiply", handle { exchange =>
      calc(exchange)(Calc.multiply)
    })

    server.createContext("/subtract", handle { exchange =>
      calc(exchange)(Calc.subtract)
    })

    server.createContext("/divide", handle { exchange =>
      calc(exchange)(Calc.divide)
    })

    server.createContext("/percent", handle { exchange =>
      calc(exchange)(Calc.percent)
    })

    server.setExecutor(null)
    server.start()
    server
  }

  def main(args: Array[String]): Unit = {
    val port = args.headOption.flatMap(_.toIntOption).getOrElse(8080)
    val server = start(port)
    println(s"Calc HTTP API listening on http://localhost:${server.getAddress.getPort}")
  }

  private def handle(f: HttpExchange => Unit): HttpHandler =
    (exchange: HttpExchange) =>
      try f(exchange)
      catch {
        case e: Exception => respond(exchange, 500, s"error: ${e.getMessage}")
      } finally exchange.close()

  private def calc(exchange: HttpExchange)(op: (Int, Int) => Int): Unit = {
    if (exchange.getRequestMethod != "GET") {
      respond(exchange, 405, "Method Not Allowed")
    } else {
      val params = queryParams(exchange.getRequestURI.getRawQuery)
      (params.get("a"), params.get("b")) match {
        case (Some(a), Some(b)) =>
          (a.toIntOption, b.toIntOption) match {
            case (Some(x), Some(y)) => respond(exchange, 200, op(x, y).toString)
            case _ => respond(exchange, 400, "Query parameters 'a' and 'b' must be integers")
          }
        case _ => respond(exchange, 400, "Query parameters 'a' and 'b' are required")
      }
    }
  }

  private def queryParams(raw: String): Map[String, String] =
    Option(raw).filter(_.nonEmpty) match {
      case None => Map.empty
      case Some(query) =>
        query
          .split("&")
          .toList
          .map { pair =>
            pair.split("=", 2) match {
              case Array(key, value) => decode(key) -> decode(value)
              case Array(key)        => decode(key) -> ""
              case _                 => "" -> ""
            }
          }
          .toMap
    }

  private def decode(value: String): String =
    URLDecoder.decode(value, StandardCharsets.UTF_8)

  private def respond(exchange: HttpExchange, status: Int, body: String): Unit = {
    val bytes = body.getBytes(StandardCharsets.UTF_8)
    exchange.getResponseHeaders.add("Content-Type", "text/plain; charset=utf-8")
    exchange.sendResponseHeaders(status, bytes.length.toLong)
    val out = exchange.getResponseBody
    try out.write(bytes)
    finally out.close()
  }
}
