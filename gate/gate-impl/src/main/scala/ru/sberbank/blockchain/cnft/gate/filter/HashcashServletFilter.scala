package ru.sberbank.blockchain.cnft.gate.filter

import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import jakarta.servlet.{Filter, FilterChain, ServletRequest, ServletResponse}
import ru.sberbank.blockchain.cnft.commons.{Base64R, Result}
import ru.sberbank.blockchain.cnft.gate.service.CNFTGateConstant.{HeaderPoW, HeaderPoWExtra, HeaderPoWNonce}
import ru.sberbank.blockchain.common.cryptography.Hashcash

import scala.util.Try

class HashcashServletFilter(
    hashcash: Hashcash[Result],
    difficulty: Int
) extends Filter {

    override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
        val httpRequest = request.asInstanceOf[HttpServletRequest]
        if (httpRequest.getMethod == "POST") {
            validatePoW(httpRequest) match {
                case Left(msg) =>
                    response.asInstanceOf[HttpServletResponse]
                        .sendError(HttpServletResponse.SC_BAD_REQUEST, msg)

                case Right(false) =>
                    response.asInstanceOf[HttpServletResponse]
                        .sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid PoW")

                case Right(true) => // all fine - do nothing
            }
        }
        chain.doFilter(httpRequest, response)
    }

    private def validatePoW(req: HttpServletRequest): Result[Boolean] = {
        for {
            hash <- Option(req.getHeader(HeaderPoW)).toRight(s"missing '$HeaderPoW' in header")
            extra <- Option(req.getHeader(HeaderPoWExtra)).toRight(s"missing '$HeaderPoWExtra' in header")
            nonceS <- Option(req.getHeader(HeaderPoWNonce)).toRight(s"missing '$HeaderPoWNonce' in header")
            nonce <- Try(nonceS.toInt).toOption.toRight(s"invalid '$HeaderPoWNonce' value: '$nonceS'")

            decodedHash <- Base64R.decode(hash)
            decodedExtra <- Base64R.decode(extra)

            reader = req.getReader
            content = Stream.continually(reader.readLine()).takeWhile(_ != null).mkString

            correct <- hashcash.isCorrect(decodedHash, content, decodedExtra, nonce)
                .map(_ && hashcash.verifyDifficulty(decodedHash, difficulty))
        } yield correct
    }
}