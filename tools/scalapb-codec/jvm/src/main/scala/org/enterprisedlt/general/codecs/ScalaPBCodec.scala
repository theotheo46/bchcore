package org.enterprisedlt.general.codecs

import com.google.protobuf._
import org.enterprisedlt.spec.BinaryCodec
import scalapb.GeneratedMessage

import java.io.ByteArrayOutputStream
import java.lang.reflect.Type
import scala.reflect.ClassTag

/**
 * @author Alexey Polubelov
 */
class ScalaPBCodec extends BinaryCodec {
    //    private val logger = LoggerFactory.getLogger(this.getClass)

    override def encode[T](value: T): Array[Byte] =
        value match {
            case null => Array.empty

            case _: Unit => Array.empty

            case m: GeneratedMessage => m.toByteArray

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

            case m if m.getClass.isArray =>
                val baos = new ByteArrayOutputStream()
                val cos = CodedOutputStream.newInstance(baos)
                val messages = m.asInstanceOf[Array[_]]
                cos.writeUInt32NoTag(messages.length)
                messages.foreach { m =>
                    val encoded = this.encode(m)
                    cos.writeByteArrayNoTag(encoded)
                }
                cos.flush()
                val result = baos.toByteArray
                result

            case _ => throw new Exception("Unsupported class")
        }

    override def decode[T](value: Array[Byte], clz: Type): T =
        clz match {

            case x: Class[_] if classOf[Unit].equals(x) => ().asInstanceOf[T]

            case _ if value.isEmpty => null.asInstanceOf[T]

            case x: Class[_] if classOf[GeneratedMessage].isAssignableFrom(x) =>
                val name = x.getCanonicalName
                val clazz = java.lang.Class.forName(name + "$")
                val companion = clazz.getField("MODULE$").get(clazz)
                val parseFrom = clazz.getMethod("parseFrom", classOf[Array[Byte]])
                parseFrom.invoke(companion, value).asInstanceOf[T]

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

            case x: Class[_] if x.isArray =>
                val elementType = x.getComponentType
                val input = CodedInputStream.newInstance(value)
                val size = input.readUInt32()
                val result = readArrayGeneric(size, input, elementType)
                result.asInstanceOf[T]

            case t =>
                throw new Exception(s"Unsupported type: ${t.getTypeName}")
        }

    private def readArrayGeneric(size: Int, input: CodedInputStream, elementType: Class[_]) =
        elementType match {
            case x if classOf[Int].equals(x) => readArray[Int](size, input, elementType)
            case x if classOf[Boolean].equals(x) => readArray[Boolean](size, input, elementType)
            case x if classOf[Byte].equals(x) => readArray[Byte](size, input, elementType)
            case x if classOf[Short].equals(x) => readArray[Short](size, input, elementType)
            case x if classOf[Char].equals(x) => readArray[Char](size, input, elementType)
            case x if classOf[Float].equals(x) => readArray[Float](size, input, elementType)
            case x if classOf[Long].equals(x) => readArray[Long](size, input, elementType)
            case x if classOf[Double].equals(x) => readArray[Double](size, input, elementType)
            case _ => readArray[AnyRef](size, input, elementType)
        }

    private def readArray[T: ClassTag](size: Int, input: CodedInputStream, elementType: Class[_]) = {
        val result = java.lang.reflect.Array.newInstance(elementType, size).asInstanceOf[Array[T]]
        for (i <- 0 until size) {
            val bytes = input.readByteArray()
            val e = this.decode[T](bytes, elementType)
            result(i) = e
        }
        result
    }
}
