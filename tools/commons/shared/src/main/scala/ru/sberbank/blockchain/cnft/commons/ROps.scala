package ru.sberbank.blockchain.cnft.commons

import scala.language.{higherKinds, implicitConversions}

/**
 * @author Alexey Polubelov
 */
trait ROps[F[+_]] {

    def map[A, B](v: F[A], f: A => B): F[B]

    def flatMap[A, B](v: F[A], f: A => F[B]): F[B]

    def fromOption[T](v: Option[T], ifNone: String): F[T]

    // native implementation for R
    def foldUntil[T, R](items: Iterable[T], zero: R, cond: R => F[Boolean], transform: (R, T) => F[R]): F[(R, Boolean)]

    //

    def apply[A](g: => A): F[A]

    def attempt[A](g: => A, reTryStrategy: ReTryStrategy): F[A]

    def Fail[A](msg: String): F[A]

    def expect(p: Boolean, error: String): F[Unit] = if (p) apply(()) else Fail(error)

    def recover[A, B >: A](v: F[A], f: String => F[B]): F[B]

    // TODO: remove these log methods:

    def logInfo[A](v: F[A], f: A => String): F[A]

    def logDebug[A](v: F[A], f: A => String): F[A]

}

trait ReTryStrategy {
    def shellReTry(t: Throwable, tryNumber: Int): Boolean
}

object NoRetry extends ReTryStrategy {
    override def shellReTry(t: Throwable, tryNumber: Int): Boolean = false
}

case class TryTimes(n: Int) extends ReTryStrategy {
    override def shellReTry(t: Throwable, tryNumber: Int): Boolean = tryNumber != n
}

object ROps {

    implicit def summonHasOps[X, F[+_] : ROps](v: F[X]): HasOps[F, X] = new HasOps[F, X] {

        override def map[B](f: X => B): F[B] = implicitly[ROps[F]].map(v, f)

        override def flatMap[B](f: X => F[B]): F[B] = implicitly[ROps[F]].flatMap(v, f)

        override def recover[B >: X](f: String => F[B]): F[B] = implicitly[ROps[F]].recover(v, f)

        override def patchR(cond: X => Boolean)(f: X => F[X]): F[X] =
            flatMap { item =>
                if (cond(item)) f(item)
                else implicitly[ROps[F]].apply(item)
            }

        //

        override def logInfo(f: X => String): F[X] = implicitly[ROps[F]].logInfo(v, f)

        override def logDebug(f: X => String): F[X] = implicitly[ROps[F]].logDebug(v, f)
    }

    implicit class IterableR_Ops[R[+_], X](items: Iterable[X])(implicit R: ROps[R]) {

        def mapR[Y](f: X => R[Y]): R[Iterable[Y]] =
        // fold until false (i.e. non stop) to Seq[Y]
            R.foldUntil[X, Seq[Y]](
                items, Seq.empty[Y],
                _ => R(false), // no stop condition
                { case (result, item) =>
                    f(item).map { v =>
                        result :+ v
                    }
                }
            ).map(_._1)

        def findR(cond: X => R[Boolean]): R[Option[X]] =
            R.foldUntil[X, X](
                items, null.asInstanceOf[X], {
                    case null => R(false)
                    case v => cond(v)
                }, { case (_, item) =>
                    R(item)
                }
            ).map { case (v, found) =>
                if (found) Some(v) else None
            }

        def filterR(cond: X => R[Boolean]): R[Iterable[X]] =
            R.foldUntil[X, Seq[X]](
                items, Seq.empty[X],
                _ => R(false),
                { case (result, item) =>
                    cond(item).map { filtered =>
                        if (filtered) result :+ item
                        else result
                    }
                }
            ).map(_._1)

        def foldLeftR[Y](zero: Y)(transform: (Y, X) => R[Y]): R[Y] =
            R.foldUntil[X, Y](
                items, zero,
                _ => R(false), // no stop condition
                transform
            ).map(_._1)
    }

}

trait HasOps[F[_], A] {

    def map[B](f: A => B): F[B]

    def flatMap[B](f: A => F[B]): F[B]

    def recover[B >: A](f: String => F[B]): F[B]

    def patch(cond: A => Boolean)(f: A => A): F[A] =
        map { item =>
            if (cond(item)) f(item)
            else item
        }

    def patchR(cond: A => Boolean)(f: A => F[A]): F[A]

    // ----------------

    def logInfo(f: A => String): F[A]

    def logDebug(f: A => String): F[A]

}
