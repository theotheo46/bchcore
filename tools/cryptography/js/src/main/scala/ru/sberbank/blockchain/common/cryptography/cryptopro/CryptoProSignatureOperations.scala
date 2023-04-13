package ru.sberbank.blockchain.common.cryptography.cryptopro

import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{Base64R, Bytes, Collection, Result, asByteArray, asBytes}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.PublicKeyAlgorithm.GOST
import ru.sberbank.blockchain.common.cryptography._
import ru.sberbank.blockchain.common.cryptography.model.ChallengeSpec
import upickle.default._

import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}


/**
 * @author Vladimir Sidorov
 */
@JSExportAll
@JSExportTopLevel("CryptoProSignatureOperations")
class CryptoProSignatureOperations(
    selectKeyCallback: SelectKeyCallback
) extends SignatureOperations[Result] {

    override def requestNewKey(): Result[KeyIdentifier] =
        for {
            certificates <- CryptoProLib.initCertList()
            thumbprint <- selectKeyCallback.selectKey(certificates)
            rawCryptoProCert <- CryptoProLib.exportCertificateByThumbprint(thumbprint)
            cert <- Result(read[CryptoProCertificate](rawCryptoProCert))
        } yield cert.thumbprint

    override def verifySignature(key: Bytes, content: Bytes, signature: Bytes): Result[Boolean] = {
        val b64Content = new String(Base64.getEncoder.encode(asByteArray(content)))
        for {
            b64Sig <- Base64R.encode(asByteArray(signature))
            _ <- CryptoProLib.verify(b64Sig, b64Content)
        } yield true
    }

    override def createSignature(key: KeyIdentifier, content: Bytes): Result[Bytes] = {
        val b64Content = new String(Base64.getEncoder.encode(asByteArray(content)))
        for {
            signedData <- CryptoProLib.signTextByThumbprint(b64Content, key)
            signatureBytes <- Base64R.decode(signedData)
        } yield asBytes(signatureBytes)
    }

    override def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] =
        CryptoProLib.exportCertificateByThumbprint(keyIdentifier)
            .toFuture
            .map(_ => true)
            .recover { case _ => false }
            .toJSPromise

    override def publicKey(keyIdentifier: KeyIdentifier): Result[Bytes] = {
        for {
            rawCryptoProCert <- CryptoProLib.exportCertificateByThumbprint(keyIdentifier)
            cert <- Result(read[CryptoProCertificate](rawCryptoProCert))
            decodedPubKey <- Base64R.decode(cert.pubKey)
        } yield
                asBytes(
                    ChallengeSpec(
                        algorithm = GOST,
                        value = asBytes(decodedPubKey),
                        extra = ""
                    ).toByteArray
                )
    }

    override def findKeysByPublic(publicKeyBytes: Collection[Bytes]): Result[Collection[String]] =
        Result.Fail("Method not supported")

    override def exportData(): Result[Bytes] = Result {
        Bytes.empty //TODO: think about - do we need anything here?
    }
}


