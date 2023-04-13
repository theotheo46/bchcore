package org.enterprisedlt.general.gson

import com.google.gson.{Gson, GsonBuilder}
import org.enterprisedlt.general.codecs.GsonCodec
import org.enterprisedlt.spec.BinaryCodec
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner


/**
 * @author Alexey Polubelov
 */
@RunWith(classOf[JUnitRunner])
class TypedGsonTest extends FunSuite {

    test("library should determine leaf type") {
        val codec: Gson = (new GsonBuilder).encodeTypes().create()
        val encoded = codec.toJson(Dummy("Hey", 1, 2))
        val decoded = codec.fromJson(encoded, classOf[DummyBase]) // check the type determined by adapter
        assert(decoded.isInstanceOf[Dummy])
        val dummy = decoded.asInstanceOf[Dummy]
        assert(dummy.someString == "Hey")
        assert(dummy.someInt == 1)
        assert(dummy.someFloat == 2)
    }

    test("Unit object over typed codec") {
        val codec: BinaryCodec = GsonCodec(gsonOptions = _.encodeTypes())
        val msg: Unit = ()
        val encoded = codec.encode[Unit](msg)
        val decoded: Unit = codec.decode[Unit](encoded, classOf[Unit])
        assertResult(msg)(decoded)
    }

}
