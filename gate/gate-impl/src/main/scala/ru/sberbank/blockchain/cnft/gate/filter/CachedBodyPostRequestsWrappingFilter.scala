package ru.sberbank.blockchain.cnft.gate.filter

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.{Filter, FilterChain, ServletRequest, ServletResponse}

class CachedBodyPostRequestsWrappingFilter extends Filter {

    override def doFilter(
        request: ServletRequest, response: ServletResponse, chain: FilterChain
    ): Unit =
        request.asInstanceOf[HttpServletRequest].getMethod match {
            case "POST" =>
                chain.doFilter(
                    new CachedBodyHttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest]),
                    response
                )
            case _ => chain.doFilter(request, response)
        }
}
