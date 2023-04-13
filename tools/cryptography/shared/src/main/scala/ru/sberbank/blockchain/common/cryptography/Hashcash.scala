package ru.sberbank.blockchain.common.cryptography

import ru.sberbank.blockchain.cnft.commons.ROps.{IterableR_Ops, summonHasOps}
import ru.sberbank.blockchain.cnft.commons.{Bytes, BytesOps, ROps, asByteArray, asBytes, concatBytes, isEqualBytes}

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import scala.language.higherKinds
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("Hashcash")
class Hashcash[R[+_]](
    hashOps: Hash[R]
)(implicit
    val R: ROps[R]
) {
    def computeHash(content: Bytes): R[Bytes] = hashOps.sha1(content)

    def verifyDifficulty(hash: Bytes, zeroesCount: Int): Boolean = {
        val hashHex = new BigInteger(asByteArray(hash))
        hashHex.getLowestSetBit == zeroesCount
    }

    def isCorrect(hash: Bytes, content: String, extra: Bytes, nonce: Int): R[Boolean] =
        hashOps
            .sha256(asBytes(content.getBytes(StandardCharsets.UTF_8)))
            .flatMap { contentHash =>
                val contentBytes = contentToBytes(contentHash, extra)
                contentBytes.setInt(0, nonce)
                computeHash(contentBytes)
                    .map(myHash => isEqualBytes(myHash, hash)
                    )
            }

    def pickUpNonce(content: String, difficulty: Int): R[HashcashResult] =
        hashOps
            .sha256(asBytes(content.getBytes(StandardCharsets.UTF_8)))
            .flatMap { contentHash =>
                var contentBytes = asBytes(Array.empty[Byte])
                var extra = asBytes(Array.empty[Byte])
                Stream.from(Int.MinValue).findR { nonce =>
                    if (nonce == Int.MinValue) {
                        extra = asBytes(System.currentTimeMillis().toString.getBytes)
                        contentBytes = contentToBytes(contentHash, extra)
                    }
                    contentBytes.setInt(0, nonce)
                    computeHash(contentBytes)
                        .map(bytes => verifyDifficulty(bytes, difficulty))
                }.flatMap {
                    case Some(nonce) =>
                        contentBytes.setInt(0, nonce)
                        computeHash(contentBytes).map(hash => HashcashResult(hash, nonce, extra))
                    case None => R.Fail("Error while finding nonce")
                }
            }

    private def contentToBytes(content: Bytes, extra: Bytes): Bytes = {
        val additionalBytes =
            if ((content.length + extra.length) % 4 != 0)
                asBytes(new Array[Byte](4 - (content.length + extra.length) % 4))
            else
                asBytes(Array.empty[Byte])
        concatBytes(Seq(asBytes(new Array[Byte](4)), additionalBytes, extra, content))
    }
}

case class HashcashResult(hash: Bytes, nonce: Int, extra: Bytes)