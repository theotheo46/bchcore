package ru.sberbank.blockchain.common.cryptography

import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.cms.{CMSProcessableByteArray, CMSSignedData}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.Selector
import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, LoggingSupport, Result, isEqualBytes}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.PublicKeyAlgorithm.{GOST, HD}
import ru.sberbank.blockchain.common.cryptography.hd.Bip32ELOps
import ru.sberbank.blockchain.common.cryptography.model.ChallengeSpec

import java.security._

/**
 * @author Alexey Polubelov
 */
class VerifyOnlyCryptography extends SignatureOperations[Result] with LoggingSupport {
    // NOTE: keep the line below (registration of BC provider) at top of this class:
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(new BouncyCastleProvider)

    private val cryptoImpl = new Bip32ELOps(ECNamedCurveTable.getParameterSpec("secp256k1"))

    override def verifySignature(keyBytes: Bytes, content: Bytes, signature: Bytes): Result[Boolean] =
        for {
            spec <- Result(ChallengeSpec.parseFrom(keyBytes))
            result <- spec.algorithm match {
                case HD =>
                    cryptoImpl.verify(spec.value, content, signature)
                case GOST =>
                    verifyCMS(spec.value, content, signature)
            }
        } yield result

    def verifyCMS(keyBytes: Bytes, content: Bytes, signature: Bytes): Result[Boolean] = Result {
        val signedContent = new CMSProcessableByteArray(content)
        val cms = new CMSSignedData(signedContent, signature)
        val signer = cms.getSignerInfos.getSigners.iterator().next() // verify only one (first) signer
        val certs = cms.getCertificates
        val cert = certs.getMatches(signer.getSID.asInstanceOf[Selector[X509CertificateHolder]]).iterator().next()
        val verifier = new JcaSimpleSignerInfoVerifierBuilder().build(cert)
        if (isEqualBytes(cert.getSubjectPublicKeyInfo.getPublicKeyData.getBytes, keyBytes)) {
            signer.verify(verifier)
        } else {
            false
        }
    }

    override def requestNewKey(): Result[KeyIdentifier] =
        Left("Unsupported method")

    override def createSignature(key: KeyIdentifier, content: Bytes): Result[Bytes] =
        Left("Unsupported method")

    override def publicKey(keyIdentifier: KeyIdentifier): Result[Bytes] =
        Left("Unsupported method")

    override def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] =
        Left("Unsupported method")

    override def findKeysByPublic(publicKeyBytes: Collection[Bytes]): Result[Collection[KeyIdentifier]] =
        Left("Unsupported method")

    override def exportData(): Result[Bytes] =
        Left("Unsupported method")

}
