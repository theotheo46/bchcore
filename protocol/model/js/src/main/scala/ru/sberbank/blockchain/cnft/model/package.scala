package ru.sberbank.blockchain.cnft

import com.google.protobuf.ByteString
import ru.sberbank.blockchain.cnft.commons.{Bytes, asBytes}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, TypeMapper}

import scala.scalajs.js.typedarray.Int8Array

/**
 * @author Alexey Polubelov
 */
package object model {

    implicit val ByteStringArrayMapper: TypeMapper[ByteString, Bytes] = new _root_.scalapb.TypeMapper[_root_.com.google.protobuf.ByteString, Bytes] {

        override def toCustom(base: ByteString): Bytes = asBytes(base.toByteArray)

        override def toBase(custom: Bytes): ByteString = ByteString.copyFrom(new Int8Array(custom).toArray)
    }

    implicit class GeneratedMessageOps(m: GeneratedMessage) {
        @inline def toBytes: Bytes = asBytes(m.toByteArray)
    }

    implicit class GeneratedMessageCompanionOps[M <: GeneratedMessage](m: GeneratedMessageCompanion[M]) {
        @inline def fromBytes(bytes: Bytes): M = m.parseFrom(new Int8Array(bytes).toArray)
    }

}
