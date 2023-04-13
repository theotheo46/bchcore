package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.commons.Result

/**
 * @author Alexey Polubelov
 */
package object dsl {

    private def Fail(msg: String) = throw new java.lang.AssertionError(msg)

    implicit class RExt[+T](value: Result[T]) {

        @inline def orFail(msg: String): T =
            value match {
                case Right(value) => value
                case Left(error) => Fail(s"$msg: $error")
            }

        @inline def expectFail: String =
            value match {
                case Right(_) => Fail("Expected fail")
                case Left(error) => error
            }

    }
}
