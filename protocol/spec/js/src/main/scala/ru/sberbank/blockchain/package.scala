package ru.sberbank

import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, collectionFromArray, collectionToArray}
import upickle.core.Visitor

import java.nio.charset.StandardCharsets
import scala.reflect.ClassTag
import scala.scalajs.js.typedarray.{Int8Array, byteArray2Int8Array}

/**
 * @author Alexey Polubelov
 */
package object blockchain {
    implicit def CollectionRW[T: ClassTag : upickle.default.ReadWriter]: upickle.default.ReadWriter[Collection[T]] =
        upickle.default.ReadWriter.join(
                upickle.default.ArrayReader[T].map(collectionFromArray),
                upickle.default.ArrayWriter[T].comap(collectionToArray)
            )

    //TODO: use native base63 encoder/decoder i.e. TextEncoder/TextDecoder
    implicit val BytesAsBase64RW: upickle.default.ReadWriter[Bytes] = new upickle.default.ReadWriter[Bytes] with upickle.default.SimpleReader[Bytes] {

        override def expectedMsg: String = "expected string"

        override def visitString(s: CharSequence, index: Int): Bytes =
            byteArray2Int8Array(
                java.util.Base64.getDecoder.decode(
                    s.toString.getBytes(StandardCharsets.UTF_8)
                )
            ).buffer


        override def write0[V](out: Visitor[_, V], v: Bytes): V =
            out.visitString(
                new String(
                    java.util.Base64.getEncoder.encode(
                        new Int8Array(v).toArray
                    ),
                    StandardCharsets.UTF_8
                ),
                -1
            )

    }
}
