package ru.sberbank.blockchain.cnft.gate.filter

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.{Filter, FilterChain, ServletRequest, ServletResponse}
import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.commons.LoggingSupport

import java.util.stream.Collectors

class AuditFilter(requests: Collection[String])
  extends Filter with LoggingSupport {
  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {

    val httpRequest = servletRequest.asInstanceOf[HttpServletRequest]
    val path = httpRequest.getPathInfo

    httpRequest.getMethod match {
      case "POST" =>
        if (requests.contains(path)) {
          try {
              val bodyStr = httpRequest.getReader.lines().collect(Collectors.joining(System.lineSeparator()))
              val userAddr = getClientIp(httpRequest)
              logger.info(s"AuditFilter: userAddr=$userAddr path=$path body=$bodyStr")
          } catch {
            case e: Exception =>
              logger.error(s"Exception in AuditFilter when logging $path -- ${e.toString}" )
              filterChain.doFilter(servletRequest, servletResponse)
          }
        }
        filterChain.doFilter(servletRequest, servletResponse)

      case _ =>
        filterChain.doFilter(servletRequest, servletResponse)
    }
  }

  private def getClientIp(request: HttpServletRequest): String = {
    if (request.getHeader("X-Forwarded-For") != null) request.getHeader("X-Forwarded-For")
    else request.getRemoteAddr
  }
}
