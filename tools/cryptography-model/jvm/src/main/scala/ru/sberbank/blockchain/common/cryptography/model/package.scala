package ru.sberbank.blockchain.common.cryptography

import com.google.protobuf.ByteString
import ru.sberbank.blockchain.cnft.commons.Bytes
import scalapb.TypeMapper

/**
 * @author Alexey Polubelov
 */
package object model {
    implicit val ByteStringArrayMapper: TypeMapper[ByteString, Bytes] = new _root_.scalapb.TypeMapper[_root_.com.google.protobuf.ByteString, Bytes] {

        override def toCustom(base: ByteString): Bytes = base.toByteArray

        override def toBase(custom: Bytes): ByteString = ByteString.copyFrom(custom)
    }
}
