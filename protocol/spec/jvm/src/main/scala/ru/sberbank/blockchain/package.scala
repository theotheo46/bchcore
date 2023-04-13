package ru.sberbank

import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection}
import upickle.core.Visitor

import java.nio.charset.StandardCharsets
import scala.reflect.ClassTag

/**
 * @author Alexey Polubelov
 */
package object blockchain {
    implicit def CollectionRW[T: ClassTag : upickle.default.ReadWriter]: upickle.default.ReadWriter[Collection[T]] =
        upickle.default.ReadWriter.join(
            upickle.default.ArrayReader[T],
            upickle.default.ArrayWriter[T]
        )

    implicit val BytesAsBase64RW: upickle.default.ReadWriter[Bytes] = new upickle.default.ReadWriter[Bytes] with upickle.default.SimpleReader[Bytes] {

        override def expectedMsg: String = "expected string"

        override def visitString(s: CharSequence, index: Int): Bytes =
            java.util.Base64.getDecoder.decode(
                s.toString.getBytes(StandardCharsets.UTF_8)
            )


        override def write0[V](out: Visitor[_, V], v: Bytes): V =
            out.visitString(
                new String(
                    java.util.Base64.getEncoder.encode(v),
                    StandardCharsets.UTF_8
                ),
                -1
            )
    }
}
