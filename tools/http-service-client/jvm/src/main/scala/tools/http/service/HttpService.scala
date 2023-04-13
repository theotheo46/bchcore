package tools.http.service

import org.apache.http.client.methods.{HttpGet, HttpPost, HttpUriRequest}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.util.EntityUtils
import ru.sberbank.blockchain.cnft.commons
import ru.sberbank.blockchain.cnft.commons.{Collection, LoggingSupport, Result, BigInt}
import utility.{Decoder, Encoder, ProxyUtility}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * @author Alexey Polubelov
 */
object HttpService extends LoggingSupport {
    // TODO: expose this to configuration
    private val MaxConcurrentConnections = 1000

    implicit def upickleEncoder[T: upickle.default.Writer]: Encoder[T, String] =
        (value: T) => upickle.default.write(value)

    implicit def upickleDecoder[T: upickle.default.Reader]: Decoder[T, String] =
        (value: String) => upickle.default.read(value)

    implicit val BigIntDecoder = new Decoder[commons.BigInt, String] {
        override def decode(encoded: String): commons.BigInt = {
            new BigInt(encoded)
        }
    }

    implicit val BigIntEncoder = new Encoder[commons.BigInt, String] {
        override def encode(value: commons.BigInt): String = {
            value.toString
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

    def defaultExecutor(baseURL: String): HttpRequestsExecutor[Result] = new HttpRequestsExecutor[Result] {
        private val httpClient =
            HttpClients.custom().setConnectionManager(newConnectionManager).build()

        private def newConnectionManager = {
            val m = new PoolingHttpClientConnectionManager()
            m.setMaxTotal(MaxConcurrentConnections)
            m.setDefaultMaxPerRoute(MaxConcurrentConnections)
            m
        }

        override def get(url: String, headers: Collection[NamedValue]): Result[String] = {
            val request = new HttpGet(url)
            executeRequest(url, request, headers)
        }

        override def post(url: String, body: String, headers: Collection[NamedValue]): Result[String] = {
            val request = new HttpPost(url)
            request.setEntity(new StringEntity(body, "UTF-8"))
            executeRequest(url, request, headers)
        }

        private def executeRequest(url: String, request: HttpUriRequest, headers: Collection[NamedValue]) = {
            headers.foreach { header =>
                request.setHeader(header.name, header.value)
            }
            for {
                response <- Result(httpClient.execute(request))
                result <- Result {
                    val e = response.getEntity
                    val responseText = new String(e.getContent.readAllBytes(), StandardCharsets.UTF_8)
                    EntityUtils.consume(e)
                    val code = response.getStatusLine.getStatusCode
                    if (code == 200)
                        Result.Ok(responseText)
                    else
                        Result.Fail(s"Call to [$url] failed ($code): $responseText")

                }.joinRight

                _ = response.close()
            } yield result
        }

        override def buildURL(path: String, parameters: Collection[NamedValue]): String = {
            val query =
                if (parameters.nonEmpty) {
                    parameters.map { parameter =>
                        s"${parameter.name}=${uriEncode(parameter.value)}"
                    }.mkString("?", "&", "")
                } else ""

            s"$baseURL$path$query"
        }

        private def uriEncode(value: String): String = URLEncoder.encode(value, "UTF-8")
    }
}
