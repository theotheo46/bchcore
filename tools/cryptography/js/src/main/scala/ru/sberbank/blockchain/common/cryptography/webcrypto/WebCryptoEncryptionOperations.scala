package ru.sberbank.blockchain.common.cryptography.webcrypto

import org.scalajs.dom.crypto.{AesGcmParams, AesKeyAlgorithm, AesKeyGenParams, AlgorithmIdentifier, BufferSource, CryptoKey, CryptoKeyPair, EcKeyAlgorithm, EcKeyImportParams, EcdhKeyDeriveParams, KeyAlgorithmIdentifier, KeyFormat, KeyUsage, SubtleCrypto}
import ru.sberbank.blockchain.cnft.commons.ROps.IterableR_Ops
import ru.sberbank.blockchain.cnft.commons.ROps.summonHasOps
import ru.sberbank.blockchain.cnft.commons.ResultOps.fromOption
import ru.sberbank.blockchain.cnft.commons.{Base64R, Bytes, Collection, Result, ResultOps, asByteArray, asBytes, collectionFromIterable, collectionFromSequence, isEqualBytes}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.{EncryptionOperations, SecureRandomGenerator}
import ru.sberbank.blockchain.common.cryptography.model.{EncryptedMessage, KeyPair, KeysList, WrappedForPublic}
import ru.sberbank.blockchain.common.cryptography.store.{KeysStore, WebCryptoKey}

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

/**
 * @author Vladimir Sidorov
 */
