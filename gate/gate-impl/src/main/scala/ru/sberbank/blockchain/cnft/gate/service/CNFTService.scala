package ru.sberbank.blockchain.cnft.gate.service

import org.enterprisedlt.fabric.client.ContractResult
import ru.sberbank.blockchain.cnft.CurrentPlatformVersion
import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.model._
import ru.sberbank.blockchain.cnft.spec.CNFTSpec

/**
 * @author Alexey Polubelov
 */
trait CNFTService extends ChainServiceSpec[Result] with ChainTxServiceSpec[Result] {
    def cnft: CNFTSpec[ContractResult]

    private implicit def wrap2TxResult[T](blockNumber: Long, txId: String, v: T): TxResult[T] = TxResult(blockNumber, txId, v)

    private implicit def ignoreTxId[T](blockNumber: Long, txId: String, v: T): T = v

    private implicit def arrayToResult[T](blockNumber: Long, txId: String, v: Collection[T]): Collection[T] = v

    override def approveTransaction(request: SignedTXRegulationRequest): Result[TxResult[Unit]] =
        cnft.approveTransaction(request).map(_ => ()).toResult

    override def addNotice(request: SignedTXRegulationNotification): Result[TxResult[Unit]] =
        cnft.addNotice(request).map(_ => ()).toResult

    override def rejectTransaction(request: SignedTXRegulationRequest): Result[TxResult[Unit]] =
        cnft.rejectTransaction(request).map(_ => ()).toResult

    // On boarding

    override def endorseMember(request: SignedEndorsement): Result[TxResult[Unit]] =
        cnft.endorseMember(request).toResult


    override def endorseMemberPublic(request: SignedPublicEndorsement): Result[TxResult[Unit]] =
        cnft.endorseMemberPublic(request).toResult

    override def revokePublicEndorsement(request: SignedPublicEndorsement): Result[TxResult[Unit]] =
        cnft.revokePublicEndorsement(request).toResult

    override def listEndorsements(memberId: String): Result[Collection[SignedEndorsement]] =
        cnft.listEndorsements(memberId).toResult


    override def listPublicEndorsements(memberId: String): Result[Collection[SignedPublicEndorsement]] =
        cnft.listPublicEndorsements(memberId).toResult

    // Profile

    override def createProfile(profile: Profile): Result[TxResult[Profile]] =
        cnft.createProfile(profile).toResult

    override def updateProfile(profile: Profile): Result[TxResult[Profile]] =
        cnft.updateProfile(profile).toResult

    override def getProfile(id: String): Result[Profile] =
        cnft.getProfile(id).toResult

    override def listProfiles: Result[Collection[Profile]] =
        cnft.listProfiles.toResult

    override def linkTokensToProfile(profileTokens: ProfileTokens): Result[TxResult[Unit]] = {
        cnft.linkTokensToProfile(profileTokens).toResult
    }

    override def getLinkedToProfileTokenIds(profileId: String): Result[Collection[String]] = {
        cnft.getLinkedToProfileTokenIds(profileId).toResult
    }

    override def unlinkTokensFromProfile(profileTokens: ProfileTokens): Result[TxResult[Unit]] = {
        cnft.unlinkTokensFromProfile(profileTokens).toResult
    }

    override def getTokenProfiles(tokenId: String): Result[Collection[TokenProfileInfo]] = {
        cnft.getTokenProfiles(tokenId).toResult
    }

    // Smart contracts

    /**
     * Register Smart contracts
     *
     * @param signedSmartContract Smart contracts structure
     * @return either error message or  array of smart contracts upon success
     */

    override def createSmartContract(signedSmartContract: SignedSmartContract): Result[TxResult[Unit]] =
        cnft.createSmartContract(signedSmartContract).toResult

    /**
     * Queries smart contracts registry for registered smart contracts by smartContractId
     *
     * @param id of smart contract
     * @return either error message or SmartContract structure for this smartContractId upon success
     */

    override def getSmartContract(id: String): Result[SmartContract] =
        cnft.getSmartContract(id).toResult


    /**
     * Queries a list of registered smart contracts
     *
     * @return either error message or list of registered smart contractss upon success
     */

    override def listSmartContracts: Result[Collection[SmartContract]] =
        cnft.listSmartContracts.toResult

    override def getSmartContractState(id: String): Result[SmartContractState] =
        cnft.getSmartContractState(id).toResult

    override def getSmartContractRegulation(id: String): Result[SmartContractRegulation] =
        cnft.getSmartContractRegulation(id).toResult


    override def approveSmartContract(signedSCRegulationRequest: SignedSCRegulationRequest): Result[TxResult[SmartContractRegulation]] =
        cnft.approveSmartContract(signedSCRegulationRequest).toResult

    override def rejectSmartContract(signedSCRegulationRequest: SignedSCRegulationRequest): Result[TxResult[String]] =
        cnft.rejectSmartContract(signedSCRegulationRequest).toResult

    // Smart contract types

