package ru.sberbank.blockchain.common.cryptography.bouncycastle

import org.bouncycastle.jce.provider.BouncyCastleProvider
import ru.sberbank.blockchain.cnft.commons.{Base64R, Bytes, Collection, LoggingSupport, Result}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.SignatureOperations
import ru.sberbank.blockchain.common.cryptography.model.ChallengeSpec
import ru.sberbank.blockchain.common.cryptography.store.CryptographicKeysStore

import java.security._
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}

/**
 * @author Alexey Polubelov
 */
class BouncyCastleSignatureOperations(
    settings: CryptographySettings,
    val keysStore: CryptographicKeysStore
) extends BouncyCastleCryptoOpsBase with SignatureOperations[Result] with LoggingSupport {

    // NOTE: keep the line below (registration of BC provider) at top of this class:
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(new BouncyCastleProvider)

    private val keyFactory = KeyFactory.getInstance(settings.keyAlgorithm)
    private val random = new SecureRandom()

    override def requestNewKey(): Result[KeyIdentifier] =
        for {
            keyGen <- Result {
                val keypairGen = KeyPairGenerator.getInstance(settings.keyAlgorithm)
                keypairGen.initialize(settings.keyLength, random)
                keypairGen
            }
            keyPair <- Result {
                keyGen.generateKeyPair()
            }
            pubKeyBytes = keyPair.getPublic.getEncoded
            pubKeyString <- Base64R.encode(pubKeyBytes)
            _ <- Result(keysStore.save(pubKeyString, keyPair.getPrivate.getEncoded))
        } yield pubKeyString


    override def createSignature(key: KeyIdentifier, content: Bytes): Result[Bytes] =
        for {
            keyBytes <- keysStore.get(key).toRight(s"[Create signature] Unknown key: $key")
            result <- Result {
                val key = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes))
                val signer = Signature.getInstance(settings.signatureAlgorithm, BouncyCastleProvider.PROVIDER_NAME)
                signer.initSign(key)
                signer.update(content)
                signer.sign()
            }
        } yield result


    override def verifySignature(keyBytes: Bytes, content: Bytes, signature: Bytes): Result[Boolean] = Result {
        val public = ChallengeSpec.parseFrom(keyBytes)
        val key = keyFactory.generatePublic(new X509EncodedKeySpec(public.value))
        val signer = Signature.getInstance(settings.signatureAlgorithm, BouncyCastleProvider.PROVIDER_NAME)
        signer.initVerify(key)
        signer.update(content)
        signer.verify(signature)
    }

    override def findKeysByPublic(publicKeyBytes: Collection[Bytes]): Result[Collection[String]] = Result.Fail("Method not supported")

    override def exportData(): Result[Bytes] = Result.Fail("Method not supported")
}
