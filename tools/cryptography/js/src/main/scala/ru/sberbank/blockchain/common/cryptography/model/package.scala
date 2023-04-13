package ru.sberbank.blockchain.common.cryptography

import com.google.protobuf.ByteString
import ru.sberbank.blockchain.cnft.commons.Bytes
import scalapb.TypeMapper
import scala.scalajs.js.typedarray.{Int8Array, byteArray2Int8Array}

/**
 * @author Alexey Polubelov
 */
package object model {
    implicit val ByteStringArrayMapper: TypeMapper[ByteString, Bytes] = new _root_.scalapb.TypeMapper[_root_.com.google.protobuf.ByteString, Bytes] {

        override def toCustom(base: ByteString): Bytes = byteArray2Int8Array(base.toByteArray).buffer

        override def toBase(custom: Bytes): ByteString = ByteString.copyFrom(new Int8Array(custom).toArray)
    }
}
