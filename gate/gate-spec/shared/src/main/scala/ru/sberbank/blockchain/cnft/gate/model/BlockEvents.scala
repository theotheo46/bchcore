package ru.sberbank.blockchain.cnft.gate.model

import ru.sberbank.blockchain.cnft.model.{BurnResponse, DataFeed, DealRequest, IssueTokenRequest, MessageRequest, Offer, PendingAccept, PendingBurn, PendingDeal, PendingIssue, Profile, ProfileTokens, RegulatorBurnRequest, RegulatorTransferRequest, SCRegulationRequest, SignedEndorsement, SignedPublicEndorsement, SmartContract, SmartContractRegulation, SmartContractState, TokenChangeResponse, TokenFreezeRequest, TokenMergeResponse, TokenType}
import upickle.default

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}


/**
 * @author Maxim Fedin
 */
@JSExportAll
@JSExportTopLevel("BlockTransactions")
case class BlockEvents(
    membersRegistered: Array[BlockEvent[String]],

    dataFeedRegistered: Array[BlockEvent[DataFeed]],
    tokenTypesRegistered: Array[BlockEvent[TokenType]],
    tokensIssued: Array[BlockEvent[IssueTokenRequest]],
    tokensExchanged: Array[BlockEvent[DealRequest]],
    tokensBurned: Array[BlockEvent[BurnResponse]],
    tokensChanged: Array[BlockEvent[TokenChangeResponse]],
    tokensMerged: Array[BlockEvent[TokenMergeResponse]],
    tokenFrozen: Array[BlockEvent[TokenFreezeRequest]],
    regulatorBurned: Array[BlockEvent[RegulatorBurnRequest]],
    regulatorTransferred: Array[BlockEvent[RegulatorTransferRequest]],

    offersAdded: Array[BlockEvent[Offer]],
    offersClosed: Array[BlockEvent[String]],

    approveEndorsements: Array[BlockEvent[SignedEndorsement]],
    publicEndorsements: Array[BlockEvent[SignedPublicEndorsement]],
    revokedEndorsements: Array[BlockEvent[SignedPublicEndorsement]],

    pendingAccepts: Array[BlockEvent[PendingAccept]],
    pendingIssues: Array[BlockEvent[PendingIssue]],
    pendingDeals: Array[BlockEvent[PendingDeal]],
    pendingBurns: Array[BlockEvent[PendingBurn]],

    smartContractsAdded: Array[BlockEvent[SmartContract]],
    smartContractsRejected: Array[BlockEvent[SCRegulationRequest]],
    smartContractsStateUpdated: Array[BlockEvent[SmartContractState]],
    smartContractsRegulationUpdated: Array[BlockEvent[SmartContractRegulation]],

    profilesCreated: Array[BlockEvent[Profile]],
    profilesUpdated: Array[BlockEvent[Profile]],

    tokensLinkedToProfile: Array[BlockEvent[ProfileTokens]],
    tokensUnlinkedFromProfile: Array[BlockEvent[ProfileTokens]],

    messages: Array[BlockEvent[MessageRequest]]
)

object BlockEvents {
    implicit val RW: default.ReadWriter[BlockEvents] = upickle.default.macroRW
}

case class BlockEvent[T](
    event: T,
    txId: String,
    blockNumber: Long
)

object BlockEvent {
    implicit def BlockEventRW[T: default.ReadWriter]: default.ReadWriter[BlockEvent[T]] = upickle.default.macroRW
}
