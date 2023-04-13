package ru.sberbank.blockchain.common.cryptography.webcrypto

import org.scalajs.dom.crypto.{AlgorithmIdentifier, CryptoKey, EcKeyImportParams, EcdsaParams, HashAlgorithm, JsonWebKey, KeyAlgorithmIdentifier, KeyFormat, KeyUsage, SubtleCrypto}
import ru.sberbank.blockchain.cnft.commons.{Bytes, Result, asByteArray, asBytes}
import ru.sberbank.blockchain.common.cryptography.P1363Convert.{signatureFromDer, signatureToDer}
import ru.sberbank.blockchain.common.cryptography.lib_elliptic.{JSECKeyPair, JSECPoint}

import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.JSON
import scala.scalajs.js.Thenable.Implicits._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Vladimir Sidorov
 */
@JSExportAll
@JSExportTopLevel("WebCryptoSignVerify")
class WebCryptoSignVerify(
    crypto: SubtleCrypto,
) {
    private val ecKeyImportParams: KeyAlgorithmIdentifier = EcKeyImportParams(name = "ECDSA", namedCurve = "P-256")


    private val ecdsaParams: AlgorithmIdentifier = EcdsaParams(name = "ECDSA", hash = HashAlgorithm.`SHA-256`)


    def verifySignature(key: JSECPoint, data: Bytes, signature: Bytes): Result[Boolean] = {
        for {
            parsedKey <- makeJWKFromPublic(key)
            publicKey <-
                crypto
                    .importKey(
                        KeyFormat.jwk,
                        parsedKey,
                        ecKeyImportParams,
                        extractable = true,
                        js.Array(KeyUsage.verify)
                    )
                    .toFuture
                    .map(_.asInstanceOf[CryptoKey])
            sigFromDer = signatureFromDer(asByteArray(signature))
            verifyStatus <-
                crypto
                    .verify(ecdsaParams, publicKey, asBytes(sigFromDer), data)
                    .toFuture

        } yield verifyStatus.asInstanceOf[Boolean]
    }.toJSPromise


    def createSignature(key: JSECKeyPair, content: Bytes): Result[Bytes] = {
        for {
            parsedKey <- makeJWKFromPrivate(key)
            importPrivKey <- crypto
                .importKey(
                    KeyFormat.jwk,
                    parsedKey,
                    ecKeyImportParams,
                    extractable = true,
                    js.Array(KeyUsage.sign))
                .toFuture
                .map(_.asInstanceOf[CryptoKey])
            derSignature <- crypto.sign(ecdsaParams, importPrivKey, content)
                .map(_.asInstanceOf[Bytes])
                .map(b => signatureToDer(asByteArray(b)))
                .map(asBytes)
        } yield derSignature

    }.toJSPromise

    private def Base64UrlEnc(input: Bytes): Result[String] = Result(Base64.getUrlEncoder.withoutPadding.encodeToString(asByteArray(input)))

    private def getXYfromPublic(key: JSECPoint): Result[(String, String)] = {
        for {
            x <- Base64UrlEnc(key.getX().toArray("be"))
            y <- Base64UrlEnc(key.getY().toArray("be"))
        } yield (x, y)
    }.toJSPromise

    private def makeJWKFromPublic(key: JSECPoint): Result[JsonWebKey] =
        getXYfromPublic(key).map { t =>
            val jwkString = s"""{"kty":"EC","key_ops":["verify"],"ext":true,"crv":"P-256","x":"${t._1}","y":"${t._2}"}"""
            JSON.parse(jwkString).asInstanceOf[JsonWebKey]
        }.toJSPromise

    private def makeJWKFromPrivate(key: JSECKeyPair): Result[JsonWebKey] = {
        for {
            (x, y) <- getXYfromPublic(key.getPublic())
            d <- Base64UrlEnc(key.getPrivate().toArray("be"))
            jwkString = s"""{"kty":"EC","key_ops":["sign"],"ext":true,"crv":"P-256","x":"$x","y":"$y","d":"$d"}"""
            result = JSON.parse(jwkString).asInstanceOf[JsonWebKey]
        } yield result
    }.toJSPromise
}