    //    /**
    //     * Register Smart contract type
    //     *
    //     * @param smartContractTemplate Smart contract template structure
    //     * @return either error message or  array of smart contract types upon success
    //     */
    //    override def registerSmartContractTemplate(smartContractTemplate: SmartContractTemplate): Result[TxResult[SmartContractTemplate]] =
    //        cnft.registerSmartContractTemplate(smartContractTemplate).toResult

    /**
     * Queries smart contract type registry for registered smart contract types by SmartContractTemplateId
     *
     * @param templateId name of smart contract type
     * @return either error message or DataFeed structure for this dataFeedId upon success
     */
    override def getSmartContractTemplate(templateId: String): Result[SmartContractTemplate] =
        cnft.getSmartContractTemplate(templateId).toResult

    /**
     * Queries a list of registered smart contract types
     *
     * @return either error message or list of registered smart contract types upon success
     */
    override def listSmartContractTemplates: Result[Collection[SmartContractTemplate]] =
        cnft.listSmartContractTemplates.toResult

    /**
     * Queries a list of accepted smart contract deals by smart contract address
     *
     * @param id smart contract id
     * @return either error message or list of accepted deals for smart contract address upon success
     */
    override def listSmartContractAcceptedDeals(id: String): Result[Collection[AcceptedDeal]] =
        cnft.listSmartContractAcceptedDeals(id).toResult

    // Data feeds

    /**
     * Register Data feed
     *
     * @param request - collection of data feed structures
     * @return either error message or  array of data feed upon success
     */
    override def registerDataFeed(request: SignedDataFeed): Result[TxResult[DataFeed]] =
        cnft.registerDataFeed(request).toResult


    /**
     * Queries data feed registry for registered data feed by dataFeedId
     *
     * @param dataFeedId name of data feed
     * @return either error message or DataFeed structure for this dataFeedId upon success
     */
    override def getDataFeed(dataFeedId: String): Result[DataFeed] =
        cnft.getDataFeed(dataFeedId).toResult


    /**
     * Queries data feed value by dataFeedId
     *
     * @param dataFeedId name of data feed
     * @return either error message or DataFeedValue structure for this dataFeedId upon success
     */

    override def getDataFeedValue(dataFeedId: String): Result[DataFeedValue] =
        cnft.getDataFeedValue(dataFeedId).toResult

    /**
     * Queries a list of registered data feeds
     *
     * @return either error message or list of registered data feeds upon success
     */
    override def listDataFeeds: Result[Collection[DataFeed]] =
        cnft.listDataFeeds.toResult

    /**
     * Sets the value of data feed
     *
     * @param request collection of DataFeedValue
     * @return either error message or "no-value" upon success
     */
    override def submitDataFeedValue(request: FeedValueRequest): Result[TxResult[Unit]] =
        cnft.submitDataFeedValue(request).map(_ => ()).toResult

    /**
     * Register TokenType
     *
     * @param request token type structure
     * @return either error message or custody url upon success
     */
    override def registerTokenType(request: SignedTokenTypeRegistration): Result[TxResult[Unit]] = {
        cnft.registerTokenType(request).toResult
    }

    /**
     * Queries token types registry for registered token type by typeId
     *
     * @param typeId name of token type
     * @return either error message or TokenType structure for this typeId upon success
     */
    override def getTokenType(typeId: String): Result[TokenType] =
        cnft.getTokenType(typeId).toResult

    /**
     * Queries a list of registered token types
     *
     * @return either error message or list of registered token types upon success
     */
    override def listTokenTypes: Result[Collection[TokenType]] =
        cnft.listTokenTypes.toResult

    /**
     * Queries a list of registered token types filtered by fungible/non-fungible
     *
     * @return either error message or list of registered token types upon success
     */
    override def listTokenTypesFiltered(filter: TokenTypeFilter): Result[Collection[TokenType]] =
        cnft.listTokenTypesFiltered(filter).toResult

    /**
     * Queries for the token (WalletToken structure)
     *
     * @param id identifier of token
     * @return either error message or token upon success
     */
    override def getToken(tokenId: String): Result[WalletToken] =
        cnft.getToken(tokenId).toResult

    /**
     * Queries for the owner of registered ID
     *
     * @param id to query for
     * @return either error message or owner public key bytes upon success
     */
    override def getTokenOwner(id: String): Result[TokenOwner] =
        cnft.getTokenOwner(id).toResult

    /**
     * Queries for the owner of registered ID
     *
     * @param id to query for
     * @return either error message or owner public key bytes upon success
     */
    override def getTokenContent(id: String): Result[TokenContent] =
        cnft.getTokenContent(id).toResult

    /**
     * Queries for the owner of registered ID
     *
     * @param id to query for
     * @return either error message or owner public key bytes upon success
     */
    override def getTokenRestrictions(id: String): Result[TokenRestrictions] =
        cnft.getTokenRestrictions(id).toResult

    /**
     * Issues token
     *
     * @return either error message or
     */
    override def issueToken(issueTokenRequest: IssueTokenRequest): Result[TxResult[Unit]] =
        cnft.issueToken(issueTokenRequest).map(_ => ()).toResult


    override def changeToken(request: SignedTokenChangeRequest): Result[TxResult[TokenChangeResponse]] =
        cnft.changeToken(request).toResult

