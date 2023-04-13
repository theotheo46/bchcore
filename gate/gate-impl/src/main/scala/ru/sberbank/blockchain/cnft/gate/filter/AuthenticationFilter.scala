package ru.sberbank.blockchain.cnft.gate.filter

import jakarta.servlet._
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECPoint
import ru.sberbank.blockchain.cnft.commons.{Base64R, LoggingSupport, Result}
import ru.sberbank.blockchain.cnft.gate.service.{CNFTGate, CNFTGateConstant}
import ru.sberbank.blockchain.common.cryptography.DecoyProvider
import ru.sberbank.blockchain.common.cryptography.bouncycastle.{BouncyCastleHasher, EllipticOps}
import ru.sberbank.blockchain.common.cryptography.sag.RingSigner

import java.io.{ByteArrayOutputStream, IOException, OutputStreamWriter}
import java.math.BigInteger
import java.nio.charset.StandardCharsets

class AuthenticationFilter(
    gate: CNFTGate[Result]
) extends Filter with LoggingSupport {

    private val curve = ECNamedCurveTable.getParameterSpec("secp256k1")
    private val rs = new RingSigner[Result, ECPoint](
        BouncyCastleHasher,
        new EllipticOps(curve, compressed = false),
        (BigInteger.ZERO, null) // TODO: split the Signer
    )
    private val decoyProvider = new DecoyProvider[Result]()

    @throws[IOException]
    @throws[ServletException]
    override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
        val httpRequest = servletRequest.asInstanceOf[HttpServletRequest]
        val path = httpRequest.getPathInfo
        httpRequest.getMethod match {
            case "POST" =>
                val isValid =
                    gate.listMembers.map(_.map(_.accessPublic)).flatMap { keys =>
                        if (keys.length > 1)
                            for {
                                token <-
                                    Option(httpRequest.getHeader(CNFTGateConstant.HEADER_ACCESS_TOKEN))
                                        .toRight("Access token is missing")
                                decoy <-
                                    Option(httpRequest.getHeader(CNFTGateConstant.HEADER_ACCESS_TOKEN_DECOY))
                                        .toRight("Access decoy is missing")
                                signature <- Base64R.decode(token)
                                decoyIds <- Base64R.decode(decoy)
                                decoyKeys <- decoyProvider.decoyKeys(decoyIds, keys)
                                // _ <- Result {logger.debug(s"AUTH total keys ${keys.length} decoy ${decoyKeys.length}")}
                                body <- Result(readBody(httpRequest))
                                valid <- rs.verify(body, decoyKeys, signature)
                            } yield valid
                        else {
                            logger.debug("Access token check skipped: there are less less then 2 member")
                            Result(true)
                        }
                    }

                isValid match {
                    case Right(true) =>
                        logger.debug(s"AUTH transaction authorized [$path]")
                        filterChain.doFilter(servletRequest, servletResponse)

                    case Right(false) =>
                        logger.warn(s"Got invalid access token [$path]")
                        servletResponse
                            .asInstanceOf[HttpServletResponse]
                            .setStatus(HttpServletResponse.SC_FORBIDDEN)

                    case Left(msg) =>
                        logger.warn(s"Failed to verify access token [$path]: $msg")
                        servletResponse
                            .asInstanceOf[HttpServletResponse]
                            .setStatus(HttpServletResponse.SC_FORBIDDEN)
                }

            case _ =>
                filterChain.doFilter(servletRequest, servletResponse)
        }
    }

    private def readBody(httpRequest: HttpServletRequest): Array[Byte] = {
        httpRequest match {
            case buffered: CachedBodyHttpServletRequestWrapper =>
                logger.debug("Using cached body")
                buffered.cachedBody

            case _ =>
                val bodyReader = httpRequest.getReader
                try {
                    val buffer = new ByteArrayOutputStream(1024)
                    val out = new OutputStreamWriter(buffer, StandardCharsets.UTF_8)
                    bodyReader.transferTo(out)
                    out.flush()
                    buffer.toByteArray
                } finally {
                    bodyReader.close()
                }
        }
    }
}
