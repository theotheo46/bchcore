package ru.sberbank.blockchain.cnft.wallet.walletmodel

import ru.sberbank.blockchain.cnft.common.types.Bytes
import ru.sberbank.blockchain.cnft.commons.asByteArray
import ru.sberbank.blockchain.cnft.model.{BurnRequest, BurnResponse, DealRejectedBySmartContract, DealRequest, IssueTokenRequest, OperationStatus, PendingBurn, PendingDeal, PendingIssue, RegulatorTransferRequest, TokenRequest, TransferProposal, OperationData => OperData}
import scalapb.GeneratedMessageCompanion

import scala.util.Try

/**
 * @author Alexey Polubelov
 */

object OperationData {

    val dataSerializers: Map[String, GeneratedMessageCompanion[_ <: OperData]] = Map(
        OperationStatus.IssueDone -> IssueTokenRequest,
        OperationStatus.IssueRejectedByRegulation -> PendingIssue,
        OperationStatus.IssuePendingRegulation -> PendingIssue,

        OperationStatus.DealDone -> DealRequest,
        OperationStatus.DealDoneByRegulator -> RegulatorTransferRequest,
        OperationStatus.AcceptPending -> DealRequest,
        OperationStatus.DealPendingRegulation -> PendingDeal,
        OperationStatus.DealRejectedByRegulation -> PendingDeal,
        OperationStatus.DealRejectedBySmartContract -> DealRejectedBySmartContract,

        OperationStatus.BurnPendingRegulation -> PendingBurn,
        OperationStatus.BurnRejectedByRegulation -> PendingBurn,
        OperationStatus.BurnDone -> BurnResponse,
        OperationStatus.BurnDoneByRegulator -> BurnRequest,

        OperationStatus.TransferProposed -> TransferProposal,
        OperationStatus.TokenRequested -> TokenRequest
    )

    def from(typeId: String, bytes: Bytes): OperData = {
        typeId match {
            case OperationStatus.DealRejectedBySmartContract =>
                Try(DealRejectedBySmartContract.parseFrom(asByteArray(bytes)))
                    .toOption
                    .getOrElse(
                        DealRejectedBySmartContract(
                            DealRequest.parseFrom(asByteArray(bytes)),
                            ""
                        )
                    )
            case _ =>
                dataSerializers
                    .getOrElse(typeId, throw new Exception(s"Unknown type: $typeId"))
                    .parseFrom(asByteArray(bytes))
        }
    }
}