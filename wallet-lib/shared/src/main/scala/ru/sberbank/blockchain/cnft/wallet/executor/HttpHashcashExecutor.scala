package ru.sberbank.blockchain.cnft.wallet.executor

import ru.sberbank.blockchain.cnft.commons.ROps.summonHasOps
import ru.sberbank.blockchain.cnft.commons.{Collection, ROps, asByteArray}
import ru.sberbank.blockchain.cnft.gate.service.CNFTGateConstant.{HeaderPoW, HeaderPoWExtra, HeaderPoWNonce}
import ru.sberbank.blockchain.cnft.gate.service.POWServiceSpec
import ru.sberbank.blockchain.common.cryptography.Hashcash
import tools.http.service.{HttpRequestsExecutor, NamedValue}

import java.util.Base64
import scala.language.higherKinds
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("HttpHashcashExecutor")
class HttpHashcashExecutor[R[+_]](
    defaultExecutor: HttpRequestsExecutor[R],
    hashcash: Hashcash[R],
    PoWService: POWServiceSpec[R]
)(implicit
    val R: ROps[R],
) extends HttpRequestsExecutor[R] {

    @JSExport
    override def buildURL(base: String, parameters: Collection[NamedValue]): String =
        defaultExecutor.buildURL(base, parameters)

    @JSExport
    override def get(url: String, headers: Collection[NamedValue]): R[String] =
        defaultExecutor.get(url, headers)

    @JSExport
    override def post(url: String, body: String, headers: Collection[NamedValue]): R[String] =
        for {
            difficulty <- PoWService.powDifficulty
            proofHeaders <- getHashcashProofHeaders(body, difficulty)
            result <- defaultExecutor.post(url, body, headers ++ proofHeaders)
        } yield result

    private def getHashcashProofHeaders(content: String, difficulty: Int): R[Array[NamedValue]] =
        for {
            hashcashResult <- hashcash.pickUpNonce(content, difficulty)
            encodedHash = Base64.getEncoder.encodeToString(asByteArray(hashcashResult.hash))
            encodedExtra = Base64.getEncoder.encodeToString(asByteArray(hashcashResult.extra))
            headers =
                Array(
                    NamedValue(HeaderPoW, encodedHash),
                    NamedValue(HeaderPoWExtra, encodedExtra),
                    NamedValue(HeaderPoWNonce, hashcashResult.nonce.toString)
                    //NamedValue("hashcash_difficulty", difficulty.toString))
                )
        } yield headers
}