    override def mergeTokens(request: SignedTokenMergeRequest): Result[TxResult[TokenMergeResponse]] =
        cnft.mergeTokens(request).toResult

    /**
     * Tries to apply DealRequest to the IDs registry
     *
     * @param dealRequest describes deal
     * @return either error message or "no-value" upon success
     */
    override def makeDeal(dealRequest: DealRequest): Result[TxResult[Unit]] =
        cnft.makeDeal(dealRequest).map(_ => ()).toResult

    /**
     * Tries to burn token in registry
     *
     * @param request to burn token id
     * @return either error message or "no-value" upon success
     */
    override def burnTokens(request: SignedBurnRequest): Result[TxResult[OperationEffect]] =
        cnft.burnTokens(request).toResult

    override def listBurntTokens: Result[Collection[WalletToken]] =
        cnft.listBurntTokens.toResult

    override def getBurntToken(tokenId: String): Result[WalletToken] =
        cnft.getBurntToken(tokenId).toResult

    override def getTokensByTypeId(typeId: String): Result[Collection[WalletToken]] =
        cnft.getTokensByTypeId(typeId).toResult

    override def acceptToken(request: AcceptTokenRequest): Result[TxResult[Unit]] =
        cnft.acceptToken(request).map(_ => ()).toResult

    override def listTokens: Result[Collection[WalletToken]] =
        cnft.listTokens.toResult

    /**
     * Tries to freeze token due regulatory actions
     *
     * @param request to freeze token
     * @return either error message or "no-value" upon success
     */
    override def freezeToken(request: Collection[TokenFreezeRequest]): Result[TxResult[Unit]] =
        cnft.freezeToken(request).toResult

    /**
     * Tries to burn token in registry by regulator
     *
     * @param request to burn token id
     * @return either error message or "no-value" upon success
     */
    override def regulatoryBurnToken(request: RegulatorBurnRequest): Result[TxResult[Unit]] =
        cnft.regulatorBurnToken(request).toResult

    /**
     * Tries to transfer token in registry by regulator
     *
     * @param request to transfer token id's
     * @return either error message or "no-value" upon success
     */
    override def regulatoryTransfer(request: RegulatorTransferRequest): Result[TxResult[Unit]] =
        cnft.regulatorTransfer(request).toResult

    /**
     * Tries to change token by regulator
     *
     * @param request to change token
     * @return either error message or "no-value" upon success
     */
    override def regulatoryChangeToken(request: RegulatorSignedTokenChangeRequest): Result[TxResult[TokenChangeResponse]] =
        cnft.regulatorChangeToken(request).toResult

    /**
     * Publish messages to blockchain
     *
     * @param messages - collection of messages to publish
     * @return either error message or "no-value" upon success
     */
    override def publishMessages(messages: Collection[Message]): Result[TxResult[Unit]] =
        cnft.publishMessages(messages).map(_ => ()).toResult

    override def listMessages(to: String): Result[Collection[MessageRequest]] =
        cnft.listMessages(to).toResult

    override def listMessagesFrom(from: String): Result[Collection[MessageRequest]] =
        cnft.listMessagesFrom(from).toResult

    /**
     * Register member information to blockchain so it can be used at Addressbook
     *
     * @param request - request with sign and encryption member's key
     * @return either error message or "no-value" upon success
     */
    override def registerMember(request: RegisterMemberRequest): Result[TxResult[String]] =
        cnft.registerMember(request).toResult

    override def getMember(id: String): Result[MemberInformation] =
        cnft.getMember(id).toResult

    override def updateMember(request: UpdateMemberInformationRequest): Result[TxResult[Unit]] =
        cnft.updateMember(request).toResult


    override def getMemberRegistrationBlock(memberId: String): Result[Long] =
        cnft.getMemberRegistrationBlock(memberId).toResult

    override def getTokenLinkedOperation(tokenId: String): Result[Collection[String]] = cnft.getTokenLinkedOperations(tokenId).toResult

    override def listMembers: Result[Collection[MemberInformation]] =
        cnft.listMembers.toResult

    //    /**
    //     * Offers token to sell
    //     *
    //     * @return either error message or "no-value" upon success
    //     */
    //    override def putOffers(putOfferRequests: Collection[PutOfferRequest]): Result[TxResult[Collection[Offer]]] =
    //        cnft.putOffers(putOfferRequests).toResult
    //
    //    /**
    //     * Finalize offer
    //     *
    //     * @param offerIds offerId of the offer request
    //     * @return either error message or "no-value" upon success
    //     */
    //    override def closeOffers(offerIds: Collection[String]): Result[TxResult[Unit]] =
    //        cnft.closeOffers(offerIds).toResult

    // Operations

    override def getOperation(operationId: String): Result[OperationHistory] =
        cnft.getOperation(operationId).toResult

    override def getCurrentVersionInfo: Result[PlatformVersion] = cnft.getCurrentVersionInfo.toResult

    override def getGateVersion: Result[String] = Result {
        CurrentPlatformVersion
    }

    override def getEngineVersion: Result[String] = cnft.getEngineVersion.toResult

    override def listOperations: Result[Collection[OperationHistory]] =
        cnft.listOperations.toResult
}
