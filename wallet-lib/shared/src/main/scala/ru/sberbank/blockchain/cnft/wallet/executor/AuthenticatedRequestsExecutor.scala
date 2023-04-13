package ru.sberbank.blockchain.cnft.wallet.executor

import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, ROps, asByteArray, asBytes}
import ru.sberbank.blockchain.cnft.gate.service.CNFTGateConstant
import ru.sberbank.blockchain.common.cryptography.AccessOperations
import tools.http.service.{HttpRequestsExecutor, NamedValue}

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.language.higherKinds

class AuthenticatedRequestsExecutor[R[+_]](
    executor: HttpRequestsExecutor[R],
    access: AccessOperations[R],
    accessDecoys: AccessDecoys[R]
)(implicit
    R: ROps[R]
) extends HttpRequestsExecutor[R] {

    import ROps.summonHasOps

    override def buildURL(base: String, parameters: Collection[NamedValue]): String =
        executor.buildURL(base, parameters)

    override def get(url: String, headers: Collection[NamedValue]): R[String] =
        executor.get(url, headers)

    override def post(url: String, body: String, headers: Collection[NamedValue]): R[String] = {
        for {
            myPublic <- access.publicKey()
            decoy <- accessDecoys.formDecoy(myPublic, CNFTGateConstant.DECOY_LENGTH)
            enrichedHeaders <-
                if (decoy._1.length > 1) {
                    access.createAccessToken(asBytes(body.getBytes(StandardCharsets.UTF_8)), decoy._1).map { token =>
                        headers :+
                            NamedValue(CNFTGateConstant.HEADER_ACCESS_TOKEN, toBase64(token)) :+
                            NamedValue(CNFTGateConstant.HEADER_ACCESS_TOKEN_DECOY, toBase64(decoy._2))
                    }
                } else R(headers)
            result <- executor.post(url, body, enrichedHeaders)
        } yield result
    }

    private def toBase64(bytes: Bytes): String =
        new String(Base64.getEncoder.encode(asByteArray(bytes)), StandardCharsets.UTF_8)

}

trait AccessKeys[R[+_]] {
    def list: R[Collection[Bytes]]
}

trait AccessDecoys[R[+_]] {
    def formDecoy(myPublic: Bytes, decoyLength: Int): R[(Collection[Bytes], Bytes)]
}