package ru.sberbank.blockchain.cnft.engine

import ru.sberbank.blockchain.cnft.commons.{Collection, LoggingSupport}

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.util.{Failure, Success, Try}

/**
 * @author Alexey Polubelov
 */
object Util extends LoggingSupport {

    def decodeB64(b64: String, name: String): Either[String, Array[Byte]] =
        Try(Base64.getDecoder.decode(b64)).toEither.left.map(t => s"Failed to decode $name: ${t.getMessage}")

    def encodeB64(data: Array[Byte], name: String): Either[String, String] =
        Try(new String(Base64.getEncoder.encode(data), StandardCharsets.UTF_8))
            .toEither.left.map(t => s"Failed to encode $name: ${t.getMessage}")

    def try2EitherWithLogging[T](obj: => T): Either[String, T] = {
        Try(obj) match {
            case Success(something) => Right(something)
            case Failure(err) =>
                val msg = err.getMessage
                logger.error(msg, err)
                Left(msg)
        }
    }

    def expect(condition: Boolean, msg: String): Either[String, Unit] =
        Either.cond(condition, (), msg)


    def expectOne[T](values: Collection[T], msg: String): Either[String, T] =
        values.headOption.toRight(msg)

}
