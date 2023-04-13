package ru.sberbank.blockchain.cnft.gate.service

import org.enterprisedlt.fabric.client.FabricChannel
import org.enterprisedlt.spec.BinaryCodec
import org.hyperledger.fabric.sdk.BlockInfo
import ru.sberbank.blockchain.cnft.commons._
import ru.sberbank.blockchain.cnft.gate.CNFTOperation._
import ru.sberbank.blockchain.cnft.gate.model._
import ru.sberbank.blockchain.cnft.model.{TokenChangeResponse, _}

import scala.jdk.CollectionConverters.{asScalaIteratorConverter, iterableAsScalaIterableConverter}

/**
 * @author Alexey Polubelov
 */
trait CNFTBlocksService extends CNFTBlocksSpec[Result] with LoggingSupport {
    def channel: FabricChannel

    def chainCodeName: String

    def codec: BinaryCodec

    override def getLatestBlockNumber: Result[Long] = channel.getChannelHeight.right.map(_ - 1)

    override def getTransactions(blockNumber: Long): Result[BlockEvents] = Result {
        fetchTransactionEnvelopeInfoByBlockNum(blockNumber)
            .map { t =>
                t.foldLeft(
                    BlockEvents(
                        membersRegistered = Array.empty,
                        dataFeedRegistered = Array.empty,
                        tokenTypesRegistered = Array.empty,
                        tokensExchanged = Array.empty,
                        tokensIssued = Array.empty,
                        tokensBurned = Array.empty,
                        tokensChanged = Array.empty,
                        tokensMerged = Array.empty,
                        tokenFrozen = Array.empty,
                        regulatorBurned = Array.empty,
                        regulatorTransferred = Array.empty,
                        offersAdded = Array.empty,
                        offersClosed = Array.empty,
                        approveEndorsements = Array.empty,
                        publicEndorsements = Array.empty,
                        revokedEndorsements = Array.empty,
                        pendingAccepts = Array.empty,
                        pendingIssues = Array.empty,
                        pendingDeals = Array.empty,
                        pendingBurns = Array.empty,
                        smartContractsAdded = Array.empty,
                        smartContractsRejected = Array.empty,
                        smartContractsStateUpdated = Array.empty,
                        smartContractsRegulationUpdated = Array.empty,
                        profilesCreated = Array.empty,
                        profilesUpdated = Array.empty,
                        tokensLinkedToProfile = Array.empty,
                        tokensUnlinkedFromProfile = Array.empty,
                        messages = Array.empty
                    )
                ) { case (transactions, envelop) =>
                    if (!envelop.isValid) {
                        transactions
                    } else {
                        val txId = envelop.getTransactionID
                        envelop
                            .getTransactionActionInfos
                            .iterator().asScala
                            .filter(_.getChaincodeIDName == chainCodeName)
                            .foldLeft(transactions) {
                                case (transactions, tai) =>
                                    if (tai.getResponseStatus != 200) { // skip unsuccessful
                                        transactions
                                    } else {
                                        val transaction = tai.getChaincodeInputArgs(0).toUTF8
                                        transaction match {
                                            case RegisterMember =>
                                                Result {
                                                    codec.decode[String](tai.getProposalResponsePayload, classOf[String])
                                                }.map { memberId =>
                                                    transactions.copy(
                                                        membersRegistered = transactions.membersRegistered :+ BlockEvent(memberId, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case RegisterDataFeed =>
                                                Result {
                                                    codec.decode[DataFeed](tai.getProposalResponsePayload, classOf[DataFeed])
                                                }.map { dataFeed =>
                                                    transactions.copy(
                                                        dataFeedRegistered = transactions.dataFeedRegistered :+ BlockEvent(dataFeed, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case RegisterTokenType =>
                                                Result {
                                                    codec.decode[SignedTokenTypeRegistration](tai.getChaincodeInputArgs(1), classOf[SignedTokenTypeRegistration])
                                                }.map { signedRequest =>
                                                    transactions.copy(
                                                        tokenTypesRegistered = transactions.tokenTypesRegistered ++ signedRequest.request.tokenType.map(tokenType => BlockEvent(tokenType, txId, blockNumber))
                                                    )
                                                }.getOrElse(transactions)

                                            case RequestIssueToken =>
                                                Result {
                                                    OperationEffect.parseFrom(tai.getProposalResponsePayload)
                                                }.map { effects =>
                                                    transactions.copy(
                                                        tokensIssued = transactions.tokensIssued ++ effects.issued.map(BlockEvent(_, txId, blockNumber)),
                                                        pendingIssues = transactions.pendingIssues ++ effects.pendingIssues.map(BlockEvent(_, txId, blockNumber))
                                                    )
                                                }.getOrElse(transactions)

                                            case MakeDeal =>
                                                Result {
                                                    OperationEffect.parseFrom(tai.getProposalResponsePayload)
                                                }.map { effects =>
                                                    appendEffectToEvents(transactions, effects, txId, blockNumber)
                                                }.getOrElse(transactions)


                                            case ApproveTransaction | AcceptToken =>
                                                Result {
                                                    codec.decode[OperationEffect](tai.getProposalResponsePayload, classOf[OperationEffect])
                                                }.map { operationEffect =>
                                                    appendEffectToEvents(transactions, operationEffect, txId, blockNumber)
                                                }.getOrElse(transactions)

                                            case RejectTransaction =>
                                                Result {
                                                    codec.decode[OperationEffect](tai.getProposalResponsePayload, classOf[OperationEffect])
                                                }.map { operationEffect =>
                                                    transactions.copy(
                                                        pendingIssues = transactions.pendingIssues ++ operationEffect.pendingIssues.map(BlockEvent(_, txId, blockNumber)),
                                                        pendingDeals = transactions.pendingDeals ++ operationEffect.pendingDeals.map(BlockEvent(_, txId, blockNumber)),
                                                        pendingBurns = transactions.pendingBurns ++ operationEffect.pendingBurns.map(BlockEvent(_, txId, blockNumber))
                                                    )
                                                }.getOrElse(transactions)

                                            case BurnToken =>
                                                Result {
                                                    codec.decode[OperationEffect](tai.getProposalResponsePayload, classOf[OperationEffect])
                                                }.map { operationEffect =>
                                                    transactions.copy(
                                                        tokensBurned = transactions.tokensBurned ++ operationEffect.burned.map(BlockEvent(_, txId, blockNumber)),
                                                        pendingBurns = transactions.pendingBurns ++ operationEffect.pendingBurns.map(BlockEvent(_, txId, blockNumber)),
                                                    )
                                                }.getOrElse(transactions)

                                            case ChangeToken =>
                                                Result {
                                                    codec.decode[TokenChangeResponse](tai.getProposalResponsePayload, classOf[TokenChangeResponse])
                                                }.map { tokenChangeResponse =>
                                                    transactions.copy(
                                                        tokensChanged = transactions.tokensChanged :+ BlockEvent(tokenChangeResponse, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case MergeTokens =>
                                                Result {
                                                    codec.decode[TokenMergeResponse](tai.getProposalResponsePayload, classOf[TokenMergeResponse])
                                                }.map { tokenMergeResponse =>
                                                    transactions.copy(
                                                        tokensMerged = transactions.tokensMerged :+ BlockEvent(tokenMergeResponse, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case FreezeToken =>
                                                Result {
                                                    codec.decode[Array[TokenFreezeRequest]](tai.getChaincodeInputArgs(1), classOf[Array[TokenFreezeRequest]])
                                                }.map { freezeRequests =>
                                                    transactions.copy(
                                                        tokenFrozen = transactions.tokenFrozen ++ freezeRequests.map(BlockEvent(_, txId, blockNumber))
                                                    )
                                                }.getOrElse(transactions)

                                            case RegulatorBurnToken =>
                                                Result {
                                                    codec.decode[RegulatorBurnRequest](tai.getChaincodeInputArgs(1), classOf[RegulatorBurnRequest])
                                                }.map { request =>
                                                    transactions.copy(
                                                        regulatorBurned = transactions.regulatorBurned :+ BlockEvent(request, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)
                                            case RegulatorTransfer =>
                                                Result {
                                                    RegulatorTransferRequest.parseFrom(tai.getChaincodeInputArgs(1))
                                                }.map { regulatoryTransferRequest =>
                                                    transactions.copy(
                                                        regulatorTransferred = transactions.regulatorTransferred :+ BlockEvent(regulatoryTransferRequest, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case RegulatorChangeToken =>
                                                Result {
                                                    codec.decode[TokenChangeResponse](tai.getProposalResponsePayload, classOf[TokenChangeResponse])
                                                }.map { tokenChangeResponse =>
                                                    transactions.copy(
                                                        tokensChanged = transactions.tokensChanged :+ BlockEvent(tokenChangeResponse, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case PutOffers =>
                                                Result {
                                                    codec.decode[Array[Offer]](tai.getProposalResponsePayload, classOf[Array[Offer]])
                                                }.map { offers =>
                                                    transactions.copy(
                                                        offersAdded = transactions.offersAdded ++ offers.map(offer => BlockEvent(offer, txId, blockNumber))
                                                    )
                                                }.getOrElse(transactions)

                                            case CloseOffers =>
                                                Result {
                                                    codec.decode[Array[String]](tai.getChaincodeInputArgs(1), classOf[Array[String]])
                                                }.map { offers =>
                                                    transactions.copy(
                                                        offersClosed = transactions.offersClosed ++ offers.map(offerId => BlockEvent(offerId, txId, blockNumber))
                                                    )
                                                }.getOrElse(transactions)

                                            case EndorseMember =>
                                                Result {
                                                    codec.decode[SignedEndorsement](tai.getChaincodeInputArgs(1), classOf[SignedEndorsement])
                                                }.map { signedApproveEndorsementRequest =>
                                                    transactions.copy(
                                                        approveEndorsements = transactions.approveEndorsements :+ BlockEvent(signedApproveEndorsementRequest, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case EndorseMemberPublic =>
                                                Result {
                                                    codec.decode[SignedPublicEndorsement](tai.getChaincodeInputArgs(1), classOf[SignedPublicEndorsement])
                                                }.map { signedEndorsementRequest =>
                                                    transactions.copy(
                                                        publicEndorsements = transactions.publicEndorsements :+ BlockEvent(signedEndorsementRequest, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case RevokePublicEndorsement =>
                                                Result {
                                                    codec.decode[SignedPublicEndorsement](tai.getChaincodeInputArgs(1), classOf[SignedPublicEndorsement])
                                                }.map { signedEndorsementRequest =>
                                                    transactions.copy(
                                                        revokedEndorsements = transactions.revokedEndorsements :+ BlockEvent(signedEndorsementRequest, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case CreateSmartContract =>
                                                Result {
                                                    codec.decode[SignedSmartContract](tai.getChaincodeInputArgs(1), classOf[SignedSmartContract])
                                                }.map { smartContract =>
                                                    transactions.copy(
                                                        smartContractsAdded = transactions.smartContractsAdded :+ BlockEvent(smartContract.contract, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case CreateProfile =>
                                                Result {
                                                    codec.decode[Profile](tai.getChaincodeInputArgs(1), classOf[Profile])
                                                }.map { profile =>
                                                    transactions.copy(
                                                        profilesCreated = transactions.profilesCreated :+ BlockEvent(profile, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case UpdateProfile =>
                                                Result {
                                                    codec.decode[Profile](tai.getChaincodeInputArgs(1), classOf[Profile])
                                                }.map { profile =>
                                                    transactions.copy(
                                                        profilesUpdated = transactions.profilesUpdated :+ BlockEvent(profile, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case LinkTokensToProfile =>
                                                Result {
                                                    codec.decode[ProfileTokens](tai.getChaincodeInputArgs(1), classOf[ProfileTokens])
                                                }.map { profileTokens =>
                                                    transactions.copy(
                                                        tokensLinkedToProfile = transactions.tokensLinkedToProfile :+ BlockEvent(profileTokens, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case UnlinkTokensFromProfile =>
                                                Result {
                                                    codec.decode[ProfileTokens](tai.getChaincodeInputArgs(1), classOf[ProfileTokens])
                                                }.map { profileTokens =>
                                                    transactions.copy(
                                                        tokensUnlinkedFromProfile = transactions.tokensUnlinkedFromProfile :+ BlockEvent(profileTokens, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case ApproveSmartContract =>
                                                Result {
                                                    codec.decode[SmartContractRegulation](tai.getProposalResponsePayload, classOf[SmartContractRegulation])
                                                }.map { smartContractsRegulation =>
                                                    transactions.copy(
                                                        smartContractsRegulationUpdated = transactions.smartContractsRegulationUpdated :+ BlockEvent(smartContractsRegulation, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case RejectSmartContract =>
                                                Result {
                                                    codec.decode[SignedSCRegulationRequest](tai.getChaincodeInputArgs(1), classOf[SignedSCRegulationRequest])
                                                }.map { signedSCRegulationRequest =>
                                                    transactions.copy(
                                                        smartContractsRejected = transactions.smartContractsRejected :+ BlockEvent(signedSCRegulationRequest.request, txId, blockNumber)
                                                    )
                                                }.getOrElse(transactions)

                                            case PublishMessages =>
                                                if (tai.getProposalResponsePayload == null || tai.getProposalResponsePayload.isEmpty) {
                                                    transactions
                                                } else
                                                    Result {
                                                        codec.decode[Array[MessageRequest]](tai.getProposalResponsePayload, classOf[Array[MessageRequest]])
                                                    }.map { messages =>
                                                        transactions.copy(
                                                            messages = transactions.messages ++ messages.map(BlockEvent(_, txId, blockNumber))
                                                        )
                                                    }.getOrElse(transactions)

                                            case SubmitDataFeedValue =>
                                                Result {
                                                    OperationEffect.parseFrom(tai.getProposalResponsePayload)
                                                }.map { effects =>

                                                    transactions.copy(
                                                        tokensIssued = transactions.tokensIssued ++ effects.issued.map(BlockEvent(_, txId, blockNumber)),
                                                        tokensExchanged = transactions.tokensExchanged ++ effects.transferred.map(BlockEvent(_, txId, blockNumber)),
                                                        pendingAccepts = transactions.pendingAccepts ++ effects.pendingAccepts.map(BlockEvent(_, txId, blockNumber)),
                                                        pendingIssues = transactions.pendingIssues ++ effects.pendingIssues.map(BlockEvent(_, txId, blockNumber)),
                                                        pendingDeals = transactions.pendingDeals ++ effects.pendingDeals.map(BlockEvent(_, txId, blockNumber)),
                                                        smartContractsStateUpdated = transactions.smartContractsStateUpdated ++ effects.smartContractUpdates.map(BlockEvent(_, txId, blockNumber))
                                                    )
                                                }.getOrElse(transactions)


                                            case _ => transactions
                                        }
                                    }
                            }
                    }
                }
            }
    }.flatMap(x => x)

    override def getTransaction(blockNumber: Long, txId: String): Result[BlockEvents] = {
        getTransactions(blockNumber).map { blockEvents =>
            BlockEvents(
                blockEvents.membersRegistered.filter(_.txId == txId),
                blockEvents.dataFeedRegistered.filter(dataFeed => dataFeed.txId == txId),
                blockEvents.tokenTypesRegistered.filter(tokenType => tokenType.txId == txId),
                blockEvents.tokensIssued.filter(tokensIssued => tokensIssued.txId == txId),
                blockEvents.tokensExchanged.filter(deal => deal.txId == txId),
                blockEvents.tokensBurned.filter(tokensBurned => tokensBurned.txId == txId),
                blockEvents.tokensChanged.filter(tokensChanged => tokensChanged.txId == txId),
                blockEvents.tokensMerged.filter(tokensMerged => tokensMerged.txId == txId),
                blockEvents.tokenFrozen.filter(tokenFrozen => tokenFrozen.txId == txId),
                blockEvents.regulatorBurned.filter(regulatorBurned => regulatorBurned.txId == txId),
                blockEvents.regulatorTransferred.filter(regulatory => regulatory.txId == txId),
                blockEvents.offersAdded.filter(offerAdded => offerAdded.txId == txId),
                blockEvents.offersClosed.filter(offerClosed => offerClosed.txId == txId),
                blockEvents.approveEndorsements.filter(_.txId == txId),
                blockEvents.publicEndorsements.filter(_.txId == txId),
                blockEvents.revokedEndorsements.filter(_.txId == txId),
                blockEvents.pendingAccepts.filter(pendingAccepts => pendingAccepts.txId == txId),
                blockEvents.pendingIssues.filter(pendingIssues => pendingIssues.txId == txId),
                blockEvents.pendingDeals.filter(_.txId == txId),
                blockEvents.pendingBurns.filter(_.txId == txId),
                blockEvents.smartContractsAdded.filter(_.txId == txId),
                blockEvents.smartContractsRejected.filter(_.txId == txId),
                blockEvents.smartContractsStateUpdated.filter(_.txId == txId),
                blockEvents.smartContractsRegulationUpdated.filter(_.txId == txId),
                blockEvents.profilesCreated.filter(_.txId == txId),
                blockEvents.profilesUpdated.filter(_.txId == txId),
                blockEvents.tokensLinkedToProfile.filter(_.txId == txId),
                blockEvents.tokensUnlinkedFromProfile.filter(_.txId == txId),
                blockEvents.messages.filter(message => message.txId == txId)
            )
        }
    }


    private def appendEffectToEvents(transactions: BlockEvents, operationEffect: OperationEffect, txId: String, blockNumber: Long): BlockEvents = {
        transactions.copy(
            tokensIssued = transactions.tokensIssued ++ operationEffect.issued.map(BlockEvent(_, txId, blockNumber)),
            tokensBurned = transactions.tokensBurned ++ operationEffect.burned.map(BlockEvent(_, txId, blockNumber)),
            tokensChanged = transactions.tokensChanged ++ operationEffect.change.map(BlockEvent(_, txId, blockNumber)),
            tokensExchanged = transactions.tokensExchanged ++ operationEffect.transferred.map(BlockEvent(_, txId, blockNumber)),
            //
            pendingIssues = transactions.pendingIssues ++ operationEffect.pendingIssues.map(BlockEvent(_, txId, blockNumber)),
            pendingAccepts = transactions.pendingAccepts ++ operationEffect.pendingAccepts.map(BlockEvent(_, txId, blockNumber)),
            pendingDeals = transactions.pendingDeals ++ operationEffect.pendingDeals.map(BlockEvent(_, txId, blockNumber)),
            pendingBurns = transactions.pendingBurns ++ operationEffect.pendingBurns.map(BlockEvent(_, txId, blockNumber)),
            tokensMerged = transactions.tokensMerged ++ operationEffect.merge.map(BlockEvent(_, txId, blockNumber)),
            smartContractsStateUpdated = transactions.smartContractsStateUpdated ++ operationEffect.smartContractUpdates.map(BlockEvent(_, txId, blockNumber))
        )
    }

    private def fetchTransactionEnvelopeInfoByBlockNum(numBlock: Long): Either[String, Array[BlockInfo#TransactionEnvelopeInfo]] =
        channel.getBlockByNumber(numBlock).map {
            _.getEnvelopeInfos.asScala
                .flatMap {
                    case te: BlockInfo#TransactionEnvelopeInfo => Some(te)
                    case _ => None
                }
                .toArray
        }

}
