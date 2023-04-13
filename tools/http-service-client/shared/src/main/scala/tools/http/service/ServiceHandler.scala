package tools.http.service

import ru.sberbank.blockchain.cnft.commons.ROps.summonHasOps
import ru.sberbank.blockchain.cnft.commons._
import tools.http.service.annotations._
import utility.{Handler, Info}

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
class ServiceHandler[EndpointResult[+_]](
    executor: HttpRequestsExecutor[EndpointResult]
)(implicit val R: ROps[EndpointResult]) extends Handler[String, EndpointResult] {
    override def handle(
        functionInfo: Info,
        argumentsInfo: Seq[Info],
        argumentsEncoded: Seq[String]
    ): EndpointResult[String] = {
        R.fromOption(functionInfo.meta.find(_.isInstanceOf[HttpMethodAnnotation]), s"Unsupported method: ${functionInfo.name}")
            .flatMap {
                case HttpGet(path) =>
                    val parameters = argumentsInfo
                        .zip(argumentsEncoded)
                        .filterNot(_._1.meta.exists(_.isInstanceOf[FromHttpHeader]))
                        .map { case (info, value) => NamedValue(info.name,
                            if (value.startsWith("\"") ) value.replaceAll("^\"|\"$", "") else value
                        )
                        }

                    val headers = extractHeaders(functionInfo, argumentsInfo, argumentsEncoded)
                    val url = executor.buildURL(path, collectionFromSequence(parameters))
                    executor.get(url, collectionFromSequence(headers))

                case HttpPost(path) =>
                    val url = executor.buildURL(path, Collection.empty)
                    val body =
                        if (argumentsEncoded.length == 1) {
                            argumentsEncoded.head
                        } else {
                            upickle.default.write(argumentsEncoded)
                        }

                    val headers = extractHeaders(functionInfo, argumentsInfo, argumentsEncoded)
                    executor.post(url, body, collectionFromSequence(headers))

                case default => R.Fail(s"Unsupported HTTP method $default")
            }
    }

    private def extractHeaders(
        functionInfo: Info,
        argumentsInfo: Seq[Info],
        argumentsEncoded: Seq[String]
    ) =
        headersFromFunction(functionInfo) ++ headersFromArgs(argumentsInfo, argumentsEncoded)


    private def headersFromFunction(functionInfo: Info) =
        functionInfo.meta.collect {
            case HttpHeaderValue(k, v) => NamedValue(k, v)
        }

    private def headersFromArgs(argumentsInfo: Seq[Info], argumentsEncoded: Seq[String]) =
        argumentsInfo
            .zip(argumentsEncoded)
            .flatMap { case (info, value) =>
                info.meta.collect {
                    case FromHttpHeader(k) => NamedValue(k, value)
                }
            }
}
