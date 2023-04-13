package ru.sberbank.blockchain.common.cryptography.sag


import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, ROps, asByteArray, asBytes, collectionFromArray}
import ru.sberbank.blockchain.common.cryptography.elliptic.ECOps
import ru.sberbank.blockchain.common.cryptography.model.RingSignature

import java.math.BigInteger
import scala.language.{higherKinds, implicitConversions}
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class RingSigner[R[+_], ECPoint](
    hasher: Hasher[R],
    ecops: ECOps[R, ECPoint],
    key: (BigInteger, ECPoint)
)(implicit
    R: ROps[R]
) {

    import ROps._

    private val G: ECPoint = ecops.getG
    private val q: BigInteger = ecops.getQ

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

        def toBytes: Bytes = asBytes(k.toByteArray)

        def isPositive: Boolean = k.signum() == 1
    }

    implicit class ArrOps(a: Bytes) {

        def asECPoint: ECPoint = ecops.deserialize(a)

        def toBigInt: BigInteger = new BigInteger(1, asByteArray(a))

    }

    private val MyPrivate = key._1
    private val MyPublic = key._2


    private def HASH(bytes: Bytes*): R[Bytes] = hasher.hash(bytes)

    def sign(message: Bytes, keys: Collection[Bytes]): R[Bytes] = {

        val ringPoints = keys.map(_.asECPoint)

        val RingSize = ringPoints.length
        val e = new Array[BigInteger](RingSize)
        val s = new Array[BigInteger](RingSize)

        for {
            k <- ecops.generatePair().map(_._1)
            _ <- R.expect(ringPoints.length >= 2, "public Key Ring too small, should be at least 2 key.")

            j <- R.fromOption(
                ringPoints.zipWithIndex
                    .find { case (k, _) => k =?= MyPublic }
                    .map(_._2),
                "Signer Public key not in ring"
            )

            ej <- HASH(message, G * k)

            _ <- R {
                e((j + 1) % RingSize) = ej.toBigInt
            }

            _ <-
                Stream.iterate[Int]((j + 1) % RingSize, RingSize - 1)(i => (i + 1) % RingSize)
                    .mapR { i =>
                        for {
                            si <- ecops.generatePair().map(_._1)
                            nextE <- HASH(message, G * si + ringPoints(i) * e(i))
                        } yield {
                            s(i) = si
                            e((i + 1) % RingSize) = nextE.toBigInt
                        }
                    }
            _ <- R {
                s(j) = (k - MyPrivate * e(j) + e(j) * q) % q
            }
            _ <- R.expect(s(j).isPositive, "signature not anonymous")
        } yield
            asBytes(
                RingSignature(
                    e = e(0).toBytes,
                    si =
                        collectionFromArray(
                            s.map(_.toBytes)
                        )
                ).toByteArray
            )
    }

    // verify verifying signature against list of public keys and return true if message signed by one of owners of
    // public keys in the list
    def verify(message: Bytes, publicKeysRingEncoded: Collection[Bytes], sign: Bytes): R[Boolean] =
        for {
            signature <- R(RingSignature.parseFrom(asByteArray(sign)))
            publicKeysRing = publicKeysRingEncoded.map(_.asECPoint)

            e = signature.e.toBigInt
            s = signature.si.map(_.toBigInt)

            ee <- {
                var ee = e
                s.zipWithIndex.toSeq.mapR { case (si, i) =>
                    HASH(message, G * si + publicKeysRing(i) * ee)
                        .map(_.toBigInt)
                        .map { v =>
                            ee = v
                            ee
                        }
                }
                    .map(_.last)
            }
        } yield
            s.length > 0 && ee == e


    // calculate and return encoded public key for given private key
    def getPublicKeyEncoded(privateKey: BigInteger): Bytes = G * privateKey

    def isPublicKeyValid(pub: Bytes): R[Unit] =
        R {
            pub.asECPoint;
            ()
        }

}

trait Hasher[R[+_]] {
    def hash(b: Seq[Bytes]): R[Bytes]
}



