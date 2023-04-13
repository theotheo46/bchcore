//package ru.sberbank.blockchain.common.cryptography.bouncycastle
//
//import org.bouncycastle.crypto.engines.AESWrapEngine
//import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV, ParametersWithRandom}
//import org.bouncycastle.jce.ECNamedCurveTable
//import org.bouncycastle.jce.provider.BouncyCastleProvider
//import ru.sberbank.blockchain.cnft.commons.{Base64R, Bytes, LoggingSupport, Result}
//import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
//import ru.sberbank.blockchain.common.cryptography.bouncycastle.CryptographySettings
//import ru.sberbank.blockchain.common.cryptography.bouncycastle.CryptographySettings.AES256GCM
//import ru.sberbank.blockchain.common.cryptography.model.EncryptedMessage
//
//import java.security._
//import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
//import java.util.Base64
//import javax.crypto.spec.{GCMParameterSpec, SecretKeySpec}
//import javax.crypto.{Cipher, KeyAgreement, KeyGenerator}
//
///**
// * @author Alexey Polubelov
// */
//class BCCryptography(
//    settings: CryptographySettings,
//    keysStore: CryptographicKeysStore
//) extends Cryptography[Result] with LoggingSupport {
//    // NOTE: keep the line below (registration of BC provider) at top of this class:
//    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(new BouncyCastleProvider)
//
//    private val keyFactory = KeyFactory.getInstance(settings.keyAlgorithm)
//    private val random = new SecureRandom()
//
//    private val CESettings = AES256GCM
//
//    override def requestNewKey(): Result[KeyIdentifier] =
//        reason match {
//            case EncryptionOperation => generateECDHKeyPair()
//            case _ =>
//                for {
//                    keyGen <- Result {
//                        val keypairGen = KeyPairGenerator.getInstance(settings.keyAlgorithm)
//                        keypairGen.initialize(settings.keyLength, random)
//                        keypairGen
//                    }
//
//                    keyPair <- Result {
//                        keyGen.generateKeyPair()
//                    }
//
//                    pubKeyBytes = keyPair.getPublic.getEncoded
//
//                    pubKeyString <- Base64R.encode(pubKeyBytes)
//
//                    _ <- Result(keysStore.save(pubKeyString, keyPair.getPrivate.getEncoded))
//                } yield pubKeyString
//        }
//
//
//    override def createSignature(key: KeyIdentifier, content: Bytes): Result[Bytes] =
//        for {
//            keyBytes <- keysStore.get(key).toRight(s"[Create signature] Unknown key: $key")
//            result <- Result {
//                val key = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes))
//                val signer = Signature.getInstance(settings.signatureAlgorithm, BouncyCastleProvider.PROVIDER_NAME)
//                signer.initSign(key)
//                signer.update(content)
//                signer.sign()
//            }
//        } yield result
//
//
//    override def verifySignature(keyBytes: Bytes, content: Bytes, signature: Bytes): Result[Boolean] = Result {
//        val key = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes))
//        val signer = Signature.getInstance(settings.signatureAlgorithm, BouncyCastleProvider.PROVIDER_NAME)
//        signer.initVerify(key)
//        signer.update(content)
//        signer.verify(signature)
//    }
//
//    override def publicKey(keyIdentifier: KeyIdentifier): Result[Option[Bytes]] = Result {
//        Base64R.decode(keyIdentifier).toOption
//    }
//
//    override def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] = Result {
//        !keysStore.get(keyIdentifier).forall(_.isEmpty)
//    }
//
//
//    def createAESKey(): Bytes = {
//        val AES_KEY_SIZE = 256
//        val keyGenerator = KeyGenerator.getInstance(CESettings.symmetricKeyAlgorithm)
//        keyGenerator.init(AES_KEY_SIZE, random)
//        keyGenerator.generateKey().getEncoded
//    }
//
//
//    def encryptAESGCM(content: Bytes, key: Bytes, IV: Bytes): Bytes = {
//        val cipher = Cipher.getInstance(CESettings.symmetricKeyCipherAlgorithm)
//        val keySpec = new SecretKeySpec(key, CESettings.symmetricKeyAlgorithm)
//        val gcmParameterSpec = new GCMParameterSpec(CESettings.symmetricTagLength, IV)
//        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec)
//        cipher.doFinal(content)
//    }
//
//
//    def decryptAESGCM(content: Bytes, key: Bytes, IV: Bytes): Bytes = {
//        val cipher = Cipher.getInstance(CESettings.symmetricKeyCipherAlgorithm)
//        val keySpec = new SecretKeySpec(key, CESettings.symmetricKeyAlgorithm)
//        val gcmParameterSpec = new GCMParameterSpec(CESettings.symmetricTagLength, IV)
//        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec)
//        cipher.doFinal(content)
//    }
//
//
//    def generateECDHKeyPair(): Result[KeyIdentifier] =
//        for {
//            keyPair <- Result {
//                val curve = ECNamedCurveTable.getParameterSpec(CESettings.ECCurve)
//
//                val secureRandom = new SecureRandom
//
//                val keyFactory = KeyPairGenerator.getInstance(CESettings.ECAlgorithm)
//                keyFactory.initialize(curve, secureRandom)
//                keyFactory.generateKeyPair()
//            }
//
//            privateKeyEncoded = keyPair.getPrivate.getEncoded
//            publicKeyEncoded = keyPair.getPublic.getEncoded
//            publicKeyB64 <- Base64R.encode(publicKeyEncoded)
//
//            _ <- Result(keysStore.save(publicKeyB64, privateKeyEncoded))
//
//        } yield publicKeyB64
//
//
//
//    def deriveKey(privateKey: Bytes, publicKey: Bytes): Bytes = {
//        val publicKeySpec = new X509EncodedKeySpec(publicKey)
//        val keyFactory = KeyFactory.getInstance(CESettings.ECAlgorithm)
//        val pubKey = keyFactory.generatePublic(publicKeySpec)
//
//        val privateKeySpec = new PKCS8EncodedKeySpec(privateKey)
//        val privKey = keyFactory.generatePrivate(privateKeySpec)
//
//        val keyAgreement = KeyAgreement.getInstance(CESettings.ECAlgorithm)
//        keyAgreement.init(privKey)
//        keyAgreement.doPhase(pubKey, true)
//
//        keyAgreement.generateSecret(CESettings.symmetricKeyCipherAlgorithm).getEncoded
//    }
//
//    def wrapKey(senderKey: Bytes, derivedKey: Bytes, IV: Bytes): Bytes = {
//        val secretKeySpec = new SecretKeySpec(senderKey, CESettings.symmetricKeyAlgorithm)
//        val derivedKeySpec = new SecretKeySpec(derivedKey, CESettings.symmetricKeyAlgorithm)
//        val gcmSpec = new GCMParameterSpec(CESettings.symmetricTagLength, IV)
//        val cipher = Cipher.getInstance(CESettings.symmetricKeyCipherAlgorithm)
//        cipher.init(Cipher.WRAP_MODE, derivedKeySpec, gcmSpec)
//        cipher.wrap(secretKeySpec)
//    }
//
//    def unWrapKey(senderWrapKey: Bytes, receiverDeriveKey: Bytes, IV: Bytes): Bytes = {
//        val derivedKeySpec = new SecretKeySpec(receiverDeriveKey, CESettings.symmetricKeyCipherAlgorithm)
//        val gcmSpec = new GCMParameterSpec(128, IV)
//
//        val cipher = Cipher.getInstance(CESettings.symmetricKeyCipherAlgorithm)
//        cipher.init(Cipher.UNWRAP_MODE, derivedKeySpec, gcmSpec)
//        val aesKey = cipher.unwrap(senderWrapKey, CESettings.symmetricKeyCipherAlgorithm, Cipher.SECRET_KEY).getEncoded
//        new SecretKeySpec(aesKey, CESettings.symmetricKeyCipherAlgorithm).getEncoded
//    }
//
//
//    def wrapKeyRFC3394(senderKey: Bytes, derivedKey: Bytes, IV: Bytes): Bytes = {
//        val wrapper = new AESWrapEngine(true)
//        wrapper.init(true,
//            new ParametersWithRandom(
//                new ParametersWithIV(
//                    new KeyParameter(derivedKey), IV
//                ), random
//            )
//        )
//        wrapper.wrap(senderKey, 0, senderKey.length)
//    }
//
//    def unWrapKeyRFC3394(senderWrapKey: Bytes, receiverDeriveKey: Bytes, IV: Bytes): Bytes = {
//        val wrapper = new AESWrapEngine(true)
//        wrapper.init(false, new ParametersWithIV(new KeyParameter(receiverDeriveKey), IV))
//        wrapper.unwrap(senderWrapKey, 0, senderWrapKey.length)
//    }
//
//    override def encrypt(message: Bytes, receiverPublicKey: Bytes): Result[Bytes] =
//        for {
//            iv <- Result {
//                //Array.fill(8)((scala.util.Random.nextInt(256) - 128).toByte)
//                val bytes = new Bytes(8)
//                random.nextBytes(bytes)
//                bytes
//            }
//            aesKey <- Result(createAESKey())
//            encryptedMessage <- Result(encryptAESGCM(message, aesKey, iv))
//            identifier <- generateECDHKeyPair()
//            privateKey <- keysStore
//                .get(identifier)
//                .toRight(s"[Encrypt] Unknown key: ${identifier}")
//            derivedKey <- Result(deriveKey(privateKey, receiverPublicKey))
//            wrappedKey <- Result(wrapKey(aesKey, derivedKey, iv))
//        } yield
//            EncryptedMessage(
//                cipherText = encryptedMessage,
//                iv = iv,
//                senderPublicKey = Base64.getDecoder.decode(identifier),
//                wrappedKey = wrappedKey
//            ).toByteArray
//
//
//    override def decrypt(decryptionData: Bytes, keyIdentifier: KeyIdentifier): Result[Bytes] =
//        for {
//            encryptResponse <- Result(EncryptedMessage.parseFrom(decryptionData))
//            keyReceiver <- keysStore.get(keyIdentifier).toRight(s"[Decrypt] Unknown key: $keyIdentifier")
//            receiverDerivedKey <- Result(deriveKey(keyReceiver, encryptResponse.senderPublicKey))
//            unWrappedKey <- Result(unWrapKey(encryptResponse.wrappedKey, receiverDerivedKey, encryptResponse.iv))
//            result <- Result(decryptAESGCM(encryptResponse.cipherText, unWrappedKey, encryptResponse.iv))
//        } yield result
//
//}
