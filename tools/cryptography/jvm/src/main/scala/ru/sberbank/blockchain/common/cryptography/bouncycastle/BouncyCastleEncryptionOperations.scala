package ru.sberbank.blockchain.common.cryptography.bouncycastle

import org.bouncycastle.crypto.engines.AESWrapEngine
import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV, ParametersWithRandom}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.ResultOps.fromOption
import ru.sberbank.blockchain.cnft.commons.{Base64R, Bytes, Collection, LoggingSupport, Result, ResultOps, asBytes, isEqualBytes}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.EncryptionOperations
import ru.sberbank.blockchain.common.cryptography.model.{EncryptedMessage, KeyPair, KeysList, WrappedForPublic}
import ru.sberbank.blockchain.common.cryptography.store.CryptographicKeysStore

import java.nio.charset.StandardCharsets
import java.security._
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.Base64
import javax.crypto.spec.{GCMParameterSpec, SecretKeySpec}
import javax.crypto.{Cipher, KeyAgreement, KeyGenerator}

/**
 * @author Alexey Polubelov
 */
class BouncyCastleEncryptionOperations(
    settings: EncryptDecryptCryptographySettings,
    val keysStore: CryptographicKeysStore
) extends BouncyCastleCryptoOpsBase with EncryptionOperations[Result] with LoggingSupport {

    // NOTE: keep the line below (registration of BC provider) at top of this class:
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(new BouncyCastleProvider)

    private val random = new SecureRandom()

    override def requestNewKey(): Result[KeyIdentifier] = generateECDHKeyPair()

    override def encrypt(message: Bytes, receiverPublicKeys: Collection[Bytes]): Result[Bytes] =
        for {
            iv <- Result { // FIXME use crypto.random
                val bytes = new Bytes(16)
                random.nextBytes(bytes)
                bytes
            }
            aesKey <- Result(createAESKey())
            encryptedMessage <- Result(encryptAESGCM(message, aesKey, iv))
            identifier <- generateECDHKeyPair()
            privateKey <-
                keysStore
                    .get(identifier)
                    .toRight(s"[Encrypt] Unknown key: $identifier")
            wrappedKeys <-
                receiverPublicKeys.toSeq.mapR { pk =>
                    Result {
                        val dk = deriveKey(privateKey, pk)
                        WrappedForPublic(
                            wrapKey(aesKey, dk, iv),
                            pk
                        )
                    }
                }
            //            _ = {
            //                val encoder = Base64.getEncoder
            //                val keysList = wrappedKeys.map(_.publicKey).map(encoder.encodeToString).mkString("\n\t - ", "\n\t - ", "\n\n")
            //                logger.info(s"[ENCRYPT] Message encrypted for keys:$keysList")
            //            }
        } yield
            EncryptedMessage(
                cipherText = encryptedMessage,
                iv = iv,
                senderPublicKey = Base64.getDecoder.decode(identifier),
                wrappedKeys = wrappedKeys.toArray
            ).toByteArray


    override def decrypt(decryptionData: Bytes, keyIdentifier: KeyIdentifier): Result[Bytes] =
        for {
            encryptResponse <- Result(EncryptedMessage.parseFrom(decryptionData))
            //            _ = {
            //                val encoder = Base64.getEncoder
            //                val keysList = encryptResponse.wrappedKeys.map(_.publicKey).map(encoder.encodeToString).mkString("\n\t - ", "\n\t - ", "\n\n")
            //                logger.info(s"[DECRYPT] Message encrypted for keys:$keysList")
            //                logger.info(s"[DECRYPT] Key to decrypt: $keyIdentifier")
            //            }
            publicKey <- Base64R.decode(keyIdentifier)
            wrappedKey <- fromOption(
                encryptResponse.wrappedKeys
                    .find { k => isEqualBytes(k.publicKey, asBytes(publicKey)) }
                    .map(_.wrappedKey),
                "Public key not in list"
            )
            keyReceiver <- keysStore.get(keyIdentifier).toRight(s"[Decrypt] Unknown key: $keyIdentifier")
            receiverDerivedKey <- Result(deriveKey(keyReceiver, encryptResponse.senderPublicKey))
            unWrappedKey <- Result(unWrapKey(wrappedKey, receiverDerivedKey, encryptResponse.iv))
            result <- Result(decryptAESGCM(encryptResponse.cipherText, unWrappedKey, encryptResponse.iv))
        } yield result

    //    override def importEncryptionKey(publicKey: Bytes, privateKey: Bytes): Result[String] =
    //        for {
    //            keyFactory <- Result(KeyFactory.getInstance(settings.ECAlgorithm))
    //            privateKeySpec <- Result(new PKCS8EncodedKeySpec(privateKey))
    //            publicKeySpec <- Result(new X509EncodedKeySpec(publicKey))
    //            publicKeyEncoded <- Result(keyFactory.generatePublic(publicKeySpec).getEncoded)
    //            privateKeyEncoded <- Result(keyFactory.generatePrivate(privateKeySpec).getEncoded)
    //            publicKeyB64 <- Base64R.encode(publicKeyEncoded)
    //            _ <- Result(keysStore.save(publicKeyB64, privateKeyEncoded))
    //        } yield publicKeyB64
    //
    //    override def exportEncryptionKey(keyIdentifier: KeyIdentifier): Result[Bytes] =
    //        for {
    //            privateKey <- keysStore
    //              .get(keyIdentifier)
    //              .toRight(s"[Encrypt] Unknown key: $keyIdentifier")
    //        } yield privateKey


    def createAESKey(): Bytes = {
        val AES_KEY_SIZE = 256
        val keyGenerator = KeyGenerator.getInstance(settings.symmetricKeyAlgorithm)
        keyGenerator.init(AES_KEY_SIZE, random)
        keyGenerator.generateKey().getEncoded
    }

    def encryptAESGCM(content: Bytes, key: Bytes, IV: Bytes): Bytes = {
        val cipher = Cipher.getInstance(settings.symmetricKeyCipherAlgorithm)
        val keySpec = new SecretKeySpec(key, settings.symmetricKeyAlgorithm)
        val gcmParameterSpec = new GCMParameterSpec(settings.symmetricTagLength, IV)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec)
        cipher.doFinal(content)
    }


    def decryptAESGCM(content: Bytes, key: Bytes, IV: Bytes): Bytes = {
        val cipher = Cipher.getInstance(settings.symmetricKeyCipherAlgorithm)
        val keySpec = new SecretKeySpec(key, settings.symmetricKeyAlgorithm)
        val gcmParameterSpec = new GCMParameterSpec(settings.symmetricTagLength, IV)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec)
        cipher.doFinal(content)
    }


    def generateECDHKeyPair(): Result[KeyIdentifier] =
        for {
            keyPair <- Result {
                val curve = ECNamedCurveTable.getParameterSpec(settings.ECCurve)

                val secureRandom = new SecureRandom

                val keyFactory = KeyPairGenerator.getInstance(settings.ECAlgorithm)
                keyFactory.initialize(curve, secureRandom)
                keyFactory.generateKeyPair()
            }

            privateKeyEncoded = keyPair.getPrivate.getEncoded
            publicKeyEncoded = keyPair.getPublic.getEncoded
            publicKeyB64 <- Base64R.encode(publicKeyEncoded)

            _ <- Result(keysStore.save(publicKeyB64, privateKeyEncoded))

        } yield publicKeyB64

    def deriveKey(privateKey: Bytes, publicKey: Bytes): Bytes = {
        val publicKeySpec = new X509EncodedKeySpec(publicKey)
        val keyFactory = KeyFactory.getInstance(settings.ECAlgorithm)
        val pubKey = keyFactory.generatePublic(publicKeySpec)

        val privateKeySpec = new PKCS8EncodedKeySpec(privateKey)
        val privKey = keyFactory.generatePrivate(privateKeySpec)

        val keyAgreement = KeyAgreement.getInstance(settings.ECAlgorithm)
        keyAgreement.init(privKey)
        keyAgreement.doPhase(pubKey, true)

        keyAgreement.generateSecret(settings.symmetricKeyCipherAlgorithm).getEncoded
    }

    def wrapKey(senderKey: Bytes, derivedKey: Bytes, IV: Bytes): Bytes = {
        val secretKeySpec = new SecretKeySpec(senderKey, settings.symmetricKeyAlgorithm)
        val derivedKeySpec = new SecretKeySpec(derivedKey, settings.symmetricKeyAlgorithm)
        val gcmSpec = new GCMParameterSpec(settings.symmetricTagLength, IV)
        val cipher = Cipher.getInstance(settings.symmetricKeyCipherAlgorithm)
        cipher.init(Cipher.WRAP_MODE, derivedKeySpec, gcmSpec)
        cipher.wrap(secretKeySpec)
    }

    def unWrapKey(senderWrapKey: Bytes, receiverDeriveKey: Bytes, IV: Bytes): Bytes = {
        val derivedKeySpec = new SecretKeySpec(receiverDeriveKey, settings.symmetricKeyCipherAlgorithm)
        val gcmSpec = new GCMParameterSpec(128, IV)

        val cipher = Cipher.getInstance(settings.symmetricKeyCipherAlgorithm)
        cipher.init(Cipher.UNWRAP_MODE, derivedKeySpec, gcmSpec)
        val aesKey = cipher.unwrap(senderWrapKey, settings.symmetricKeyCipherAlgorithm, Cipher.SECRET_KEY).getEncoded
        new SecretKeySpec(aesKey, settings.symmetricKeyCipherAlgorithm).getEncoded
    }


    def wrapKeyRFC3394(senderKey: Bytes, derivedKey: Bytes, IV: Bytes): Bytes = {
        val wrapper = new AESWrapEngine(true)
        wrapper.init(true,
            new ParametersWithRandom(
                new ParametersWithIV(
                    new KeyParameter(derivedKey), IV
                ), random
            )
        )
        wrapper.wrap(senderKey, 0, senderKey.length)
    }

    def unWrapKeyRFC3394(senderWrapKey: Bytes, receiverDeriveKey: Bytes, IV: Bytes): Bytes = {
        val wrapper = new AESWrapEngine(true)
        wrapper.init(false, new ParametersWithIV(new KeyParameter(receiverDeriveKey), IV))
        wrapper.unwrap(senderWrapKey, 0, senderWrapKey.length)
    }

    override def exportData(): Result[Bytes] = Result {
        KeysList(
            keysStore.list.map { case (id, key) =>
                KeyPair(
                    pk = Base64.getDecoder.decode(id.getBytes(StandardCharsets.UTF_8)),
                    sk = key
                )
            }.toArray
        ).toByteArray
    }
}
