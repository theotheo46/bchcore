package ru.sberbank.blockchain.common.cryptography

import java.math.BigInteger
import java.util

object P1363Convert {

    def signatureToDer(plain: Array[Byte]): Array[Byte] = {
        // for example assume 256-bit-order curve like P-256
        val n: Int = 32
        val r: BigInteger = new BigInteger(+1, util.Arrays.copyOfRange(plain, 0, n))
        val s: BigInteger = new BigInteger(+1, util.Arrays.copyOfRange(plain, n, n * 2))
        val x1: Array[Byte] = r.toByteArray
        val x2: Array[Byte] = s.toByteArray
        // already trimmed two's complement, as DER wants
        val len: Int = x1.length + x2.length + (2 + 2)
        var idx: Int = if (len >= 128) 3 else 2
        // and can be removed if you will definitely not use such curve(s)
        val out: Array[Byte] = Array.ofDim[Byte](idx + len)
        out(0) = 0x30
        if (idx == 3) {
            out(1) = 0x81.toByte
            out(2) = len.toByte
        } else {
            out(1) = len.toByte
        }
        out(idx) = 2
        out(idx + 1) = x1.length.toByte
        System.arraycopy(x1, 0, out, idx + 2, x1.length)
        idx += x1.length + 2
        out(idx) = 2
        out(idx + 1) = x2.length.toByte
        System.arraycopy(x2, 0, out, idx + 2, x2.length)
        out
    }

    // the len>=128 case can only occur for curves of 488 bits or more,
    // the len>=128 case can only occur for curves of 488 bits or more,

    def signatureFromDer(der: Array[Byte]): Array[Byte] = {
        // for example assume 256-bit-order curve like P-256
        val n: Int = 32
        var r: BigInteger = null
        var s: BigInteger = null
        var out: Array[Byte] = null
        if (der(0) != 0x30) throw new Exception()
        // the 0x81 case only occurs for curve over 488 bits
        var idx: Int = if (der(1) == 0x81) 3 else 2
        if (der(idx) != 2) throw new Exception()
        r = new BigInteger(
            1,
            util.Arrays.copyOfRange(der, idx + 2, idx + 2 + der(idx + 1)))
        idx += der(idx + 1) + 2
        if (der(idx) != 2) throw new Exception()
        s = new BigInteger(
            1,
            util.Arrays.copyOfRange(der, idx + 2, idx + 2 + der(idx + 1)))
        if (idx + der(idx + 1) + 2 != der.length) throw new Exception()
        // common output
        out = Array.ofDim[Byte](2 * n)
        convertToFixed(r, out, 0, n)
        convertToFixed(s, out, n, n)
        out
    }

    private def convertToFixed(x: BigInteger, a: Array[Byte], off: Int, len: Int): Unit = {
        val t: Array[Byte] = x.toByteArray
        if (t.length == len + 1 && t(0) == 0) System.arraycopy(t, 1, a, off, len)
        else if (t.length <= len)
            System.arraycopy(t, 0, a, off + len - t.length, t.length)
        else throw new Exception()
    }

    //    def byteArrayToUint8Array(arr: Array[Byte]): Uint8Array = {
    //        js.Dynamic.newInstance(js.Dynamic.global.Uint8Array)(arr.toJSArray).asInstanceOf[Uint8Array]
    //    }

}