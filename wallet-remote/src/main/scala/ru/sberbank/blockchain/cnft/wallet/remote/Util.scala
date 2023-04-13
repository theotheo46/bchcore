package ru.sberbank.blockchain.cnft.wallet.remote

import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/**
 * @author Maxim Fedin
 */
object Util {

    private val logger = LoggerFactory.getLogger(this.getClass)

    //    def parse[A <: GeneratedMessage with Message[A]](content: Array[Byte], p: GeneratedMessageCompanion[A]): Either[String, A] =
    //        Try(p.parseFrom(content)).toEither.left.map(t => s"Failed to parse ${p.getClass.getSimpleName}: ${t.getMessage}")

    def decodeB64(b64: String, name: String): Either[String, Array[Byte]] =
        Try(Base64.getDecoder.decode(b64)).toEither.left.map(t => s"Failed to decode $name: ${t.getMessage}")

    def encodeB64(data: Array[Byte], name: String): Either[String, String] =
        Try(new String(Base64.getEncoder.encode(data), StandardCharsets.UTF_8))
            .toEither.left.map(t => s"Failed to encode $name: ${t.getMessage}")

    def toB64(data: Array[Byte]): String = new String(Base64.getEncoder.encode(data), StandardCharsets.UTF_8)


    def try2EitherWithLogging[T](obj: => T): Either[String, T] = {
        Try(obj) match {
            case Success(something) => Right(something)
            case Failure(err) =>
                val msg = s"Error: ${err.getMessage}"
                logger.error(msg, err)
                Left(msg)
        }
    }


    def environmentMandatory(name: String): String =
        sys.env.getOrElse(name,
            throw new Exception(s"Mandatory environment variable $name is missing.")
        )

    def environmentOptional(name: String): Option[String] =
        sys.env.get(name)

    def environmentOptional(name: String, default: String): String =
        sys.env.getOrElse(name, default)

    def stringToDuration(s: String): Duration = scala.concurrent.duration.Duration.apply(s)

}