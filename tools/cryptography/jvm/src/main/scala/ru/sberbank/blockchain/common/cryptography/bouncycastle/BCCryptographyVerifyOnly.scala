package ru.sberbank.blockchain.common.cryptography.bouncycastle

import org.bouncycastle.jce.provider.BouncyCastleProvider
import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, LoggingSupport, Result}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.SignatureOperations
import ru.sberbank.blockchain.common.cryptography.model.ChallengeSpec

import java.security._
import java.security.spec.X509EncodedKeySpec

/**
 * @author Alexey Polubelov
 */
class BCCryptographyVerifyOnly(
    settings: CryptographySettings
) extends SignatureOperations[Result] with LoggingSupport {
    // NOTE: keep the line below (registration of BC provider) at top of this class:
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(new BouncyCastleProvider)

    private val keyFactory = KeyFactory.getInstance(settings.keyAlgorithm)

    override def verifySignature(keyBytes: Bytes, content: Bytes, signature: Bytes): Result[Boolean] = Result {
        val public = ChallengeSpec.parseFrom(keyBytes)
        val key = keyFactory.generatePublic(new X509EncodedKeySpec(public.value))
        val signer = Signature.getInstance(settings.signatureAlgorithm, BouncyCastleProvider.PROVIDER_NAME)
        signer.initVerify(key)
        signer.update(content)
        signer.verify(signature)
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
