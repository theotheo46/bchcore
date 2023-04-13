package ru.sberbank.blockchain.common.cryptography.hd

import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.math.ec.ECPoint
import ru.sberbank.blockchain.cnft.commons.{Bytes, Result, asBytes}
import ru.sberbank.blockchain.common.cryptography.bouncycastle.EllipticOps
import ru.sberbank.blockchain.common.cryptography.{Bip32, HDHasher}

import java.security.{MessageDigest, Security}

class Bip32ELOps(
    curve: ECNamedCurveParameterSpec
) extends Bip32[Result, ECPoint](BCHasher, new EllipticOps(curve)) {
    // NOTE: keep the line below (registration of BC provider) at top of this class:
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(new BouncyCastleProvider)

}

object BCHasher extends HDHasher[Result] {
    override def hmac512(key: Bytes, data: Bytes): Result[Bytes] = Result {
        val mac = new HMac(new SHA512Digest())
        mac.init(new KeyParameter(key.toArray))
        mac.update(data.toArray, 0, data.length.toInt)
        val out = new Array[Byte](64)
        mac.doFinal(out, 0)
        asBytes(out)
    }

    override def sha256(data: Bytes): Result[Bytes] = Result {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(data)
        md.digest()
    }
}
