//package ru.sberbank.blockchain.common.cryptography.webcrypto
//
//import org.scalajs.dom.crypto.{AlgorithmIdentifier, BufferSource, CryptoKey, CryptoKeyPair, EcKeyAlgorithm, EcKeyImportParams, EcdsaParams, HashAlgorithm, KeyAlgorithmIdentifier, KeyFormat, KeyUsage, SubtleCrypto}
//import ru.sberbank.blockchain.cnft.commons.{Base64R, Bytes, Collection, Result, asByteArray, asBytes}
//import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
//import ru.sberbank.blockchain.common.cryptography.P1363Convert.{signatureFromDer, signatureToDer}
//import ru.sberbank.blockchain.common.cryptography.{KeysStore, SignatureOperations, WebCryptoKey}
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.scalajs.js
//import scala.scalajs.js.JSConverters._
//import scala.scalajs.js.Thenable.Implicits._
//import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
//import scala.scalajs.js.typedarray.ArrayBuffer
//
///**
// * @author Vladimir Sidorov
// */
//@JSExportAll
//@JSExportTopLevel("WebCryptoSignatureOperations")
//class WebCryptoSignatureOperations(
//    crypto: SubtleCrypto,
//    keyStore: KeysStore[WebCryptoKey]
//) extends SignatureOperations[Result] {
//
//    private val ecKeyImportParams: KeyAlgorithmIdentifier = EcKeyImportParams(name = "ECDSA", namedCurve = "P-256")
//
//    private val ecKeyGenParams: KeyAlgorithmIdentifier = EcKeyAlgorithm(name = "ECDSA", namedCurve = "P-256")
//
//    private val ecdsaParams: AlgorithmIdentifier = EcdsaParams(name = "ECDSA", hash = HashAlgorithm.`SHA-256`)
//
//    private val keyUsagesSign: js.Array[KeyUsage] = js.Array(KeyUsage.sign, KeyUsage.verify)
//
//    override def requestNewKey(): Result[KeyIdentifier] =
//        generateKey(ecKeyGenParams, extractable = true, keyUsagesSign)
//            .map(exportKeyPairToWebCryptoKeyPub).toJSPromise
//
//
//    def exportKeyPairToWebCryptoKeyPub(key: KeyIdentifier): Result[KeyIdentifier] = {
//        for {
//            keyFromStore <- keyStore.get(key)
//        } yield keyFromStore.get.identifier
//    }.toJSPromise
//
//    def generateKey(
//        algorithm: KeyAlgorithmIdentifier,
//        extractable: Boolean,
//        keyUsages: js.Array[KeyUsage]): Result[String] = {
//        for {
//            keyPair <-
//                crypto
//                    .generateKey(
//                        algorithm,
//                        extractable,
//                        keyUsages
//                    )
//                    .toFuture
//                    .map(_.asInstanceOf[CryptoKeyPair])
//
//            pubKey <-
//                crypto
//                    .exportKey(KeyFormat.spki, keyPair.publicKey)
//                    .toFuture
//                    .map(_.asInstanceOf[ArrayBuffer])
//            privKey <-
//                crypto
//                    .exportKey(KeyFormat.pkcs8, keyPair.privateKey)
//                    .toFuture
//                    .map(_.asInstanceOf[ArrayBuffer])
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
//    override def verifySignature(key: Bytes, content: Bytes, signature: Bytes): Result[Boolean] = {
//        for {
//            publicKey <-
//                crypto
//                    .importKey(
//                        KeyFormat.spki,
//                        key,
//                        ecKeyImportParams,
//                        extractable = true,
//                        js.Array(KeyUsage.verify)
//                    )
//                    .toFuture
//                    .map(_.asInstanceOf[CryptoKey])
//            sigFromDer = signatureFromDer(asByteArray(signature))
//            verifyStatus <-
//                crypto
//                    .verify(ecdsaParams, publicKey, asBytes(sigFromDer), content)
//                    .toFuture
//
//        } yield verifyStatus.asInstanceOf[Boolean]
//    }.toJSPromise
//
//    override def publicKey(keyIdentifier: KeyIdentifier): Result[Option[Bytes]] =
//        keyStore.get(keyIdentifier).map(_.map(_.publicAsBytes)).toJSPromise
//
//    override def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] =
//        keyStore.get(keyIdentifier).map(_.isDefined).toJSPromise
//
//
//    override def createSignature(key: KeyIdentifier, content: Bytes): Result[Bytes] = {
//        for {
//            webCryptoKey <- keyStore.get(key)
//            privateKey = webCryptoKey.get.privateKeyAsBytes
//            importPrivKey <- importKey(
//                KeyFormat.pkcs8,
//                privateKey,
//                ecKeyImportParams,
//                extractable = true,
//                js.Array(KeyUsage.sign))
//            derSignature <- crypto.sign(ecdsaParams, importPrivKey, content)
//                .map(_.asInstanceOf[Bytes])
//                .map(b => signatureToDer(asByteArray(b)))
//                .map(asBytes)
//        } yield derSignature
//
//    }.toJSPromise
//
//    def importKey(
//        keyFormat: KeyFormat,
//        key: BufferSource,
//        algorithm: KeyAlgorithmIdentifier,
//        extractable: Boolean,
//        keyUsages: js.Array[KeyUsage]): Result[CryptoKey] = {
//        crypto
//            .importKey(keyFormat, key, algorithm, extractable, keyUsages)
//            .toFuture.map(_.asInstanceOf[CryptoKey])
//    }.toJSPromise
//
//    override def findKeysByPublic(publicKeyBytes: Collection[Bytes]): Result[Collection[String]] =
//        Result.Fail("Method not supported")
//
//}
