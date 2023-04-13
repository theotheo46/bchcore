package ru.sberbank.blockchain.cnft

import com.google.protobuf.ByteString
import ru.sberbank.blockchain.cnft.commons.Bytes
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, TypeMapper}

/**
 * @author Alexey Polubelov
 */
package object model {

    implicit val ByteStringArrayMapper: TypeMapper[ByteString, Bytes] = new _root_.scalapb.TypeMapper[_root_.com.google.protobuf.ByteString, Bytes] {

        override def toCustom(base: ByteString): Bytes = base.toByteArray

        override def toBase(custom: Bytes): ByteString = ByteString.copyFrom(custom)
    }

    implicit class GeneratedMessageOps(m: GeneratedMessage) {
        @inline def toBytes: Bytes = m.toByteArray
    }

    implicit class GeneratedMessageCompanionOps[M <: GeneratedMessage](m: GeneratedMessageCompanion[M]) {
        @inline def fromBytes(bytes: Bytes): M = m.parseFrom(bytes)
    }

}
