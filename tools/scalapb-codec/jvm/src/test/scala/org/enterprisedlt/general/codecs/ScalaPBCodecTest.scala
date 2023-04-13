package org.enterprisedlt.general.codecs

/**
 * @author Alexey Polubelov
 */
class ScalaPBCodecTest extends org.scalatest.funsuite.AnyFunSuite {
    private val codec = new ScalaPBCodec()

    test("int arrays should work") {
        val sample = Array(1, 2, 3)
        val encoded = codec.encode(sample)
        val decoded = codec.decode[Array[Int]](encoded, classOf[Array[Int]])
        println(decoded.mkString("Array(", ", ", ")"))
        assert(decoded sameElements sample)
    }

    test("string arrays should work") {
        val sample = Array("a", "b", "c")
        val encoded = codec.encode(sample)
        val decoded = codec.decode[Array[String]](encoded, classOf[Array[String]])
        println(decoded.mkString("Array(", ", ", ")"))
        assert(decoded sameElements sample)
    }

    test("boolean arrays should work") {
        val sample = Array(true, false, true)
        val encoded = codec.encode(sample)
        val decoded = codec.decode[Array[Boolean]](encoded, classOf[Array[Boolean]])
        println(decoded.mkString("Array(", ", ", ")"))
        assert(decoded sameElements sample)
    }
}
