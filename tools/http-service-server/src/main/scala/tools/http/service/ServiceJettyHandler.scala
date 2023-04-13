package tools.http.service

import jakarta.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.http.MimeTypes
import org.slf4j.LoggerFactory
import ru.sberbank.blockchain.cnft.commons.Result
import tools.http.service.annotations.{HttpGet, HttpPost}
import utility.{Route, RoutesUtility}

import java.io.{ByteArrayOutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets


/**
 * @author Alexey Polubelov
 */
class ServiceJettyHandler[T](service: T)(implicit utility: RoutesUtility[T, Result, String]) extends HttpServlet {
    private val logger = LoggerFactory.getLogger(getClass)
    private val routes = utility.routes(service)

    private val GetBindings: Map[String, Route[String, Result]] = routes.flatMap { route =>
        route
            .functionInfo.meta
            .find(_.isInstanceOf[HttpGet])
            .map(a => a.asInstanceOf[HttpGet].url)
            .map(url => (url, route))
    }.toMap

    private val PostBindings = routes.flatMap { route =>
        route
            .functionInfo.meta
            .find(_.isInstanceOf[HttpPost])
            .map(a => a.asInstanceOf[HttpPost].url)
            .map(url => (url, route))
    }.toMap

    override protected def service(request: HttpServletRequest, response: HttpServletResponse): Unit = {
        request.getMethod match {
            case "GET" =>
                GetBindings.get(request.getPathInfo) match {
                    case Some(route) =>
                        val result = Result {
                            val arguments = route.argumentsInfo.map { parameter =>
                                Option(request.getParameter(parameter.name)).getOrElse(throw new Exception(s"Mandatory parameter ${parameter.name} is missing"))
                            }
                            route.execute(arguments)
                        }.joinRight
                        handleResult(result, response)

                    case None => response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                }

            case "POST" =>
                PostBindings.get(request.getPathInfo) match {
                    case Some(route) =>
                        val result = Result {
                            val body = {
                                request.setCharacterEncoding(StandardCharsets.UTF_8.name())
                                val bodyReader = request.getReader
                                try {
                                    val buffer = new ByteArrayOutputStream(1024)
                                    val out = new OutputStreamWriter(buffer, StandardCharsets.UTF_8)
                                    bodyReader.transferTo(out)
                                    out.flush()
                                    buffer.toString(StandardCharsets.UTF_8)
                                } finally {
                                    bodyReader.close()
                                }
                            }
                            val arguments =
                                if (route.argumentsInfo.length == 1) {
                                    Seq(body)
                                } else {
                                    upickle.default.read[Seq[String]](body)
                                }

                            route.execute(arguments)
                        }.joinRight
                        handleResult(result, response)

                    case None => response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                }

            case "OPTIONS" =>
                request.getHeader("Access-Control-Request-Method") match {
                    case "POST" => PostBindings.get(request.getPathInfo) match {
                        case Some(_) => response.setStatus(HttpServletResponse.SC_NO_CONTENT)
                        case None => response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                    }
                    case "GET" => GetBindings.get(request.getPathInfo) match {
                        case Some(_) => response.setStatus(HttpServletResponse.SC_NO_CONTENT)
                        case None => response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                    }
                    case _ => response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                }

            case methodName =>
                response.getWriter.print(s"Unsupported method type $methodName")
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)

        }
    }

    private def handleResult(result: Result[String], response: HttpServletResponse): Unit =
        result match {
            case Left(msg) =>
                logger.error(s"Got error: $msg")
                response.setContentType(MimeTypes.Type.TEXT_PLAIN.asString())
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                val out = response.getWriter
                out.print(msg)
                out.close()

            case Right(value) =>
                response.setContentType(MimeTypes.Type.APPLICATION_JSON.toString)
                response.setStatus(HttpServletResponse.SC_OK)
                val out = response.getWriter
                out.print(value)
                out.close()
        }

}
