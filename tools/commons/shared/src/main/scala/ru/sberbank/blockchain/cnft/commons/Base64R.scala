package ru.sberbank.blockchain.cnft.commons

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
object Base64R {
    def encode[R[+_] : ROps](v: Array[Byte]): R[String] =
        implicitly[ROps[R]].apply {
            new String(Base64.getEncoder.encode(v), StandardCharsets.UTF_8)
        }

    def decode[R[+_] : ROps](v: String): R[Array[Byte]] =
        implicitly[ROps[R]].apply {
            Base64.getDecoder.decode(v.getBytes(StandardCharsets.UTF_8))
        }
}
