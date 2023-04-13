package tools.http.service

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import ru.sberbank.blockchain.cnft.commons
import ru.sberbank.blockchain.cnft.commons.{Collection, Result, collectionFromArray, collectionToArray}
import utility.{Decoder, Encoder, ProxyUtility}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.Thenable.Implicits._
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
 * @author Alexey Polubelov
 */
@JSExportTopLevel("HttpService")
object HttpService {

    implicit def CollectionRW[T: ClassTag : upickle.default.ReadWriter]: upickle.default.ReadWriter[Collection[T]] =
        upickle.default.readwriter[Array[T]].bimap(collectionToArray, collectionFromArray)

    implicit def upickleEncoder[T: upickle.default.Writer]: Encoder[T, String] =
        (value: T) => upickle.default.write(value)

    implicit def upickleDecoder[T: upickle.default.Reader]: Decoder[T, String] =
        (value: String) => upickle.default.read(value)

    implicit val BigIntDecoder = new Decoder[commons.BigInt, String] {
        override def decode(encoded: String): commons.BigInt = {
            scalajs.js.BigInt(encoded)
        }
    }

    implicit val BigIntEncoder = new Encoder[commons.BigInt, String] {
        override def encode(value: commons.BigInt): String = {
            value.toString()
        }
    }

    implicit val HttpServiceFactory: HttpServiceFactory[Result] = new HttpServiceFactory[Result] {

        override def newService[T: U](url: String): T = createService(url)

        override def newService[T: U](executor: HttpRequestsExecutor[Result]): T = createService(executor)

        override def defaultExecutor(url: String): HttpRequestsExecutor[Result] = HttpService.defaultExecutor(url)
    }

    def createService[T](baseURL: String)(implicit utility: ProxyUtility[T, Result, String]): T =
        utility.proxy(new ServiceHandler[Result](defaultExecutor(baseURL)))

    def createService[T](executor: HttpRequestsExecutor[Result])(implicit utility: ProxyUtility[T, Result, String]): T =
        utility.proxy(new ServiceHandler[Result](executor))

    @JSExport()
    def defaultExecutor(baseURL: String): HttpRequestsExecutor[Result] = new HttpRequestsExecutor[Result] {
        private def nv2tuple = (nv: NamedValue) => nv.name -> nv.value

        @JSExport()
        override def get(url: String, headers: Collection[NamedValue]): Result[String] =
            Ajax.get(url, headers = headers.map(nv2tuple).toMap).flatMap(handleResponse(url, _)).toJSPromise

        @JSExport()
        override def post(url: String, body: String, headers: Collection[NamedValue]): Result[String] =
            Ajax.post(url, data = body, headers = headers.map(nv2tuple).toMap).flatMap(handleResponse(url, _)).toJSPromise

        @JSExport()
        override def buildURL(path: String, parameters: Collection[NamedValue]): String = {
            val query =
                if (parameters.nonEmpty) {
                    parameters.map { parameter =>
                        s"${parameter.name}=${uriEncode(parameter.value)}"
                    }.mkString("?", "&", "")
                } else ""

            s"$baseURL$path$query"
        }
    }

    def uriEncode(value: String): String =
        scalajs.js.URIUtils.encodeURI(value).replace("+", "%2B")

    private def handleResponse(url: String, response: dom.XMLHttpRequest): Result[String] = {
        val responseText = response.responseText
        if (response.status == 200) {
            Result.Ok(responseText)
        } else {
            Result.Fail(s"Call to [$url] failed (${response.status}): $responseText")
        }
    }
}
