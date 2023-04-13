package org.enterprisedlt

import scala.util.Try

/**
 * @author Alexey Polubelov
 */
package object spec {

    type ContractResult[+V] = Either[String, V]

    implicit def Try2ContractResult[V]: Try[V] => ContractResult[V] = {
        case scala.util.Success(x) => Right(x)
        case scala.util.Failure(failure) => Left(failure.getMessage)
    }
}
