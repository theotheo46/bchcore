package tools.http.service

import utility.ProxyUtility

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
trait HttpServiceFactory[R[+_]] {

    type U[T] = ProxyUtility[T, R, String]

    def newService[T: U](url: String): T

    def newService[T: U](executor: HttpRequestsExecutor[R]): T

    def defaultExecutor(url: String): HttpRequestsExecutor[R]
}