package ru.sberbank.blockchain.common.cryptography.webcrypto

import org.scalajs.dom.crypto.{AesDerivedKeyParams, AesGcmParams, CryptoKey, HashAlgorithm, KeyFormat, KeyUsage, Pbkdf2Params, SubtleCrypto}
import ru.sberbank.blockchain.cnft.commons.ROps.summonHasOps
import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, Result, asByteArray, asBytes}
import ru.sberbank.blockchain.common.cryptography.model.PBEncrypted

import java.nio.charset.StandardCharsets
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import scala.scalajs.js.typedarray.Uint8Array

/**
 * @author Alexey Polubelov
 */
@JSExportAll
@JSExportTopLevel("WebCryptoPBEncryption")
class WebCryptoPBEncryption(
    crypto: SubtleCrypto
) {

    //TODO: crypto.getRandomValues(new Uint8Array(12));
    private def randomBytes(length: Int): Bytes = {
        asBytes(
            Array.fill(16)((scala.util.Random.nextInt(256) - 128).toByte)
        )
    }

    private def key(password: String, salt: Bytes): Result[CryptoKey] = {
        for {
            keyRef <- crypto.importKey(
                KeyFormat.raw,
                asBytes(password.getBytes(StandardCharsets.UTF_8)), //enc.encode(password),
                "PBKDF2",
                extractable = false,
                Collection(KeyUsage.deriveKey)
            ).map(_.asInstanceOf[CryptoKey])
            result <- crypto.deriveKey(
                Pbkdf2Params(
                    name = "PBKDF2",
                    salt = salt,
                    iterations = 250000,
                    hash = HashAlgorithm.`SHA-256`,
                ),
                keyRef,
                AesDerivedKeyParams("AES-GCM", 256),
                extractable = false,
                Collection(KeyUsage.encrypt, KeyUsage.decrypt)
            )
        } yield result.asInstanceOf[CryptoKey]
    }

    def encrypt(password: String, data: Bytes): Result[Bytes] = {
        val iv = randomBytes(16)
        val salt = randomBytes(16)
        for {
            key <- key(password, salt)
            result <- crypto.encrypt(
                AesGcmParams(
                    "AES-GCM", iv, new Uint8Array(0).buffer, 128
                ),
                key,
                data
            ).map(_.asInstanceOf[Bytes])
        } yield {
            asBytes(
                PBEncrypted(salt, iv, result).toByteArray
            )
        }
    }

    def decrypt(password: String, data: Bytes): Result[Bytes] = {
        for {
            msg <- Result(PBEncrypted.parseFrom(asByteArray(data)))
            key <- key(password, msg.salt)
            result <- crypto.decrypt(
                AesGcmParams(
                    "AES-GCM", msg.iv, new Uint8Array(0).buffer, 128
                ),
                key,
                msg.data
            ).map(_.asInstanceOf[Bytes])
        } yield result
    }

}
