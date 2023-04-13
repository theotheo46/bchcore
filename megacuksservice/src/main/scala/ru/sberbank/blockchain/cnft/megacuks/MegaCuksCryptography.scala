package ru.sberbank.blockchain.cnft.megacuks

import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cms.{CMSProcessableByteArray, CMSSignedData}
import org.bouncycastle.util.Selector
import ru.sberbank.blockchain.cnft.commons.{Base64R, Bytes, Collection, LoggingSupport, Result, ResultOps, isEqualBytes}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.PublicKeyAlgorithm.GOST
import ru.sberbank.blockchain.common.cryptography._
import ru.sberbank.blockchain.common.cryptography.model.ChallengeSpec


class MegaCuksCryptography(
    megaCuksService: MegaCuksServiceSpec[Result],
    config: MegaCuksConfiguration
) extends SignatureOperations[Result] with LoggingSupport {

    def keyServiceByOperation: MegaCuksKeyService = config.defaultKeyService

    def keyServiceByKey(key: KeyIdentifier): Option[MegaCuksKeyService] = {
        if (config.defaultKeyService.id == key) Some(config.defaultKeyService)
        else config.keyServiceOverrides.values.find(_.id == key)
    }

    override def requestNewKey(): Result[KeyIdentifier] = Result(keyServiceByOperation.id)

    override def createSignature(key: KeyIdentifier, content: Bytes): Result[Bytes] =
        for {
            maybeKeyService <- Result(keyServiceByKey(key))
            _ <- ResultOps.fromOption(maybeKeyService, s"Has no MCKey with $key keyId")
            _ <- Either.cond(content != null, (), "Content must be non empty")
            encodedB64Content <- Base64R.encode(content)
            response <-
                megaCuksService.requestSign(
                    SignByRequest(
                        rqUID = java.util.UUID.randomUUID().toString,
                        serviceName = key,
                        systemId = config.megaCuksSystemId,
                        fileData = encodedB64Content
                    )
                )
            statusCode = response.statusCode
            _ <- Either.cond(statusCode == 0, (), s"Error in Megacuks Service, statusCode: $statusCode")
            signedData <-
                Option(response.signedData)
                    .map(_.strip()) // could contain leading and trailing whitespaces, which are not allowed in B64
                    .filter(_.nonEmpty)
                    .toRight("Error getting cms signature, signedData is empty")
            signatureBytes <- Base64R.decode(signedData)
        } yield signatureBytes

    override def verifySignature(key: Bytes, content: Bytes, signature: Bytes): Result[Boolean] =
        for {
            spec <- Result(ChallengeSpec.parseFrom(key))
            keyBytes <-
                Either.cond(
                    spec.algorithm.equals(PublicKeyAlgorithm.GOST),
                    spec.value, s"Invalid key type: ${spec.algorithm}"
                )

            signedContent <- Result(new CMSProcessableByteArray(content))
            cms <- Result(new CMSSignedData(signedContent, signature))
            signer <- Result(cms.getSignerInfos.getSigners.iterator().next()) // get only one (first) signer
            certs <- Result(cms.getCertificates)
            cert <- Result(certs.getMatches(signer.getSID.asInstanceOf[Selector[X509CertificateHolder]]).iterator().next())
            _ <- Either.cond(isEqualBytes(cert.getSubjectPublicKeyInfo.getPublicKeyData.getBytes, keyBytes), (), "Public key mismatch")
            _ <- Either.cond(content.nonEmpty, (), "Content must be non empty")
            _ <- Either.cond(signature.nonEmpty, (), "Signature must be non empty")
            b64Message <- Base64R.encode(content)
            b64Signature <- Base64R.encode(signature)
            response <-
                megaCuksService.requestVerify(
                    CheckDocSignDetachReq(
                        rqUID = java.util.UUID.randomUUID().toString,
                        serviceName = config.megaCuksVerifyService,
                        systemId = config.megaCuksSystemId,
                        fileData = b64Message,
                        signData = b64Signature,
                        bsnCode = config.megaCuksBsnCode
                    )
                )
            statusCode = response.statusCode
            _ <- Either.cond(statusCode == 0, (), s"Got error from Megacuks Service, statusCode: $statusCode")
        } yield {
            val status = response.totalCheckResult.contains(MegaCuksConstants.TotalCheckResult.SignatureValid)
            logger.debug(s"Signature verification status $status")
            status
        }

    override def publicKey(keyIdentifier: KeyIdentifier): Result[Bytes] =
        for {
            key <- ResultOps.fromOption(keyServiceByKey(keyIdentifier), "Key not found")
            decodedCert <- Base64R.decode(key.certificateB64)
            x509Certificate <- Result(new X509CertificateHolder(decodedCert))
            extensions <- ResultOps.fromOption(Option(x509Certificate.getExtensions), "Invalid certificate: extensions are missing")
            maybeKeyUsage <- Result(Option(KeyUsage.fromExtensions(extensions)))
            keyUsage <- ResultOps.fromOption(maybeKeyUsage, "Invalid certificate: missing KeyUsage")
            _ <- Either.cond(keyUsage.hasUsages(KeyUsage.digitalSignature), (), "Invalid certificate: not a signing certificate")
            result =
                ChallengeSpec(
                    algorithm = GOST,
                    value = x509Certificate.getSubjectPublicKeyInfo.getPublicKeyData.getBytes,
                    extra = ""
                ).toByteArray
        } yield result

    override def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] =
        Result(keyServiceByKey(keyIdentifier).isDefined)

    override def findKeysByPublic(publicKeyBytes: Collection[Bytes]): Result[Collection[KeyIdentifier]] =
        Left("Method not supported")

    override def exportData(): Result[Bytes] = Result(Bytes.empty)
}
