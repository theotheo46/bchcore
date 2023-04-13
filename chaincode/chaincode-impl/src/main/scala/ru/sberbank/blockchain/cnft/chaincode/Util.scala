package ru.sberbank.blockchain.cnft.chaincode

import org.enterprisedlt.fabric.contract.ContractCodecs
import org.enterprisedlt.general.codecs.ScalaPBCodec
import ru.sberbank.blockchain.cnft.commons.LoggingSupport

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.util.{Failure, Success, Try}

/**
 * @author Alexey Polubelov
 */
object Util extends LoggingSupport {

    val ProtoCodec = new ScalaPBCodec()
    val ContractCodecs: ContractCodecs = new ContractCodecs(
        parametersDecoder = ProtoCodec,
        ledgerCodec = ProtoCodec,
        resultEncoder = ProtoCodec,
        transientDecoder = ProtoCodec
    )

    //    val CryptoEngine = new BCCryptography(CryptographySettings.EC256_SHA256)

    //    val TokenEngine = new TokenIDsEngine(
    //        BCTokenStore,
    //        CryptoEngine
    //    )

    //    def verifySignature(keyBytes: Array[Byte], content: Array[Byte], signature: Array[Byte]): Boolean =
    //        CryptoEngine.verifySignature(keyBytes, content, signature)

    def decodeB64(b64: String, name: String): Either[String, Array[Byte]] =
        Try(Base64.getDecoder.decode(b64)).toEither.left.map(t => s"Failed to decode $name: ${t.getMessage}")

    def encodeB64(data: Array[Byte], name: String): Either[String, String] =
        Try(new String(Base64.getEncoder.encode(data), StandardCharsets.UTF_8))
            .toEither.left.map(t => s"Failed to encode $name: ${t.getMessage}")

    //    def parse[A <: GeneratedMessage with Message[A]](content: Array[Byte], p: GeneratedMessageCompanion[A]): Either[String, A] =
    //        Try(p.parseFrom(content)).toEither.left.map(t => s"Failed to parse ${p.getClass.getSimpleName}: ${t.getMessage}")

    def try2EitherWithLogging[T](obj: => T): Either[String, T] = {
        Try(obj) match {
            case Success(something) => Right(something)
            case Failure(err) =>
                val msg = s"Error: ${err.getMessage}"
                logger.error(msg, err)
                Left(msg)
        }
    }

    def toB64(data: Array[Byte]): String = new String(Base64.getEncoder.encode(data), StandardCharsets.UTF_8)

}
