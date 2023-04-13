package org.enterprisedlt.fabric.client

import ru.sberbank.blockchain.cnft.commons.{ROps, ReTryStrategy, Result}

import scala.util.{Failure, Success, Try}

/**
 * @author Alexey Polubelov
 */
sealed trait ContractResult[+T] {

    def map[B](f: T => B): ContractResult[B]

    def flatMap[B](f: T => ContractResult[B]): ContractResult[B]

    def toResult[X](implicit convert: (Long, String, T) => X): Result[X]

    def isSuccessful: Boolean
}

case class SuccessTx[+T](
    blockNumber: Long,
    txId: String,
    value: T
) extends ContractResult[T] {

    override def map[B](f: T => B): ContractResult[B] = SuccessTx(blockNumber, txId, f(value))

    override def flatMap[B](f: T => ContractResult[B]): ContractResult[B] =
        f(value) match {
            case SuccessTx(_, _, value) => SuccessTx(blockNumber, txId, value) // NOTE: We always keep original ID
            case err: FailedTx => err
        }

    override def toResult[X](implicit convert: (Long, String, T) => X): Result[X] = Result(convert(blockNumber, txId, value))

    override def isSuccessful: Boolean = true
}

case class FailedTx(msg: String) extends ContractResult[Nothing] {

    override def map[B](f: Nothing => B): ContractResult[B] = this

    override def flatMap[B](f: Nothing => ContractResult[B]): ContractResult[B] = this

    override def toResult[X](implicit convert: (Long, String, Nothing) => X): Result[X] = Result.Fail(msg)

    override def isSuccessful: Boolean = false
}

object ContractResult {

    implicit val ContractResultOps: ROps[ContractResult] = new ROps[ContractResult] {

        override def map[A, B](v: ContractResult[A], f: A => B): ContractResult[B] =
            v.map(f)

        override def flatMap[A, B](v: ContractResult[A], f: A => ContractResult[B]): ContractResult[B] =
            v.flatMap(f)

        override def fromOption[T](v: Option[T], ifNone: String): ContractResult[T] =
            v match {
                case Some(value) => SuccessTx(-1L, "-", value)
                case None => FailedTx(ifNone)
            }

        override def apply[A](g: => A): ContractResult[A] =
            Try(g) match {
                case Failure(exception) => FailedTx(exception.getMessage)
                case Success(value) => SuccessTx(-1L, "-", value)
            }

        override def attempt[A](g: => A, reTryStrategy: ReTryStrategy): ContractResult[A] = apply(g)

        override def Fail[A](msg: String): ContractResult[A] = FailedTx(msg)

        override def recover[A, B >: A](v: ContractResult[A], f: String => ContractResult[B]): ContractResult[B] = ??? // TODO: implement

        override def foldUntil[T, R](
            items: Iterable[T], zero: R,
            cond: R => ContractResult[Boolean],
            transform: (R, T) => ContractResult[R]
        ): ContractResult[(R, Boolean)] = ???

        //TODO: these two must be defined in separate type class

        override def logInfo[A](v: ContractResult[A], f: A => String): ContractResult[A] = ???

        override def logDebug[A](v: ContractResult[A], f: A => String): ContractResult[A] = ???

    }
}
