package org.enterprisedlt.general.codecs

import java.lang.reflect.Type

import com.google.protobuf._
import org.enterprisedlt.spec.BinaryCodec

/**
 * @author Maxim Fedin
 */
class ProtobufCodec extends BinaryCodec {

    override def encode[T](value: T): Array[Byte] =
        value match {
            case null => Array.empty

            case _: Unit => Array.empty

            case m: Byte =>
                val buffer = new Array[Byte](1)
                CodedOutputStream.newInstance(buffer).write(m)
                buffer

            case m: Boolean =>
                val buffer = new Array[Byte](1)
                CodedOutputStream.newInstance(buffer).writeBoolNoTag(m)
                buffer

            case m: Short =>
                val buffer = new Array[Byte](2)
                CodedOutputStream.newInstance(buffer).writeSInt32NoTag(m.toInt)
                buffer

            case m: Char =>
                val buffer = new Array[Byte](2)
                CodedOutputStream.newInstance(buffer).writeSInt32NoTag(m.toInt)
                buffer

            case m: Int =>
                val buffer = new Array[Byte](4)
                CodedOutputStream.newInstance(buffer).writeSInt32NoTag(m)
                buffer

            case m: Float =>
                val buffer = new Array[Byte](4)
                CodedOutputStream.newInstance(buffer).writeFloatNoTag(m)
                buffer

            case m: Long =>
                val buffer = new Array[Byte](8)
                CodedOutputStream.newInstance(buffer).writeSInt64NoTag(m)
                buffer

            case m: Double =>
                val buffer = new Array[Byte](8)
                CodedOutputStream.newInstance(buffer).writeDoubleNoTag(m)
                buffer

            case m: String =>
                val buffer = ByteString.copyFromUtf8(m)
                buffer.toByteArray

            case m: Array[Byte] =>
                val buffer = ByteString.copyFrom(m)
                buffer.toByteArray


            case m: Message => m.toByteArray

            case _ => throw new Exception("Unsupported class")
        }


    override def decode[T](value: Array[Byte], clz: Type): T =
        clz match {

            case x: Class[_] if classOf[Unit].equals(x) => ().asInstanceOf[T]

            case _ if value.isEmpty => null.asInstanceOf[T]

            case x: Class[_] if classOf[Int].equals(x) =>
                CodedInputStream
                    .newInstance(value)
                    .readSInt32()
                    .asInstanceOf[T]

            case x: Class[_] if classOf[Boolean].equals(x) =>
                CodedInputStream
                    .newInstance(value)
                    .readBool()
                    .asInstanceOf[T]

            case x: Class[_] if classOf[Byte].equals(x) =>
                CodedInputStream
                    .newInstance(value)
                    .readRawByte()
                    .asInstanceOf[T]

            case x: Class[_] if classOf[Short].equals(x) =>
                CodedInputStream
                    .newInstance(value)
                    .readSInt32()
                    .toShort
                    .asInstanceOf[T]

            case x: Class[_] if classOf[Char].equals(x) =>
                CodedInputStream
                    .newInstance(value)
                    .readSInt32()
                    .toChar
                    .asInstanceOf[T]

            case x: Class[_] if classOf[Float].equals(x) =>
                CodedInputStream
                    .newInstance(value)
                    .readFloat()
                    .asInstanceOf[T]

            case x: Class[_] if classOf[Long].equals(x) =>
                CodedInputStream
                    .newInstance(value)
                    .readSInt64()
                    .asInstanceOf[T]

            case x: Class[_] if classOf[Double].equals(x) =>
                CodedInputStream
                    .newInstance(value)
                    .readDouble()
                    .asInstanceOf[T]

            case x: Class[_] if classOf[String].equals(x) =>
                ByteString
                    .copyFrom(value)
                    .toStringUtf8
                    .asInstanceOf[T]

            case x: Class[_] if classOf[Array[Byte]].equals(x) =>
                ByteString
                    .copyFrom(value)
                    .toByteArray
                    .asInstanceOf[T]

            case x: Class[_] if classOf[Message].isAssignableFrom(x) =>
                x
                    .getMethod("parser")
                    .invoke(null)
                    .asInstanceOf[Parser[T]]
                    .parseFrom(value)

            case t =>
                throw new Exception(s"Unsupported type: ${t.getTypeName}")
        }
}

object ProtobufCodec {
    def apply(): ProtobufCodec = new ProtobufCodec()
}
