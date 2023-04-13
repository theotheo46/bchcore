package ru.sberbank.blockchain.cnft

import org.scalajs.dom.console

import java.nio.charset.StandardCharsets
import java.util
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.typedarray.{Int32Array, Int8Array, Uint8Array, byteArray2Int8Array}
import scala.util.control.NoStackTrace

/**
 * @author Alexey Polubelov
 */
package object commons {
    type Result[+T] = js.Promise[T]

    import scala.scalajs.js.JSConverters._
    import scala.scalajs.js.Thenable.Implicits._

    implicit val ResultOps: ROps[Result] = new ROps[Result] {

        override def logInfo[A](v: Result[A], f: A => String): Result[A] =
            v.map { result =>
                console.info(f(result))
                result
            }.toJSPromise

        override def logDebug[A](v: Result[A], f: A => String): Result[A] =
            v.map { result =>
                console.debug(f(result))
                result
            }.toJSPromise


        override def map[A, B](v: Result[A], f: A => B): Result[B] = v.map(f).toJSPromise

        override def flatMap[A, B](v: Result[A], f: A => Result[B]): Result[B] = v.flatMap(v => f(v)).toJSPromise

        override def fromOption[T](v: Option[T], ifNone: String): Result[T] =
            v match {
                case Some(value) => Future.successful(value).toJSPromise
                case None => Future.failed(new Failed(ifNone)).toJSPromise
            }

        override def apply[A](g: => A): Result[A] = Result.apply(g)

        override def attempt[A](g: => A, retry: ReTryStrategy): Result[A] = Result.attempt(g, retry)

        override def Fail[T](msg: String): Result[T] = Future.failed(Failed(msg)).toJSPromise

        override def recover[A, B >: A](v: Result[A], f: String => Result[B]): Result[B] =
            v.recoverWith {
                case t => f(t.getMessage)
            }.toJSPromise

        // implementation for asynchronous R (i.e. Promise)
        override def foldUntil[T, R](
            items: Iterable[T], zero: R,
            cond: R => Result[Boolean],
            transform: (R, T) => Result[R]
        ): Result[(R, Boolean)] =
            asyncFoldUntil(items.iterator, zero, cond, transform)

        private def asyncFoldUntil[X, Y](
            it: Iterator[X], current: Y,
            cond: Y => Result[Boolean],
            transform: (Y, X) => Result[Y]
        ): Result[(Y, Boolean)] = {
            flatMap(cond(current), { stop: Boolean =>
                if (stop) {
                    Result((current, true))
                } else {
                    if (it.hasNext) {
                        flatMap(transform(current, it.next()), { next: Y =>
                            asyncFoldUntil(it, next, cond, transform)
                        })
                    } else Result((current, false))
                }
            })
        }
    }

    object Result {

        private def TryApply[T](g: => T, tn: Int, retry: ReTryStrategy): Future[T] = {
            Future.apply(g).recoverWith {
                case t =>
                    if (retry.shellReTry(t, tn)) TryApply(g, tn + 1, retry)
                    else throw t
            }
        }

        def apply[T](g: => T): Result[T] = Future.apply(g).toJSPromise

        def attempt[T](g: => T, retry: ReTryStrategy): Result[T] = TryApply(g, 1, retry).toJSPromise

        def Ok(): Result[Unit] = Future.successful(()).toJSPromise

        def Ok[T](v: T): Result[T] = Future.successful(v).toJSPromise

        def Fail[T](msg: String): Result[T] = Future.failed(new Failed(msg)).toJSPromise

        def expect(p: Boolean, error: String): Result[Unit] = if (p) Ok() else Fail(error)

    }


    //
    // Collection
    //

    type Collection[T] = js.Array[T]

    object Collection {
        def apply[T](items: T*): Collection[T] = js.Array(items: _*)

        def empty[T: ClassTag]: Collection[T] = apply()

        def newBuilder[T: ClassTag]: scala.collection.mutable.Builder[T, Collection[T]] = {
            val builder = mutable.ArrayBuilder.make[T]()
            new scala.collection.mutable.Builder[T, Collection[T]] {

                override def +=(elem: T): this.type = {
                    builder += elem
                    this
                }

                override def clear(): Unit = builder.clear()

                override def result(): Collection[T] = {
                    builder.result().toJSArray
                }
            }
        }
    }

    def collectionToArray[T: ClassTag](v: Collection[T]): scala.Array[T] = v.toArray

    def collectionToSequence[T](v: Collection[T]): scala.collection.Seq[T] = v.toSeq

    def collectionFromArray[T](v: scala.Array[T]): Collection[T] = v.toJSArray

    def collectionFromSequence[T: ClassTag](v: scala.collection.Seq[T]): Collection[T] = v.toJSArray

    def collectionFromIterable[T: ClassTag](v: scala.collection.Iterable[T]): Collection[T] = v.toJSArray

    //
    // Bytes
    //

    type Bytes = js.typedarray.ArrayBuffer

    object Bytes {
        def empty: Bytes = new Bytes(0)
    }

    def concatBytes(buffers: Seq[Bytes]): Bytes = {
        val totalSize = buffers.map(_.byteLength).sum
        val result = new Uint8Array(totalSize)
        var index: Int = 0
        buffers.foreach { buffer =>
            result.set(new Uint8Array(buffer), index)
            index = index + buffer.byteLength
        }
        result.buffer
    }

    def asBytes(v: Array[Byte]): Bytes = byteArray2Int8Array(v).buffer

    def asByteArray(v: Bytes): Array[Byte] = new Int8Array(v).toArray

    def isEqualBytes(a: Bytes, b: Bytes): Boolean =
        util.Arrays.equals(
            new Int8Array(a).toArray,
            new Int8Array(b).toArray
        )

    implicit class BytesOps(value: Bytes) {

        def toUTF8: String = new String(asByteArray(value), StandardCharsets.UTF_8)

        def setAtIndex(i: Int, v: Byte): Bytes = {
            val arr = new Int8Array(value)
            arr.set(i, v)
            value
        }

        def setInt(pos: Int, v: Int): Bytes = {
            val arr = new Int32Array(value)
            arr.set(pos, v)
            value
        }

        def length: Int = value.byteLength
    }

    @inline def isEmptyBytes(v: Bytes): Boolean = v.byteLength == 0

    type Optional[+T] = js.UndefOr[T]

    @inline def optionalFromOption[T](o: Option[T]): Optional[T] = o.orUndefined

    @inline def optionFromOptional[T](o: Optional[T]): Option[T] = o.toOption

    type BigInt = scalajs.js.BigInt

    def big2long(big: BigInt): Long = big.toString.toLong

}

case class Failed(msg: String) extends NoStackTrace {
    override def getMessage: String = msg
}
