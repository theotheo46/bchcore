package ru.sberbank.blockchain.cnft.commons

import ru.sberbank.blockchain.cnft.commons.ROps.summonHasOps

import java.math.BigInteger
import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
object Base58 {
    private val exclude = Set('O', 'I', 'l')
    private val ABC =
        (('1' to '9') ++ ('A' to 'Z') ++ ('a' to 'z'))
            .filterNot(c => exclude.contains(c))
            .toArray

    private val BASE = BigInteger.valueOf(ABC.length.toLong)

    def encode(bytes: Array[Byte]): String = {
        val zerosCount = bytes.takeWhile(_ == 0).length
        var c = new BigInteger(1, bytes)
        val sb = new java.lang.StringBuilder(zerosCount + (bytes.length * 1.4).toInt)
        while (!c.equals(BigInteger.ZERO)) {
            val Array(q, r) = c.divideAndRemainder(BASE)
            sb.insert(0, ABC(r.intValue()))
            c = q
        }
        if (zerosCount != 0) {
            for {_ <- 0 until zerosCount} yield sb.insert(0, '1')
        }
        sb.toString
    }

    /**
     * Safe version of decode, in case the value is invalid the method will return Fail
     *
     * @param value - Base58 encoded value
     * @return - decoded bytes
     */
    def decodeR[R[+_]](value: String)(implicit R: ROps[R]): R[Array[Byte]] = {
        val zerosCount = value.takeWhile(_ == '1').length
        val v = value.splitAt(zerosCount)._2
        ROps.IterableR_Ops(v.toIterable)(R)
            .foldLeftR(BigInteger.ZERO) { case (r, c) =>
                for {
                    index <- R(ABC.indexOf(c).toLong)
                    _ <- R.expect(index >= 0, s"Invalid character: $c")
                } yield r.multiply(BASE).add(BigInteger.valueOf(index))
            }
            .map(_.toByteArray.dropWhile(_ == 0)) // drop sign byte
            .map { n =>
                if (zerosCount != 0) {
                    Array.fill(zerosCount)(0.toByte) ++ n
                } else n
            }
    }


    /**
     * Unsafe version which with throw exception in case the value is invalid
     * NOTE: will throw Exception if input is invalid
     *
     * @param value - Base58 encoded value
     * @return - decoded bytes
     */
    def decode(value: String): Array[Byte] = {
        val zerosCount = value.takeWhile(_ == '1').length
        val v = value.splitAt(zerosCount)._2
        val r = v.foldLeft(BigInteger.ZERO) { case (r, c) =>
            val index = ABC.indexOf(c).toLong
            if (index < 0) throw new Exception(s"Invalid character: $c")
            r.multiply(BASE).add(BigInteger.valueOf(index))
        }.toByteArray.dropWhile(_ == 0) // drop sign byte
        if (zerosCount != 0) {
            Array.fill(zerosCount)(0.toByte) ++ r
        } else r
    }

}
