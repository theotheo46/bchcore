package ru.sberbank.blockchain.cnft

import java.nio.charset.StandardCharsets
import java.util
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

/**
 * @author Alexey Polubelov
 */
package object commons extends LoggingSupport {
    type Result[+T] = Either[String, T]

    implicit val ResultOps: ROps[Result] = new ROps[Result] {

        override def logInfo[A](v: Result[A], f: A => String): Result[A] =
            v.map { result =>
                logger.info(f(result))
                result
            }

        override def logDebug[A](v: Result[A], f: A => String): Result[A] =
            v.map { result =>
                logger.debug(f(result))
                result
            }

        override def map[A, B](v: Result[A], f: A => B): Result[B] = v.map(f)

        override def flatMap[A, B](v: Result[A], f: A => Result[B]): Result[B] = v.flatMap(f)

        override def fromOption[T](v: Option[T], ifNone: String): Result[T] =
            v match {
                case Some(value) => apply(value)
                case None =>
                    logger.error(s"R.fromOption: $ifNone", new Exception())
                    Fail(ifNone)
            }

        override def apply[A](g: => A): Result[A] = Result.apply(g)(logger)

        override def attempt[A](g: => A, reTryStrategy: ReTryStrategy): Result[A] = Result.attempt(g, reTryStrategy)

        override def Fail[T](msg: String): Result[T] = Left(msg)

        override def recover[A, B >: A](v: Result[A], f: String => Result[B]): Result[B] =
            v match {
                case Left(msg) => f(msg)
                case other => other
            }

        // implementation for synchronous R (i.e. Either)
        override def foldUntil[T, R](
            items: Iterable[T], zero: R,
            cond: R => Result[Boolean],
            transform: (R, T) => Result[R]
        ): Result[(R, Boolean)] = {
            val it = items.iterator
            cond(zero)
                .map(v => (zero, v))
            match {
                case r@Right((_, stop)) =>
                    if (stop) r
                    else {
                        var current = zero
                        while (it.hasNext) {
                            val next = it.next()
                            current =
                                transform(current, next)
                                    .flatMap { v =>
                                        cond(v).map(s => (v, s))
                                    }
                                match {
                                    case r@Right((value, cond)) =>
                                        if (cond) return r
                                        else value
                                    case fail => return fail
                                }
                        }
                        Result((current, false))
                    }
                case fail => fail
            }
        }

    }

    object Result {
        def Ok(): Result[Unit] = Ok(())

        def Ok[T](v: T): Result[T] = Right(v)

        def Fail[T](msg: String): Result[T] = Left(msg)

        def apply[T](f: => T)(implicit logger: Logger): Result[T] =
            scala.util.Try(f) match {
                case Success(something) => Ok(something)
                case Failure(err) =>
                    val msg = s"Error: ${err.getMessage}"
                    logger.error(msg, err)
                    Fail(msg)
            }

        def attempt[T](f: => T, retry: ReTryStrategy): Result[T] =
            Stream
                .continually(scala.util.Try(f))
                .zipWithIndex.map { case (v, i) => (v, i + 1) } // index is zero based
                .find {
                    case (Failure(t), n) => !retry.shellReTry(t, n)
                    case (Success(_), _) => true
                }
                .map {
                    case (Success(something), _) => Ok(something)
                    case (Failure(err), n) =>
                        val msg = s"Error (try N = $n): ${err.getMessage}"
                        logger.error(msg, err)
                        Fail(msg)
                }
                .getOrElse(Fail("Unexpected fail"))

        def expect(p: Boolean, error: String): Result[Unit] = if (p) Ok() else Fail(error)
    }

    //
    // Collection
    //

    type Collection[T] = scala.Array[T] //scala.collection.Seq[T]
    val Collection: Array.type = scala.Array // : Seq.type = scala.collection.Seq

    def collectionToArray[T](v: Collection[T]): scala.Array[T] = v

    def collectionToSequence[T](v: Collection[T]): scala.collection.Seq[T] = Predef.genericArrayOps(v).toSeq

    def collectionFromArray[T](v: scala.Array[T]): Collection[T] = v

    @inline def collectionFromSequence[T: ClassTag](v: scala.collection.Seq[T]): Collection[T] = v.toArray

    @inline def collectionFromIterable[T: ClassTag](v: scala.collection.Iterable[T]): Collection[T] = v.toArray

    //
    // Bytes
    //

    type Bytes = scala.Array[Byte]
    val Bytes: Array.type = scala.Array

    def concatBytes(buffers: Seq[Bytes]): Bytes = {
        val totalSize = buffers.map(_.length).sum
        val result = new Array[Byte](totalSize)
        var index: Int = 0
        buffers.foreach { buffer =>
            System.arraycopy(buffer, 0, result, index, buffer.length)
            index = index + buffer.length
        }
        result
    }

    @inline def asBytes(v: Array[Byte]): Bytes = v

    @inline def asByteArray(v: Bytes): Array[Byte] = v

    def isEqualBytes(a: Bytes, b: Bytes): Boolean = util.Arrays.equals(a, b)

    @inline def isEmptyBytes(v: Bytes): Boolean = v.isEmpty
    //

    type Optional[+T] = Option[T]

    @inline def optionalFromOption[T](o: Option[T]): Optional[T] = o

    @inline def optionFromOptional[T](o: Optional[T]): Option[T] = o

    implicit class BytesOps(value: Bytes) {

        def toUTF8: String = new String(asByteArray(value), StandardCharsets.UTF_8)

        def setInt(pos: Int, v: Int): Bytes = {
            value(pos) = (v >>> 0).toByte
            value(pos + 1) = (v >>> 8).toByte
            value(pos + 2) = (v >>> 16).toByte
            value(pos + 3) = (v >>> 24).toByte
            value
        }
    }

    type BigInt = java.math.BigInteger

    def big2long(big: BigInt): Long = big.longValue

}
