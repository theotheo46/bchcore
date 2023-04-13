package ru.sberbank.blockchain.common.cryptography.lib_elliptic

import ru.sberbank.blockchain.cnft.commons.{Bytes, LoggingSupport, Result, asByteArray, asBytes}
import ru.sberbank.blockchain.common.cryptography.elliptic.ECOps

import java.math.BigInteger
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

/**
 * @author Alexey Polubelov
 */
class LibEllipticOps(
    jsec: JSEC,
    compressed: Boolean = true
) extends ECOps[Result, JSECPoint] with LoggingSupport {

    //val wc = new WebCryptoSignVerify(crypto)

    override def compare(p: JSECPoint, o: JSECPoint): Boolean = p.getX().cmp(o.getX()) == 0 && p.getY().cmp(o.getY()) == 0

    override def serialize(p: JSECPoint): Bytes = new Uint8Array(p.encode("array", compressed)).buffer

    override def deserialize(a: Bytes): JSECPoint = jsec.curve.decodePoint(new Uint8Array(a))

    override def multECpoints(p: JSECPoint, k: BigInteger): JSECPoint = p.mul(new Uint8Array(asBytes(k.toByteArray)))

    override def addECPoints(p: JSECPoint, o: JSECPoint): JSECPoint = p.add(o)

    override def getG: JSECPoint = jsec.g

    override def getQ: BigInteger = new BigInteger(1, asByteArray(jsec.n.toArray("be")))

    override def generatePair(): Result[(BigInteger, JSECPoint)] = Result {
        val kp = jsec.genKeyPair()
        (
            new BigInteger(1, asByteArray(kp.getPrivate().toArray("be"))),
            kp.getPublic()
        )
    }

    override def sign(key: BigInteger, data: Bytes): Result[Bytes] = Result {
        val hexKey = key.toString(16)
        val pk = jsec.keyFromPrivate(hexKey, "hex")
        val signature = pk.sign(new Uint8Array(data))

//        logger.trace(
//            s"\nSIGN: " +
//                s"DATA: ${hex(data)} " +
//                s"Key: ${pk.getPublic().encode("hex", compressed = true)} " +
//                s"R: ${hex(signature.r.toArray("be"))} " +
//                s"S: ${hex(signature.s.toArray("be"))} "
//        )

        val sc = asByteArray(signature.r.toArray("be")) ++ asByteArray(signature.s.toArray("be"))
        asBytes(sc)
    }

    override def verify(key: JSECPoint, data: Bytes, signature: Bytes): Result[Boolean] = Result {
        val pk = jsec.keyFromPublic(key)
        pk.verify(
            new Uint8Array(data),
            js.Dictionary(
                "r" -> hex(signature.slice(0, 32)),
                "s" -> hex(signature.slice(32, 64))
            )
        )
    }

    private def hex(value: Bytes): String =
        new BigInteger(1, asByteArray(value)).toString(16)

}


@js.native
trait JSEC extends js.Any {
    def curve: JSECCurve = js.native

    def g: JSECPoint = js.native

    def n: BN = js.native

    def genKeyPair(): JSECKeyPair = js.native

    def keyFromPublic(k: JSECPoint): JSECKeyPair = js.native

    def keyFromPrivate(k: String, e: String): JSECKeyPair = js.native

    def sign(h: Uint8Array, k: String, e: String, o: js.Object): JSECSignature

    def verify(h: Uint8Array, s: Uint8Array, k: String, e: String): Boolean

}

@js.native
trait JSECKeyPair extends js.Any {
    def getPublic(): JSECPoint

    def getPublic(s: Boolean, e: String): String

    def getPrivate(): BN

    def sign(data: Uint8Array): JSECSignature

    def verify(hash: Uint8Array, signature: js.Dictionary[String]): Boolean
}

@js.native
trait JSECPoint extends js.Any {
    def mul(a: Uint8Array): JSECPoint = js.native

    def add(a: JSECPoint): JSECPoint = js.native

    def getX(): BN = js.native

    def getY(): BN = js.native

    def encode(value: String, compressed: Boolean): Bytes = js.native

    def g: JSECPoint = js.native

    def n: BN = js.native
}

@js.native
trait JSECCurve extends js.Any {
    def decodePoint(a: Uint8Array): JSECPoint = js.native
}

@js.native
trait BN extends js.Any {
    def toArray(t: String): Bytes = js.native

    def cmp(a: BN): Int = js.native
}

@js.native
trait JSECSignature extends js.Any {
    def r: BN = js.native

    def s: BN = js.native

    def recoveryParam: js.Object = js.native

    def toDER(): js.Iterable[Short] = js.native

    def toDER(e: String): String = js.native
}