@JSExportAll
@JSExportTopLevel("WebCryptoEncryptionOperations")
class WebCryptoEncryptionOperations(
    crypto: SubtleCrypto,
    secureRandomGenerator: SecureRandomGenerator,
    keyStore: KeysStore[WebCryptoKey]
) extends EncryptionOperations[Result] {

    private val aesKeyAlgorithm: AlgorithmIdentifier = AesKeyAlgorithm(name = "AES-GCM", length = 256)

    private val aesKeyGenParams: KeyAlgorithmIdentifier = AesKeyGenParams(name = "AES-GCM", length = 256)

    val ecdhKeyGenParams: KeyAlgorithmIdentifier = EcKeyAlgorithm(name = "ECDH", namedCurve = "P-256")

    private val ecdhKeyImportParams: KeyAlgorithmIdentifier = EcKeyImportParams(name = "ECDH", namedCurve = "P-256")

    val keyUsagesDerive: js.Array[KeyUsage] = js.Array(KeyUsage.deriveKey, KeyUsage.deriveBits)

    private val keyUsagesCrypt: js.Array[KeyUsage] = js.Array(KeyUsage.encrypt, KeyUsage.decrypt)

    private val keyUsagesWrap: js.Array[KeyUsage] = js.Array(KeyUsage.wrapKey, KeyUsage.unwrapKey)

    override def requestNewKey(): Result[KeyIdentifier] =
        generateKey(ecdhKeyGenParams, extractable = true, keyUsagesDerive)
            .flatMap(exportKeyPairToWebCryptoKeyPub)

    def exportKeyPairToWebCryptoKeyPub(key: KeyIdentifier): Result[KeyIdentifier] = {
        for {
            keyFromStore <- keyStore.get(key)
        } yield keyFromStore.get.identifier
    }

    def generateKey(
        algorithm: KeyAlgorithmIdentifier,
        extractable: Boolean,
        keyUsages: js.Array[KeyUsage]): Result[String] =
        for {
            keyPair <-
                crypto.generateKey(
                    algorithm,
                    extractable,
                    keyUsages
                )
                    .map(_.asInstanceOf[CryptoKeyPair])

            pubKey <-
                crypto
                    .exportKey(KeyFormat.spki, keyPair.publicKey)
                    .map(_.asInstanceOf[ArrayBuffer])
            privKey <-
                crypto
                    .exportKey(KeyFormat.pkcs8, keyPair.privateKey)
                    .map(_.asInstanceOf[ArrayBuffer])
            pubKeyB64 <- Base64R.encode(asByteArray(pubKey))
            webCryptoKey = WebCryptoKey(
                identifier = pubKeyB64,
                publicAsBytes = pubKey,
                privateKeyAsBytes = privKey
            )
        } yield {
            keyStore.save(pubKeyB64, webCryptoKey)
            pubKeyB64
        }


    def generateAesKey(
        algorithm: KeyAlgorithmIdentifier,
        extractable: Boolean,
        keyUsages: js.Array[KeyUsage]): Result[CryptoKey] = {
        for {
            key <-
                crypto
                    .generateKey(
                        algorithm,
                        extractable,
                        keyUsages
                    )
                    .map(_.asInstanceOf[CryptoKey])
        } yield key
    }

    override def publicKey(keyIdentifier: KeyIdentifier): Result[Bytes] =
        keyStore.get(keyIdentifier).flatMap {
            case Some(value) => Result(value.publicAsBytes)
            case None => Result.Fail(s"Unknown key: $keyIdentifier")
        }

    override def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] =
        keyStore.get(keyIdentifier).map(_.isDefined)

    //    override def importEncryptionKey(publicKey: Bytes, privateKey: Bytes): Result[String] = {
    //        for {
    //            importedPublicKey <- importKey(
    //                KeyFormat.spki,
    //                publicKey,
    //                ecdhKeyImportParams,
    //                extractable = true,
    //                keyUsagesDerive)
    //            importedPrivateKey <- importKey(
    //                KeyFormat.pkcs8,
    //                privateKey,
    //                ecdhKeyImportParams,
    //                extractable = true,
    //                keyUsagesDerive)
    //            pubKey <-
    //              crypto
    //                .exportKey(KeyFormat.spki, importedPublicKey)
    //                .toFuture
    //                .map(_.asInstanceOf[ArrayBuffer])
    //            privKey <-
    //              crypto
    //                .exportKey(KeyFormat.pkcs8, importedPrivateKey)
    //                .toFuture
    //                .map(_.asInstanceOf[ArrayBuffer])
    //            pubKeyB64 <- Base64R.encode(asByteArray(pubKey))
    //            webCryptoKey = WebCryptoKey(
    //                identifier = pubKeyB64,
    //                publicAsBytes = pubKey,
    //                privateKeyAsBytes = privKey
    //            )
    //        } yield {
    //            keyStore.save(pubKeyB64, webCryptoKey)
    //            pubKeyB64
    //        }
    //    }.toJSPromise
    //
    //    override def exportEncryptionKey(keyIdentifier: KeyIdentifier): Result[Bytes] = {
    //        for {
    //            mayBeKey <- keyStore.get(keyIdentifier).map(_.map(_.privateKeyAsBytes))
    //            privateKey <- ResultOps.fromOption(mayBeKey, s"[exportEncryptionKey] $keyIdentifier not found")
    //        } yield privateKey
    //    }.toJSPromise


    def importKey(
        keyFormat: KeyFormat,
        key: BufferSource,
        algorithm: KeyAlgorithmIdentifier,
        extractable: Boolean,
        keyUsages: js.Array[KeyUsage]): Result[CryptoKey] = {
        crypto
            .importKey(keyFormat, key, algorithm, extractable, keyUsages)
            .map(_.asInstanceOf[CryptoKey])
    }

    def exportKey(
        format: KeyFormat,
        key: CryptoKey): Result[ArrayBuffer] = {
        crypto.exportKey(format, key).map(_.asInstanceOf[ArrayBuffer])
    }


    def deriveKey(
        publicKey: CryptoKey,
        baseKey: CryptoKey,
        derivedKeyType: KeyAlgorithmIdentifier,
        extractable: Boolean,
        keyUsages: js.Array[KeyUsage]): Result[CryptoKey] = {
        val algorithm: AlgorithmIdentifier = EcdhKeyDeriveParams(name = "ECDH", public = publicKey)
        crypto
            .deriveKey(algorithm, baseKey, derivedKeyType, extractable, keyUsages)
            .map(_.asInstanceOf[CryptoKey])
    }

    def wrapKey(
        format: KeyFormat,
        key: CryptoKey,
        wrappingKey: CryptoKey,
        wrapAlgorithm: AlgorithmIdentifier): Result[ArrayBuffer] = {
        crypto
            .wrapKey(format, key, wrappingKey, wrapAlgorithm)
            .map(wrappedKey =>
                wrappedKey.asInstanceOf[ArrayBuffer])
    }

    def unwrapKey(
        format: String,
        wrappedKey: BufferSource,
        unwrappingKey: CryptoKey,
        unwrapAlgorithm: AlgorithmIdentifier,
        unwrappedKeyAlgorithm: AlgorithmIdentifier,
        extractable: Boolean,
        keyUsages: js.Array[KeyUsage]
    ): Result[CryptoKey] = {
        crypto
            .unwrapKey(format, wrappedKey, unwrappingKey, unwrapAlgorithm, unwrappedKeyAlgorithm, extractable, keyUsages)
            .map(_.asInstanceOf[CryptoKey])
    }

    def encryptAES(
        algorithm: AlgorithmIdentifier,
        key: CryptoKey,
        data: BufferSource): Result[ArrayBuffer] =
        crypto
            .encrypt(algorithm, key, data)
            .map(encodedData => encodedData.asInstanceOf[ArrayBuffer])


    def decryptAES(
        algorithm: AlgorithmIdentifier,
        key: CryptoKey,
        data: BufferSource): Result[Bytes] = {
        crypto
            .decrypt(algorithm, key, data)
            .map(_.asInstanceOf[Bytes])
        //            .toFuture
        //            .map { decodedData =>
        //                new String(decodedData.asInstanceOf[ArrayBuffer], StandardCharsets.UTF_8)
        //            }
    }

    override def encrypt(message: Bytes, receiverPublicKeys: Collection[Bytes]): Result[Bytes] = {
        val IV = secureRandomGenerator.nextBytes(16)

        val aesGcmParams = AesGcmParams(name = "AES-GCM", IV, new Uint8Array(0).buffer, 128)

        for {
            aesKey <- generateAesKey(aesKeyGenParams, extractable = true, keyUsagesCrypt)
            encryptedContent <- encryptAES(aesGcmParams, aesKey, message)

            tempKey <- generateKey(ecdhKeyGenParams, extractable = true, keyUsagesDerive)
            keyFromStore <- keyStore.get(tempKey)
            importedPrivateKey <- importKey(
                KeyFormat.pkcs8,
                keyFromStore.get.privateKeyAsBytes,
                ecdhKeyImportParams,
                extractable = true,
                keyUsagesDerive)

            wrappedKeys <- receiverPublicKeys.toSeq.mapR { pk =>
                for {
                    ck <- importKey(
                        KeyFormat.spki,
                        pk,
                        ecdhKeyImportParams,
                        extractable = true,
                        js.Array()
                    )
                    dk <- deriveKey(
                        ck,
                        importedPrivateKey,
                        aesKeyGenParams,
                        extractable = true,
                        keyUsagesWrap)

                    wk <- wrapKey(
                        KeyFormat.raw,
                        aesKey,
                        dk,
                        aesGcmParams)
                } yield
                    WrappedForPublic(wk, pk)
            }
            senderPubKey = keyFromStore.get.publicAsBytes


        } yield
            asBytes(
                EncryptedMessage(
                    cipherText = encryptedContent,
                    senderPublicKey = senderPubKey,
                    iv = IV,
                    wrappedKeys = collectionFromIterable(wrappedKeys)
                ).toByteArray
            )
    }

    override def decrypt(decryptionData: Bytes, keyIdentifier: KeyIdentifier): Result[Bytes] = {

        val encryptResponse = EncryptedMessage.parseFrom(asByteArray(decryptionData))

        val unWrapAlgorithm = AesGcmParams(name = "AES-GCM", encryptResponse.iv, new Uint8Array(0).buffer, 128)

        for {
            privateKeyReceiver <- keyStore.get(keyIdentifier)
            importedPrivateKey <- importKey(
                KeyFormat.pkcs8,
                privateKeyReceiver.get.privateKeyAsBytes,
                ecdhKeyImportParams,
                extractable = true,
                keyUsagesDerive)
            publicKeySender <- importKey(
                KeyFormat.spki,
                encryptResponse.senderPublicKey,
                ecdhKeyImportParams,
                extractable = true,
                js.Array())
            deriveKey <- deriveKey(
                publicKeySender,
                importedPrivateKey,
                aesKeyGenParams,
                extractable = true,
                keyUsagesWrap)

            myPublicKey <- Base64R.decode(keyIdentifier)
            wrappedKey <- fromOption(
                encryptResponse.wrappedKeys
                    .find { k => isEqualBytes(k.publicKey, asBytes(myPublicKey)) }
                    .map(_.wrappedKey),
                "Public key not in list"
            )

            unWrappedKey <- unwrapKey(
                KeyFormat.raw.toString,
                wrappedKey,
                deriveKey,
                unWrapAlgorithm,
                aesKeyAlgorithm,
                extractable = true,
                keyUsagesCrypt
            )

            decryptedContent <- decryptAES(
                unWrapAlgorithm,
                unWrappedKey,
                encryptResponse.cipherText)

        } yield decryptedContent
    }

    override def exportData(): Result[Bytes] = Result {
        asBytes(
            KeysList(
                collectionFromSequence(
                    keyStore.list.map { case (id, key) =>
                        KeyPair(
                            pk = asBytes(Base64.getDecoder.decode(id.getBytes(StandardCharsets.UTF_8))),
                            sk = key.privateKeyAsBytes
                        )
                    }
                )
            ).toByteArray
        )
    }
}
