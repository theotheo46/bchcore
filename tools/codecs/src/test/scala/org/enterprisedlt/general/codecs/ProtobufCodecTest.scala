package org.enterprisedlt.general.codecs

import java.nio.charset.StandardCharsets
import org.enterprisedlt.general.proto.TestMessage
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner

import java.util


/**
 * @author Maxim Fedin
 */
@RunWith(classOf[JUnitRunner])
class ProtobufCodecTest extends FunSuite {

    val codec: ProtobufCodec = new ProtobufCodec()

    case class TestClass()

    test("String encoding/decoding works fine") {
        val msg: String = "Hello World"
        //
        val encoded = codec.encode[String](msg)
        val decoded = codec.decode[String](encoded, classOf[String])
        //
        assert(msg == decoded)
    }

    test("Array encoding/decoding works fine") {
        val msg: Array[Byte] = "Hello world".getBytes(StandardCharsets.UTF_8)
        //
        val encoded = codec.encode[Array[Byte]](msg)
        val decoded = codec.decode[Array[Byte]](encoded, classOf[Array[Byte]])
        //
        assert(util.Arrays.equals(msg, decoded))
    }

    test("Protobuf message encoding/decoding works fine") {
        val msg: TestMessage = TestMessage.newBuilder()
            .setStringValue("Hello World")
            .setIntValue(10)
            .build()
        //
        val encoded = codec.encode[TestMessage](msg)
        val decoded = codec.decode[TestMessage](encoded, classOf[TestMessage])
        //
        assert(msg == decoded)
    }

    test("Unsupported class for non-protobuf object works fine") {
        val msg: Array[Byte] = "Hello World".getBytes(StandardCharsets.UTF_8)
        an[java.lang.Exception] should be thrownBy { // Ensure a particular exception type is thrown
            codec.decode[TestClass](msg, classOf[TestClass])
        }
    }

    test("null encoding/decoding works fine") {
        val msg = null
        //
        val encoded = codec.encode[AnyRef](msg)
        val decoded = codec.decode[AnyRef](encoded, classOf[AnyRef])
        //
        assert(msg == decoded)
    }

    test("Positive Int encoding/decoding works fine") {
        val msg: Int = 100
        //
        val encoded = codec.encode[Int](msg)
        val decoded = codec.decode[Int](encoded, classOf[Int])
        //
        assert(msg == decoded)
    }

    test("Boolean true encoding/decoding works fine") {
        val msg: Boolean = true
        //
        val encoded = codec.encode[Boolean](msg)
        val decoded = codec.decode[Boolean](encoded, classOf[Boolean])
        //
        assert(msg == decoded)
    }

    test("Boolean false encoding/decoding works fine") {
        val msg: Boolean = false
        //
        val encoded = codec.encode[Boolean](msg)
        val decoded = codec.decode[Boolean](encoded, classOf[Boolean])
        //
        assert(msg == decoded)
    }

    test("Negative Int encoding/decoding works fine") {
        val msg: Int = -100
        //
        val encoded = codec.encode[Int](msg)
        val decoded = codec.decode[Int](encoded, classOf[Int])
        //
        assert(msg == decoded)
    }

    test("Positive Byte encoding/decoding works fine") {
        val msg: Byte = 123
        //
        val encoded = codec.encode[Byte](msg)
        val decoded = codec.decode[Byte](encoded, classOf[Byte])
        //
        assert(msg == decoded)
    }

    test("Negative Byte encoding/decoding works fine") {
        val msg: Byte = -123
        //
        val encoded = codec.encode[Byte](msg)
        val decoded = codec.decode[Byte](encoded, classOf[Byte])
        //
        assert(msg == decoded)
    }

    test("Positive Short encoding/decoding works fine") {
        val msg: Short = 2
        //
        val encoded = codec.encode[Short](msg)
        val decoded = codec.decode[Short](encoded, classOf[Short])
        //
        assert(msg == decoded)
    }

    test("Negative Short encoding/decoding works fine") {
        val msg: Short = -2
        //
        val encoded = codec.encode[Short](msg)
        val decoded = codec.decode[Short](encoded, classOf[Short])
        //
        assert(msg == decoded)
    }

    test("Char encoding/decoding works fine") {
        val msg: Char = 'X'
        //
        val encoded = codec.encode[Char](msg)
        val decoded = codec.decode[Char](encoded, classOf[Char])
        //
        assert(msg == decoded)
    }

    test("Positive Float encoding/decoding works fine") {
        val msg: Float = .1f
        //
        val encoded = codec.encode[Float](msg)
        val decoded = codec.decode[Float](encoded, classOf[Float])
        //
        assert(msg == decoded)
    }

    test("Negative Float encoding/decoding works fine") {
        val msg: Float = -.1f
        //
        val encoded = codec.encode[Float](msg)
        val decoded = codec.decode[Float](encoded, classOf[Float])
        //
        assert(msg == decoded)
    }

    test("Positive Long encoding/decoding works fine") {
        val msg: Long = 100000
        //
        val encoded = codec.encode[Long](msg)
        val decoded = codec.decode[Long](encoded, classOf[Long])
        //
        assert(msg == decoded)
    }

    test("Negative Long encoding/decoding works fine") {
        val msg: Long = -100000
        //
        val encoded = codec.encode[Long](msg)
        val decoded = codec.decode[Long](encoded, classOf[Long])
        //
        assert(msg == decoded)
    }

    test("Positive Double encoding/decoding works fine") {
        val msg: Double = 100000.12
        //
        val encoded = codec.encode[Double](msg)
        val decoded = codec.decode[Double](encoded, classOf[Double])
        //
        assert(msg == decoded)
    }

    test("Negative Double encoding/decoding works fine") {
        val msg: Double = -100000.12
        //
        val encoded = codec.encode[Double](msg)
        val decoded = codec.decode[Double](encoded, classOf[Double])
        //
        assert(msg == decoded)
    }

    test("Unit encoding/decoding works fine") {
        val msg: Unit = ()
        //
        val encoded = codec.encode[Unit](msg)
        val decoded = codec.decode[Unit](encoded, classOf[Unit])
        //
        assertResult(msg)(decoded)
    }
}