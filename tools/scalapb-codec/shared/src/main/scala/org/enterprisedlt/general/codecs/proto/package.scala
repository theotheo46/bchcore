package org.enterprisedlt.general.codecs

import com.google.protobuf.{ByteString, CodedInputStream, CodedOutputStream}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import utility.{Decoder, Encoder}

import java.io.ByteArrayOutputStream
import scala.reflect.ClassTag

/**
 * @author Alexey Polubelov
 */
package object proto {

    // ================================ Boolean ===============================

    implicit val BooleanDecoder: Decoder[Boolean, Array[Byte]] =
        (encoded: Array[Byte]) => CodedInputStream.newInstance(encoded).readBool()

    implicit val BooleanEncoder: Encoder[Boolean, Array[Byte]] =
        (value: Boolean) => {
            val result = new Array[Byte](1)
            val stream = CodedOutputStream.newInstance(result)
            stream.writeBoolNoTag(value)
            stream.flush()
            result
        }

    // ================================= Unit =================================

    implicit val UnitDecoder = new Decoder[Unit, Array[Byte]] {
        override def decode(encoded: Array[Byte]): Unit = ()
    }

    implicit val UnitEncoder = new Encoder[Unit, Array[Byte]] {
        override def encode(value: Unit): Array[Byte] = Array.empty[Byte]
    }

    // ================================= String =================================

    implicit val StringDecoder = new Decoder[String, Array[Byte]] {
        override def decode(encoded: Array[Byte]): String = ByteString.copyFrom(encoded).toStringUtf8
    }

    implicit val StringEncoder = new Encoder[String, Array[Byte]] {
        override def encode(value: String): Array[Byte] = ByteString.copyFromUtf8(value).toByteArray
    }

    // ================================= Long =================================

    implicit val LongDecoder = new Decoder[Long, Array[Byte]] {
        override def decode(encoded: Array[Byte]): Long = {
            val input = CodedInputStream.newInstance(encoded)
            input.readSInt64()
        }
    }

    implicit val LongEncoder = new Encoder[Long, Array[Byte]] {
        override def encode(value: Long): Array[Byte] = {
            val baos = new ByteArrayOutputStream()
            val cos = CodedOutputStream.newInstance(baos)
            cos.writeSInt64NoTag(value)
            cos.flush()
            baos.toByteArray
        }
    }

    // ================================= Array of  Bytes =================================

    implicit val ArrayOfBytesDecoder = new Decoder[Array[Byte], Array[Byte]] {
        override def decode(encoded: Array[Byte]): Array[Byte] = encoded
    }

    implicit val ArrayOfBytesEncoder = new Encoder[Array[Byte], Array[Byte]] {
        override def encode(value: Array[Byte]): Array[Byte] = value
    }

    // ================================= Proto Message =================================

    implicit def MessageDecoder[T <: GeneratedMessage : GeneratedMessageCompanion] = new Decoder[T, Array[Byte]] {
        override def decode(encoded: Array[Byte]): T = implicitly[GeneratedMessageCompanion[T]].parseFrom(encoded)
    }

    implicit def MessageEncoder[T <: GeneratedMessage] = new Encoder[T, Array[Byte]] {
        override def encode(value: T): Array[Byte] = value.toByteArray
    }

    // ================================= Array decodeable elements =================================

    implicit def ArrayDecoder[T](implicit tag: ClassTag[T], decoder: Decoder[T, Array[Byte]]) = new Decoder[Array[T], Array[Byte]] {
        override def decode(encoded: Array[Byte]): Array[T] = {
            val input = CodedInputStream.newInstance(encoded)
            val size = input.readUInt32()
            val result = new Array[T](size)
            for (i <- 0 until size) {
                val bytes = input.readByteArray()
                val e = decoder.decode(bytes)
                result(i) = e
            }
            result
        }
    }

    implicit def ArrayEncoder[T](implicit encoder: Encoder[T, Array[Byte]]) = new Encoder[Array[T], Array[Byte]] {
        override def encode(value: Array[T]): Array[Byte] = {
            val baos = new ByteArrayOutputStream()
            val cos = CodedOutputStream.newInstance(baos)
            cos.writeUInt32NoTag(value.length)
            value.foreach { m =>
                cos.writeByteArrayNoTag(encoder.encode(m))
            }
            cos.flush()
            baos.toByteArray
        }
    }

}
