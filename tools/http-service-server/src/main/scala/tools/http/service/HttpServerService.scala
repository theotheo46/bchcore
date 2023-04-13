package tools.http.service

import ru.sberbank.blockchain.cnft.commons
import ru.sberbank.blockchain.cnft.commons.{LoggingSupport, Result, BigInt}
import utility.{Decoder, Encoder, RoutesUtility}

/**
 * @author Alexey Polubelov
 */
object HttpServerService extends LoggingSupport {

    implicit val String2StringDecoder = new Decoder[String, String] {
        override def decode(encoded: String): String = {
            if (encoded.startsWith("\"")) encoded.replaceAll("^\"|\"$", "") else encoded
        }
    }

    implicit val BigIntDecoder = new Decoder[commons.BigInt, String] {
        override def decode(encoded: String): commons.BigInt = {
            new BigInt(encoded)
        }
    }

    implicit val BigIntEncoder = new Encoder[commons.BigInt, String] {
        override def encode(value: commons.BigInt): String = {
            value.toString
        }
    }

    implicit def upickleEncoder[T: upickle.default.Writer]: Encoder[T, String] =
        (value: T) => upickle.default.write(value)

    implicit def upickleDecoder[T: upickle.default.Reader]: Decoder[T, String] =
        (value: String) => upickle.default.read(value)

    def serviceHandler[T](service: T)(implicit utilityT: RoutesUtility[T, Result, String]) =
        new ServiceJettyHandler[T](service)

}
