package org.enterprisedlt.fabric.client

import org.enterprisedlt.spec.{ContractOperation, OperationType, Transient}
import org.hyperledger.fabric.protos.peer.Chaincode
import org.hyperledger.fabric.protos.peer.TransactionPackage.TxValidationCode
import org.hyperledger.fabric.sdk.Channel.DiscoveryOptions.createDiscoveryOptions
import org.hyperledger.fabric.sdk._
import org.hyperledger.fabric.sdk.exception.TransactionEventException
import org.slf4j.LoggerFactory
import ru.sberbank.blockchain.cnft.commons.Result
import utility.{Handler, Info, ProxyUtility}

import java.util.concurrent.ExecutionException
import scala.collection.JavaConverters._
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.util.{Failure, Success, Try}

/**
 * @author Alexey Polubelov
 * @author Maxim Fedin
 */
class FabricChainCode(
    fabricClient: FabricClient,
    fabricChannel: Channel,
    fabricChainCodeID: Chaincode.ChaincodeID,
    bootstrapOrderers: java.util.Collection[Orderer],
    discoveryForEndorsement: Boolean,
    discoveryForOrdering: Boolean
) {
    type TransactionEvent = BlockEvent#TransactionEvent

    private val logger = LoggerFactory.getLogger(this.getClass)

    private val ReTryValidationCodes =
        Set(
            TxValidationCode.MVCC_READ_CONFLICT_VALUE,
            TxValidationCode.PHANTOM_READ_CONFLICT_VALUE
        )

    private val ReTryTimes = 5

    def rawQuery(function: String, args: Array[Array[Byte]], transient: Map[String, Array[Byte]] = Map.empty): ContractResult[Array[Byte]] = {
        val request = fabricClient.newQueryProposalRequest()
        request.setChaincodeName(fabricChainCodeID.getName)
        request.setFcn(function)
        request.setArgs(args: _*)
        if (transient.nonEmpty) {
            request.setTransientMap(transient.asJava)
        }
        val responses = fabricChannel.queryByChaincode(request).asScala
        val responsesByStatus = responses.groupBy(_.getStatus)
        val failed = responsesByStatus.getOrElse(ChaincodeResponse.Status.FAILURE, List.empty)
        val succeeded = responsesByStatus.getOrElse(ChaincodeResponse.Status.SUCCESS, List.empty)
        if (failed.nonEmpty && succeeded.isEmpty) {
            FailedTx(extractErrorMessage(failed.head))
        } else {
            val succeddedConsistencySet = SDKUtils.getProposalConsistencySets(succeeded.asJavaCollection)
            if (succeddedConsistencySet.size() != 1) {
                FailedTx(s"Got inconsistent proposal responses [${succeddedConsistencySet.size}]")
            } else {
                val response = succeeded.head
                val txId = response.getTransactionID
                //println(s"Executed $function - txId: $txId")
                SuccessTx(-1, txId, extractPayload(response))
            }
        }
    }

    def rawQueryDefinitions(): Result[Array[Byte]] = {
        val request = fabricClient.newQueryProposalRequestDefiniton()
        request.setChaincodeName(fabricChainCodeID.getName)
        val responses = fabricChannel.lifecycleQueryChaincodeDefinition(request, fabricChannel.getPeers)
        val responsesByStatus = responses.groupBy(_.getStatus)
        val failed = responsesByStatus.getOrElse(ChaincodeResponse.Status.FAILURE, List.empty)
        val succeeded = responsesByStatus.getOrElse(ChaincodeResponse.Status.SUCCESS, List.empty)
        if (failed.nonEmpty && succeeded.isEmpty) {
            Left(extractErrorMessage(failed.head))
        } else {
            Right(extractPayload(succeeded.head))
        }
    }

    def rawInvoke(function: String, args: Array[Array[Byte]], transient: Map[String, Array[Byte]] = Map.empty): ContractResult[Array[Byte]] =
        Iterator.continually {
            Try {
                val request = fabricClient.newTransactionProposalRequest()
                request.setChaincodeName(fabricChainCodeID.getName)
                request.setFcn(function)
                request.setArgs(args: _*)
                if (transient.nonEmpty) {
                    request.setTransientMap(transient.asJava)
                }
                val responses = if (discoveryForEndorsement) {
                    fabricChannel.sendTransactionProposalToEndorsers(
                        request,
                        createDiscoveryOptions()
                            .setEndorsementSelector(ServiceDiscovery.EndorsementSelector.ENDORSEMENT_SELECTION_RANDOM)
                            .setForceDiscovery(true)
                            // indicate to sdk that we will handle result sets by our self
                            .setInspectResults(true)
                    )
                } else fabricChannel.sendTransactionProposal(request)
                val responsesConsistencySets = SDKUtils.getProposalConsistencySets(responses)
                if (responsesConsistencySets.size() != 1) {
                    val responsesByStatus = responses.asScala.groupBy(_.getStatus)
                    val failed = responsesByStatus.getOrElse(ChaincodeResponse.Status.FAILURE, List.empty)

                    if (failed.nonEmpty) throw new Exception(extractErrorMessage(failed.head))
                    else throw new Exception(s"Got inconsistent proposal responses [${responsesConsistencySets.size}]")
                } else {
                    val orderersToCommit =
                        if (discoveryForOrdering) fabricChannel.getOrderers
                        else bootstrapOrderers

                    val response = responses.iterator().next()
                    val txId = response.getTransactionID
                    val result =
                        fabricChannel
                            .sendTransaction(responses, orderersToCommit, fabricClient.getUserContext)
                            .get()

                    SuccessTx(result.getBlockEvent.getBlockNumber, txId, extractPayload(response))
                }
            } match {
                case Failure(exception) =>
                    logger.warn(s"Invoke failed [$function] with exception: ${exception.getMessage}")
                    val ex = unwrapExecutionException(exception)
                    (
                        FailedTx(ex.getMessage),
                        validationCode(ex).exists(code => ReTryValidationCodes.contains(code))
                    )

                case Success(value) => (value, false)
            }

        }.zipWithIndex.find { case ((tx, recoverable), tryNumber) =>
            tx.isSuccessful || !recoverable || tryNumber >= ReTryTimes
        }.map(_._1._1).getOrElse(FailedTx("Failed to execute transaction [CORE BUG]"))


    private def unwrapExecutionException(ex: Throwable): Throwable =
        ex match {
            case e: ExecutionException => e.getCause
            case other => other
        }

    private def validationCode(ex: Throwable): Option[Int] =
        ex match {
            case txEvent: TransactionEventException =>
                Some(txEvent.getTransactionEvent.getValidationCode.toInt)
            case _ => None
        }

    class ResultOverwrite[TXEvent, T](value: T) extends java.util.function.Function[TXEvent, T]() {
        override def apply(x: TXEvent): T = value
    }

    private def extractPayload(response: ProposalResponse): Array[Byte] =
        Option(response.getProposalResponse)
            .flatMap(r => Option(r.getResponse))
            .flatMap(r => Option(r.getPayload))
            .flatMap(r => Option(r.toByteArray))
            .getOrElse(Array.empty)

    private def extractErrorMessage(response: ProposalResponse): String =
        Option(response.getProposalResponse)
            .flatMap(r => Option(r.getResponse))
            .flatMap(r => Option(r.getMessage))
            .getOrElse("General error occurred")

    /**
     * An instance of proxy is always new, not cached,
     * the maximum number of instances can be produced is only int max value
     * */
    def as[T](implicit utility: ProxyUtility[T, ContractResult, Array[Byte]]): T = {
        utility.proxy(
            new Handler[Array[Byte], ContractResult] {
                override def handle(
                    function: Info, argumentsInfo: Seq[Info], argumentsEncoded: Seq[Array[Byte]]
                ): ContractResult[Array[Byte]] =
                    function.meta.find(_.isInstanceOf[ContractOperation]) match {
                        case Some(operation) =>
                            operation.asInstanceOf[ContractOperation].value match {
                                case OperationType.Query =>
                                    //TODO use fold to produce arguments and transient - see parseArgs above
                                    val transient = argumentsInfo.zip(argumentsEncoded).flatMap {
                                        case (info, argument) =>
                                            info.meta.find(_.isInstanceOf[Transient]).map(_ => (info.name, argument))
                                    }.toMap
                                    rawQuery(function.name, argumentsEncoded.toArray, transient)

                                case OperationType.Invoke =>
                                    //TODO use fold to produce arguments and transient - see parseArgs above
                                    val transient = argumentsInfo.zip(argumentsEncoded).flatMap {
                                        case (info, argument) =>
                                            info.meta.find(_.isInstanceOf[Transient]).map(_ => (info.name, argument))
                                    }.toMap
                                    rawInvoke(function.name, argumentsEncoded.toArray, transient)

                                case other =>
                                    FailedTx(s"${
                                        function.name
                                    } has annotation of type ${
                                        other.getClass.getCanonicalName
                                    } which is not supported")
                            }

                        case _ => FailedTx(s"${
                            function.name
                        } has no annotation  of type ${
                            classOf[ContractOperation].getCanonicalName
                        }")
                    }
            }
        )
    }

    //
    //    def parseArgs(method: Method, args: Array[AnyRef]): (Array[AnyRef], Map[String, AnyRef]) =
    //        Option(method.getParameters)
    //            .getOrElse(Array.empty)
    //            .zip(args)
    //            .foldLeft((Array.empty[AnyRef], Map.empty[String, AnyRef])) {
    //                case ((arguments, transient), (parameter, value)) =>
    //                    if (parameter.isAnnotationPresent(classOf[Transient]))
    //                        (arguments, transient + (parameter.getName -> value)) // put transient to transient map
    //                    else
    //                        (arguments :+ value, transient) // put non transient to parameters
    //            }

}
