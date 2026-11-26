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

    server.createContext("/account/reactivate", handle { exchange =>
      reactivate(exchange)
    })

    server.createContext("/account/status", handle { exchange =>
      accountStatus(exchange)
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

  private def reactivate(exchange: HttpExchange): Unit = {
    if (exchange.getRequestMethod != "POST") {
      respond(exchange, 405, "Method Not Allowed")
    } else {
      val params = queryParams(readBody(exchange))
      (params.get("account"), params.get("password"), params.get("otp")) match {
        case (Some(account), Some(password), Some(otp)) =>
          AccountReactivation.reactivate(account, password, otp) match {
            case Right(message) => respond(exchange, 200, message)
            case Left("Unknown account") => respond(exchange, 404, "Unknown account")
            case Left(error) => respond(exchange, 401, error)
          }
        case _ =>
          respond(exchange, 400, "Body parameters 'account', 'password' and 'otp' are required")
      }
    }
  }

  private def accountStatus(exchange: HttpExchange): Unit = {
    if (exchange.getRequestMethod != "GET") {
      respond(exchange, 405, "Method Not Allowed")
    } else {
      val params = queryParams(exchange.getRequestURI.getRawQuery)
      params.get("account") match {
        case Some(account) => respond(exchange, 200, if (AccountReactivation.isActive(account)) "active" else "inactive")
        case None => respond(exchange, 400, "Query parameter 'account' is required")
      }
    }
  }

  private def readBody(exchange: HttpExchange): String = {
    val in = exchange.getRequestBody
    try new String(in.readAllBytes(), StandardCharsets.UTF_8)
    finally in.close()
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
