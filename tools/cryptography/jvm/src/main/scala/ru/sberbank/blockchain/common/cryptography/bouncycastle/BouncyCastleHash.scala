package ru.sberbank.blockchain.common.cryptography.bouncycastle

import ru.sberbank.blockchain.cnft.commons.{Bytes, Result}
import ru.sberbank.blockchain.common.cryptography.Hash
import org.bouncycastle.crypto.digests._

object BouncyCastleHash extends Hash[Result] {
    override def sha256(content: Bytes): Result[Bytes] = {
        val digest = new SHA256Digest
        digest.update(content, 0, content.length)
        val out = new Array[Byte](digest.getDigestSize)
        digest.doFinal(out, 0)
        Result(out)
    }

    override def sha1(content: Bytes): Result[Bytes] = {
        val digest = new SHA1Digest
        digest.update(content, 0, content.length)
        val out = new Array[Byte](digest.getDigestSize)
        digest.doFinal(out, 0)
        Result(out)
    }
}
