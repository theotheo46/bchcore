package org.enterprisedlt.fabric.contract

import org.enterprisedlt.fabric.contract.exception.ResolveRoleFunctionException
import org.enterprisedlt.spec.{ContractInit, ContractOperation, Restrict}
import org.hyperledger.fabric.metrics.Metrics
import org.hyperledger.fabric.shim.Chaincode.Response
import org.hyperledger.fabric.shim.Chaincode.Response.Status
import org.hyperledger.fabric.shim._
import org.slf4j.{Logger, LoggerFactory}
import ru.sberbank.blockchain.cnft.commons.Result
import utility.{Route, RoutesUtility}

import java.io.{PrintWriter, StringWriter}
import java.nio.charset.StandardCharsets
import scala.collection.JavaConverters._

/**
 * @author Alexey Polubelov
 */
class FabricChainCode[T](
    service: T,
    codecs: ContractCodecs = ContractCodecs(),
    simpleTypesPartitionName: String = "SIMPLE",
    resolveRole: () => String = () => throw new ResolveRoleFunctionException
)(implicit
    utility: RoutesUtility[T, Result, Array[Byte]]
) extends ChaincodeBase {
    type ChainCodeFunction = ChaincodeStub => Response

    private val logger: Logger = LoggerFactory.getLogger(this.getClass)
    private val routes = utility.routes(service)

    private val InitFunction: Option[ChainCodeFunction] = {
        routes
            .filter(_.functionInfo.meta.exists(_.isInstanceOf[ContractInit]))
            .map(chainCodeFunctionTemplate).headOption
    }

    override def init(api: ChaincodeStub): Response =
        try {
            InitFunction
                .map(_ (api))
                .getOrElse(mkSuccessResponse())
        } catch {
            case t: Throwable =>
                logger.error("Got exception during init", t)
                throw t
        }

    private val InvokeFunctions: Map[String, ChainCodeFunction] =
        routes
            .filter(_.functionInfo.meta.exists(_.isInstanceOf[ContractOperation]))
            .map(r =>
                r.functionInfo.name -> chainCodeFunctionTemplate(r)
            ).toMap

    override def invoke(api: ChaincodeStub): Response =
        try {
            InvokeFunctions
                .get(api.getFunction).map(_ (api))
                .getOrElse {
                    val msg = s"Unknown function ${api.getFunction}"
                    logger.debug(msg)
                    mkErrorResponse(msg)
                }
        } catch {
            case t: Throwable =>
                logger.error("Got exception during invoke", t)
                throw t
        }

    //
    //
    //

    private[this] def chainCodeFunctionTemplate(m: Route[Array[Byte], Result]): ChainCodeFunction = (api: ChaincodeStub) =>
        try {
            logger.debug(s"Executing ${m.functionInfo.name}")
            OperationContext.set(api, codecs, simpleTypesPartitionName) // has to be set _before_ call to resolveRole
            Either
                .cond(
                    m.functionInfo.meta
                        .find(_.isInstanceOf[Restrict])
                        .map(_.asInstanceOf[Restrict].value)
                        .forall(_.contains(resolveRole())),
                    (), "Access denied"
                )
                .flatMap { _ =>
                    // TODO: transient support
                    Right(api.getArgs.asScala.tail.toArray)
                    //makeParameters(m.getParameters, api.getArgs.asScala.tail.toArray, api.getTransient)
                }
                .flatMap { parameters =>
                    val result = m.execute(parameters)
                    logger.debug(s"Execution of ${m.functionInfo.name} done")
                    result
                }
            match {
                case Right(v) => mkSuccessResponse(v)
                case Left(msg) => mkErrorResponse(msg)
            }
        } catch {
            case t: Throwable =>
                logger.error("Exception during contract operation", t)
                mkExceptionResponse(t)
        } finally {
            OperationContext.clear()
        }


    //    private def foldLeftEither[X, L, R](elements: Iterable[X])(z: R)(f: (R, X) => Either[L, R]): Either[L, R] =
    //        elements.foldLeft(Right(z).asInstanceOf[Either[L, R]]) { case (r, x) => r.flatMap(v => f(v, x)) }

    private def mkSuccessResponse(): Response = new Chaincode.Response(Status.SUCCESS, null, null)

    private def mkSuccessResponse(v: Array[Byte]): Response = new Chaincode.Response(Status.SUCCESS, null, v)

    private def mkErrorResponse(v: String): Response = new Chaincode.Response(Status.INTERNAL_SERVER_ERROR, v, null)

    //    private def mkErrorResponse(v: Array[Byte]): Response = new Chaincode.Response(Status.INTERNAL_SERVER_ERROR, null, v)

    private def mkExceptionResponse(throwable: Throwable): Response =
        new Chaincode.Response(Status.INTERNAL_SERVER_ERROR, throwable.getMessage,
            stackTraceString(throwable).getBytes(StandardCharsets.UTF_8))

    private def stackTraceString(throwable: Throwable): String = Option(throwable).map { throwable =>
        val buffer = new StringWriter
        throwable.printStackTrace(new PrintWriter(buffer))
        buffer.toString
    } getOrElse ""

    //
    def startAsServer(chaincodeProperties: ChaincodeServerProperties, args: Array[String]): Unit = {
        initializeLogging()
        processEnvironmentOptions()
        processCommandLineOptions(args)
        validateOptions()
        val cfg = getChaincodeConfig // it does inner mutation ...
        Metrics.initialize(cfg)

        val server = new NettyChaincodeServer(this, chaincodeProperties)
        logger.info(s"Starting chaincode server at port ${chaincodeProperties.getPortChaincodeServer} ...")
        server.start()
    }

}
