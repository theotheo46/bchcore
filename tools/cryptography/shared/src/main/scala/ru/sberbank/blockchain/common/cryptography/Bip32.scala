package ru.sberbank.blockchain.common.cryptography

import ru.sberbank.blockchain.cnft.commons.{Bytes, ROps, asByteArray, asBytes, concatBytes}
import ru.sberbank.blockchain.common.cryptography.elliptic.ECOps

import java.math.BigInteger
import scala.language.{higherKinds, implicitConversions}

/**
 * @author Alexey Polubelov
 */
class Bip32[R[+_], ECPoint](
    hasher: HDHasher[R],
    ecops: ECOps[R, ECPoint],
)(implicit
    R: ROps[R]
) {

    import ROps._

    private val G: ECPoint = ecops.getG
    //private val q: BigInteger = ecops.getQ

    implicit class ECPOps(p: ECPoint) {
        def +(o: ECPoint): ECPoint = ecops.addECPoints(p, o)

        def *(k: BigInteger): ECPoint = ecops.multECpoints(p, k)

        def =?=(o: ECPoint): Boolean = ecops.compare(p, o)

    }

    implicit def ECPoint2Bytes(p: ECPoint): Bytes = ecops.serialize(p)

    implicit class SKOps(k: BigInteger) {
        def %(o: BigInteger): BigInteger = k.mod(o)

        def -(o: BigInteger): BigInteger = k.subtract(o)

        def +(o: BigInteger): BigInteger = k.add(o)

        def *(p: BigInteger): BigInteger = k.multiply(p)

        def toBytes: Bytes = asBytes(k.toByteArray.dropWhile(_ == 0))

        def isPositive: Boolean = k.signum() == 1
    }

    implicit class ArrOps(a: Bytes) {

        def asECPoint: ECPoint = ecops.deserialize(a)

        def toBigInt: BigInteger = new BigInteger(1, asByteArray(a))

    }

    def derivePrivateKey(key: BigInteger, cc: Bytes, index: Int): R[(BigInteger, Bytes)] =
        for {
            publicKey <- publicForPrivate(key)
            indexBytes <-
                R(Array(
                    ((index >> 24) & 0xffffffff).toByte,
                    ((index >> 16) & 0xffffffff).toByte,
                    ((index >> 8) &  0xffffffff).toByte,
                    (index & 0xffffffff).toByte
                ))
            hashed <- hasher.hmac512(cc, concatBytes(Seq(publicKey, asBytes(indexBytes)))).map(x => asByteArray(x))
            pKey <- R((new BigInteger(1, hashed.take(32)) + key) % ecops.getQ)
            cc <- R(asBytes(hashed.takeRight(32)))
        } yield (pKey, cc)

    def publicForPrivate(key: BigInteger): R[Bytes] = R(G * key)

    def sign(key: BigInteger, data: Bytes): R[Bytes] =
        hasher.sha256(data).flatMap(hash => ecops.sign(key, hash))

    def verify(key: Bytes, data: Bytes, signature: Bytes): R[Boolean] =
        hasher.sha256(data).flatMap { hash =>
            ecops.verify(key.asECPoint, hash, signature)
        }

    def splitKey(key: BigInteger): R[(BigInteger, BigInteger)] = {
        key match {
            case value if value.toByteArray.length == 64 =>
                R((new BigInteger(1, value.toByteArray.take(32)), new BigInteger(1, value.toByteArray.takeRight(32))))
            case _ => R.Fail("Check key error: length not enough.")
        }
    }
}

trait HDHasher[R[+_]] {
    def hmac512(key: Bytes, data: Bytes): R[Bytes]

    def sha256(data: Bytes): R[Bytes]
}
