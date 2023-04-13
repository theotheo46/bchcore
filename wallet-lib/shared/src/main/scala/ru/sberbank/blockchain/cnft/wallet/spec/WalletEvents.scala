package ru.sberbank.blockchain.cnft.wallet.spec

import ru.sberbank.blockchain.cnft.commons.Collection
import ru.sberbank.blockchain.cnft.gate.model.BlockEvent
import ru.sberbank.blockchain.cnft.model.{BurnRequest, DataFeed, EndorsementRequest, MessageRequest, PendingAccept, PendingBurn, PendingDeal, PendingIssue, ProfileTokens, SignedEndorsement, SignedPublicEndorsement, SignedRejectEndorsementRequest, SmartContract, SmartContractRegulation, SmartContractState, TokenChangeResponse, TokenMergeResponse, TokenRequest, TransferProposal}
import ru.sberbank.blockchain.cnft.wallet.walletmodel.SCRejectedResult
import ru.sberbank.blockchain._
import ru.sberbank.blockchain.cnft.wallet.spec.ValidatedEvent.Valid
import upickle.default._

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("WalletEvents")
case class WalletEvents(
    blockNumber: Long,
    member: MemberEvents,
    owner: OwnerEvents,
    issuer: IssuerEvents,
    smartContracts: SmartContractEvents,
    dataFeeds: DataFeedEvents,
    regulator: RegulatorEvents,
    operations: OperationsListEvents,
    profiles: ProfileEvents
)


object WalletEvents {
    implicit val WalletEventsRW: ReadWriter[WalletEvents] = upickle.default.macroRW

    def empty: WalletEvents = WalletEvents(
        blockNumber = 0L,
        member = MemberEvents(Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty),
        owner = OwnerEvents(Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty),
        issuer = IssuerEvents(Collection.empty),
        smartContracts = SmartContractEvents(Collection.empty, Collection.empty, Collection.empty, Collection.empty),
        dataFeeds = DataFeedEvents(Collection.empty),
        regulator = RegulatorEvents(Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty),
        operations = OperationsListEvents(Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty),
        profiles = ProfileEvents(Collection.empty, Collection.empty, Collection.empty, Collection.empty)
    )
}

@JSExportAll
@JSExportTopLevel("MemberEvents")
case class MemberEvents(
    registered: Collection[BlockEvent[String]],
    endorsed: Collection[SignedEndorsement],
    endorsedPublic: Collection[SignedPublicEndorsement],
    revokedEndorsements: Collection[SignedPublicEndorsement],
    endorsementRequested: Collection[IncomingMessage[BlockEvent[EndorsementRequest]]],
    endorsementRejected: Collection[IncomingMessage[BlockEvent[SignedRejectEndorsementRequest]]],
    genericMessages: Collection[IncomingMessage[BlockEvent[MessageRequest]]]
)

object MemberEvents {
    implicit val MemberEventsRW: ReadWriter[MemberEvents] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("OwnerEvents")
case class OwnerEvents(
    tokensIssued: Collection[String], // by me
    transfersProposed: Collection[IncomingMessage[BlockEvent[TransferProposal]]],
    tokensRequested: Collection[IncomingMessage[BlockEvent[TokenRequest]]],
    tokensPending: Collection[PendingAccept],
    tokensReceived: Collection[String],
    tokensBurn: Collection[String],
    tokenChanged: Collection[TokenChangeResponse]
)

object OwnerEvents {
    implicit val OwnerEventsRW: ReadWriter[OwnerEvents] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("IssuerEvents")
case class IssuerEvents(
    tokensBurn: Collection[BurnRequest], // tokens issued by me was burn
)

object IssuerEvents {
    implicit val IssuerEventsRW: ReadWriter[IssuerEvents] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("SmartContractEvents")
case class SmartContractEvents
(
    rejectedSmartContracts: Collection[SCRejectedResult],
    addedSmartContracts: Collection[SmartContract],
    stateUpdatedSmartContracts: Collection[SmartContractState],
    regulationUpdatedSmartContracts: Collection[SmartContractRegulation],
)

object SmartContractEvents {
    implicit val SmartContractEventsRW: ReadWriter[SmartContractEvents] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("DataFeedEvents")
case class DataFeedEvents
(
    registeredDataFeed: Collection[DataFeed]
)

object DataFeedEvents {
    implicit val SmartContractEventsRW: ReadWriter[DataFeedEvents] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("ProfileEvents")
case class ProfileEvents(
    created: Collection[String],
    updated: Collection[String],
    tokensLinked: Collection[ProfileTokens],
    tokensUnlinked: Collection[ProfileTokens]
)

object ProfileEvents {
    implicit val ProfileEventsRW: ReadWriter[ProfileEvents] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("RegulatorEvents")
case class RegulatorEvents(
    pendingIssues: Collection[ValidatedEvent[PendingIssue]],
    pendingDeals: Collection[ValidatedEvent[PendingDeal]],
    pendingBurn: Collection[ValidatedEvent[PendingBurn]],
    pendingSmartContracts: Collection[String],
    tokenChanged: Collection[TokenChangeResponse],
    tokenMerged: Collection[TokenMergeResponse]
)

object RegulatorEvents {
    implicit val RegulatorEventsRW: ReadWriter[RegulatorEvents] = upickle.default.macroRW
}


@JSExportAll
@JSExportTopLevel("OperationsListEvents")
case class OperationsListEvents(
    exchangeOperations: Collection[String],
    burnMyTokenOperations: Collection[String],
    issueForMeTokenOperations: Collection[String],
    dealPendingOperations: Collection[String],
    burnPendingOperations: Collection[String],
    outgoingAcceptPendingOperations: Collection[String],
    incommingAcceptPendingOperations: Collection[String],
    issuePendingsOperations: Collection[String]
)

object OperationsListEvents {
    implicit val OperationsListEventsRW: ReadWriter[OperationsListEvents] = upickle.default.macroRW
}

case class ValidatedEvent[T](
    event: T,
    msg: String
) {
    def isValid: Boolean = msg == Valid
}


object ValidatedEvent {
    val Valid = "VALID"

    implicit def ValidatedEventRW[T: upickle.default.ReadWriter]: upickle.default.ReadWriter[ValidatedEvent[T]] = upickle.default.macroRW
}
