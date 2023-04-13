package ru.sberbank.blockchain.cnft.common

import ru.sberbank.blockchain.cnft.commons
import ru.sberbank.blockchain.cnft.commons.{Base58, ROps, asByteArray}

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.language.higherKinds
import scala.reflect.ClassTag

/**
 * @author Alexey Polubelov
 */
package object types {
    type Result[+T] = commons.Result[T]
    val Result: commons.Result.type = commons.Result

    implicit val ResultOps: ROps[Result] = commons.ResultOps

    type Collection[T] = commons.Collection[T]
    val Collection: commons.Collection.type = commons.Collection

    @inline def collectionFromSequence[T: ClassTag](v: scala.collection.Seq[T]): Collection[T] = commons.collectionFromSequence[T](v)

    @inline def collectionFromIterable[T: ClassTag](v: scala.collection.Iterable[T]): Collection[T] = commons.collectionFromIterable[T](v)

    implicit class CollectionR_Ops[R[+_], X : ClassTag](items: Collection[X])(implicit R: ROps[R]) {

        def mapR[Y: ClassTag](f: X => R[Y]): R[Collection[Y]] =
        // fold until false (i.e. non stop) to Seq[Y]
            R.map(
                R.foldUntil[X, Seq[Y]](
                    items, Seq.empty[Y],
                    _ => R(false), // no stop condition
                    { case (result, item) =>
                        R.map(f(item), { v: Y =>
                            result :+ v
                        })
                    }
                ), { x: (Seq[Y], Boolean) =>
                    collectionFromSequence(x._1)
                }
            )

        def findR(cond: X => R[Boolean]): R[Option[X]] =
            R.map(
                R.foldUntil[X, X](
                    items, null.asInstanceOf[X], {
                        case null => R(false)
                        case v => cond(v)
                    }, { case (_, item) =>
                        R(item)
                    }
                ), { x: (X, Boolean) =>
                    val (v, found) = x
                    if (found) Some(v) else None
                }
            )

        def filterR(cond: X => R[Boolean]): R[Collection[X]] =
            R.map[(Seq[X], Boolean), Collection[X]](
                R.foldUntil[X, Seq[X]](
                    items,
                    zero = Seq.empty[X],
                    cond = _ => R(false),
                    transform = { case (result, item) =>
                        R.map[Boolean, Seq[X]](
                            cond(item),
                            { if (_) result :+ item else result }
                        )
                    }
                ),
                { case (filtered, _) => collectionFromSequence(filtered) }
            )

    }

    type Bytes = commons.Bytes
    val Bytes: commons.Bytes.type = commons.Bytes

    @inline def concatBytes(buffers: Seq[Bytes]): Bytes = commons.concatBytes(buffers)

    @inline def asBytes(v: Array[Byte]): Bytes = commons.asBytes(v)

    @inline def fromB64(v: String): Bytes = asBytes(Base64.getDecoder.decode(v))


    implicit class BytesOps(value: Bytes) {
        def toB64: String = new String(Base64.getEncoder.encode(asByteArray(value)), StandardCharsets.UTF_8)

        def toB58: String = Base58.encode(asByteArray(value))

        def toUTF8: String = new String(asByteArray(value), StandardCharsets.UTF_8)

        def =?=(other: Bytes): Boolean = commons.isEqualBytes(value, other)

        def =!=(other: Bytes): Boolean = !commons.isEqualBytes(value, other)

    }

    type Optional[+T] = commons.Optional[T]

    implicit class OptionalOps[+T](value: Optional[T]) {
        def toR[R[+_] : ROps](ifNone: String): R[T] =
            value
                .map(x => implicitly[ROps[R]].apply(x))
                .getOrElse(implicitly[ROps[R]].Fail(ifNone))
    }

    type BigInt = commons.BigInt

    implicit class BigIntOps(big: BigInt) {
        def toLong: Long = commons.big2long(big)
    }


}
