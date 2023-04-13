package tools.http.service

import ru.sberbank.blockchain.cnft.commons.Collection

import scala.language.higherKinds
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}


/**
 * @author Alexey Polubelov
 */
trait HttpRequestsExecutor[R[_]] {

    def buildURL(base: String, parameters: Collection[NamedValue]): String

    def get(url: String, headers: Collection[NamedValue]): R[String]

    def post(url: String, body: String, headers: Collection[NamedValue]): R[String]
}

@JSExportAll
@JSExportTopLevel("NamedValue")
case class NamedValue(
    name: String,
    value: String
)