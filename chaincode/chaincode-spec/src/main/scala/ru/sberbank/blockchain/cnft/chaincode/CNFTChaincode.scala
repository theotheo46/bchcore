package ru.sberbank.blockchain.cnft.chaincode

import org.enterprisedlt.spec.{ContractOperation, OperationType}
import ru.sberbank.blockchain.cnft.model._
import ru.sberbank.blockchain.cnft.spec.CNFTSpec

/**
 * @author Alexey Polubelov
 */
trait CNFTChaincode[ContractResult[_]] extends CNFTSpec[ContractResult] {

    // =====================================================================================================================
    // Public profiles
    // =====================================================================================================================

    /**
     * Creates public profile.
     *
     * @param profile public profile structure.
     * @return either error message or public profile upon success.
     */
    @ContractOperation(OperationType.Invoke)
    override def createProfile(profile: Profile): ContractResult[Profile]

    /**
     * Updates public profile.
     *
     * @param profile public profile structure.
     * @return either error message or public profile upon success.
     */
    @ContractOperation(OperationType.Invoke)
    override def updateProfile(profile: Profile): ContractResult[Profile]

    /**
     * Queries public profile by id.
     *
     * @param id public profile id.
     * @return either error message or public profile upon success.
     */
    @ContractOperation(OperationType.Query)
    override def getProfile(id: String): ContractResult[Profile]

    /**
     * Queries a list of public profiles.
     *
     * @return either error message or list of public profiles upon success.
     */
    @ContractOperation(OperationType.Query)
    override def listProfiles: ContractResult[Array[Profile]]


    /**
     * Linking tokens array to public profile address
     *
     * @param profileTokens profile address, with an array of token ids to be linked
     * @return either error message or ProfileTokens upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def linkTokensToProfile(profileTokens: ProfileTokens): ContractResult[Unit]

    /**
     * Getting tokens linked to profile
     *
     * @param profileId profile id
     * @return an array of token ids
     */
    @ContractOperation(OperationType.Query)
    override def getLinkedToProfileTokenIds(profileId: String): ContractResult[Array[String]]

    /**
     * Unlink tokens from public profile
     *
     * @param profileTokens profile address, with an array of token ids to be linked
     * @return either error message or ProfileTokens on success
     */
    @ContractOperation(OperationType.Invoke)
    override def unlinkTokensFromProfile(profileTokens: ProfileTokens): ContractResult[Unit]

    /**
     * Get linked to token profiles
     *
     * @param tokenId tokenId to query
     * @return an array of profileInfo objects
     */
    @ContractOperation(OperationType.Query)
    override def getTokenProfiles(tokenId: String): ContractResult[Array[TokenProfileInfo]]

    // =====================================================================================================================
    // Transaction regulation
    // =====================================================================================================================
    @ContractOperation(OperationType.Invoke)
    override def approveTransaction(request: SignedTXRegulationRequest): ContractResult[OperationEffect]

    @ContractOperation(OperationType.Invoke)
    override def addNotice(request: SignedTXRegulationNotification): ContractResult[OperationEffect]

    @ContractOperation(OperationType.Invoke)
    override def rejectTransaction(request: SignedTXRegulationRequest): ContractResult[OperationEffect]


    // On boarding

    @ContractOperation(OperationType.Invoke)
    override def endorseMember(request: SignedEndorsement): ContractResult[Unit]

    @ContractOperation(OperationType.Invoke)
    override def endorseMemberPublic(request: SignedPublicEndorsement): ContractResult[Unit]

    @ContractOperation(OperationType.Invoke)
    override def revokePublicEndorsement(request: SignedPublicEndorsement): ContractResult[Unit]

    @ContractOperation(OperationType.Query)
    override def listEndorsements(memberId: String): ContractResult[Array[SignedEndorsement]]

    @ContractOperation(OperationType.Query)
    override def listPublicEndorsements(memberId: String): ContractResult[Array[SignedPublicEndorsement]]

    //    // =====================================================================================================================
    //    // Transactions
    //    // =====================================================================================================================
    //
    //    @ContractOperation(OperationType.Invoke)
    //    override def sendTransaction(signedTransaction: SignedTransaction): ContractResult[Unit]
    //
    // =====================================================================================================================
    // Smart contracts
    // =====================================================================================================================

    /**
     * Register Smart contract
     *
     * @param signedSmartContract Smart contract structure
     * @return either error message or  array of smart contract  upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def createSmartContract(signedSmartContract: SignedSmartContract): ContractResult[Unit]

    /**
     * Queries smart contract type registry for registered smart contract types by SmartContractTemplateId
     *
     * @param smartContractId name of smart contract type
     * @return either error message or SmartContract structure for this dataFeedId upon success
     */
    @ContractOperation(OperationType.Query)
    override def getSmartContract(smartContractId: String): ContractResult[SmartContract]

    /**
     * Queries a list of registered smart contract types
     *
     * @return either error message or list of registered smart contract types upon success
     */
    @ContractOperation(OperationType.Query)
    override def listSmartContracts: ContractResult[Array[SmartContract]]

    @ContractOperation(OperationType.Query)
    override def getSmartContractState(id: String): ContractResult[SmartContractState]

    @ContractOperation(OperationType.Query)
    override def getSmartContractRegulation(id: String): ContractResult[SmartContractRegulation]

    @ContractOperation(OperationType.Invoke)
    override def approveSmartContract(signedSCRegulationRequest: SignedSCRegulationRequest): ContractResult[SmartContractRegulation]

    @ContractOperation(OperationType.Invoke)
    override def rejectSmartContract(signedSCRegulationRequest: SignedSCRegulationRequest): ContractResult[String]
    // =====================================================================================================================
    // Smart contract templates
    // =====================================================================================================================

    //    /**
    //     * Register Smart contract type
    //     *
    //     * @param request Smart contract type structure
    //     * @return either error message or  array of smart contract types upon success
    //     */
    //    @ContractOperation(OperationType.Invoke)
    //    override def registerSmartContractTemplate(request: SmartContractTemplate): ContractResult[SmartContractTemplate]

    /**
     * Queries smart contract type registry for registered smart contract types by SmartContractTemplateId
     *
     * @param templateId name of smart contract type
     * @return either error message or DataFeed structure for this dataFeedId upon success
     */
    @ContractOperation(OperationType.Query)
    override def getSmartContractTemplate(templateId: String): ContractResult[SmartContractTemplate]

    /**
     * Queries a list of registered smart contract types
     *
     * @return either error message or list of registered smart contract types upon success
     */
    @ContractOperation(OperationType.Query)
    override def listSmartContractTemplates: ContractResult[Array[SmartContractTemplate]]

    /**
     * Queries a list of accepted smart contract deals by smart contract address
     *
     * @param id smart contract address
     * @return either error message or list of accepted deals for smart contract address upon success
     */
    @ContractOperation(OperationType.Query)
    override def listSmartContractAcceptedDeals(id: String): ContractResult[Array[AcceptedDeal]]

    // =====================================================================================================================
    // Data feeds
    // =====================================================================================================================


    /**
     * Register Data feed
     *
     * @param request data feed structure
     * @return either error message or  array of data feed upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def registerDataFeed(request: SignedDataFeed): ContractResult[DataFeed]

    /**
     * Queries data feed registry for registered data feed by dataFeedId
     *
     * @param dataFeedId name of data feed
     * @return either error message or DataFeed structure for this dataFeedId upon success
     */
    @ContractOperation(OperationType.Query)
    override def getDataFeed(dataFeedId: String): ContractResult[DataFeed]

    /**
     * Queries a list of registered data feeds
     *
     * @return either error message or list of registered data feeds upon success
     */
    @ContractOperation(OperationType.Query)
    override def listDataFeeds: ContractResult[Array[DataFeed]]

    /**
     * Sets the value of data feed
     *
     * @param requests collection of FeedValueRequest
     * @return either error message or "no-value" upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def submitDataFeedValue(requests: FeedValueRequest): ContractResult[OperationEffect]

    /**
     * Gets the value of data feed
     *
     * @param dataFeedId unique identificator for data feed
     * @return either error message or DataFeedValue upon success
     */
    @ContractOperation(OperationType.Query)
    override def getDataFeedValue(dataFeedId: String): ContractResult[DataFeedValue]

    /**
     * Register TokenType
     *
     * @param request token type structure
     * @return either error message or custody url upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def registerTokenType(request: SignedTokenTypeRegistration): ContractResult[Unit]

    /**
     * Queries token types registry for registered token type by typeId
     *
     * @param typeId name of token type
     * @return either error message or TokenType structure for this typeId upon success
     */
    @ContractOperation(OperationType.Query)
    override def getTokenType(typeId: String): ContractResult[TokenType]

    /**
     * Queries a list of registered token types
     *
     * @return either error message or list of registered token types upon success
     */
    @ContractOperation(OperationType.Query)
    override def listTokenTypes: ContractResult[Array[TokenType]]

    /**
     * Queries a list of registered token types filtered by fungible/non-fungible
     *
     * @return either error message or list of registered token types upon success
     */
    @ContractOperation(OperationType.Query)
    override def listTokenTypesFiltered(filter: TokenTypeFilter): ContractResult[Array[TokenType]]

    /**
     * Queries for the token (WalletToken structure)
     *
     * @param id identifier of token
     * @return either error message or token upon success
     */
    @ContractOperation(OperationType.Query)
    override def getToken(tokenId: String): ContractResult[WalletToken]

    /**
     * Queries for the owner of registered ID
     *
     * @param tokenId to query for
     * @return either error message or owner public key bytes upon success
     */
    @ContractOperation(OperationType.Query)
    override def getTokenOwner(tokenId: String): ContractResult[TokenOwner]

    /**
     * Queries  information for token
     *
     * @return either error message or
     */
    @ContractOperation(OperationType.Query)
    override def getTokenContent(tokenId: String): ContractResult[TokenContent]

    /**
     * Queries  information for token
     *
     * @return either error message or
     */
    @ContractOperation(OperationType.Query)
    override def getTokenRestrictions(tokenId: String): ContractResult[TokenRestrictions]

    /**
     * Issue token
     *
     * @return either error message or
     */
    @ContractOperation(OperationType.Invoke)
    override def issueToken(issueTokenRequest: IssueTokenRequest): ContractResult[OperationEffect]

    /**
     * Change token
     *
     * @return either error message or
     */
    @ContractOperation(OperationType.Invoke)
    override def changeToken(request: SignedTokenChangeRequest): ContractResult[TokenChangeResponse]

    /**
     * Merge tokens
     *
     * @return either error message or
     */
    @ContractOperation(OperationType.Invoke)
    override def mergeTokens(request: SignedTokenMergeRequest): ContractResult[TokenMergeResponse]

    /**
     * Accept token
     *
     * @return either error message or
     */
    @ContractOperation(OperationType.Invoke)
    override def acceptToken(request: AcceptTokenRequest): ContractResult[OperationEffect]

    /**
     * Tries to apply DealRequest to the IDs registry
     *
     * @param dealRequest describes deal
     * @return either error message or "no-value" upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def makeDeal(dealRequest: DealRequest): ContractResult[OperationEffect]

    /**
     * Tries to burn token in registry
     *
     * @param request to burn token id
     * @return either error message or "no-value" upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def burnTokens(request: SignedBurnRequest): ContractResult[OperationEffect]

    /**
     * Tries to freeze token due regulatory actions
     *
     * @param request to freeze token
     * @return either error message or "no-value" upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def freezeToken(request: Array[TokenFreezeRequest]): ContractResult[Unit]


    /**
     * Tries to burn token in registry by regulator
     *
     * @param request to burn tokens
     * @return either error message or "no-value" upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def regulatorBurnToken(request: RegulatorBurnRequest): ContractResult[Unit]

    /**
     * Tries transfer token in registry to new owner
     *
     * @param request transfer token id's
     * @return either error message or "no-value" upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def regulatorTransfer(request: RegulatorTransferRequest): ContractResult[Unit]

    /**
     * Regulatory change token
     *
     * @return either error message or
     */
    @ContractOperation(OperationType.Invoke)
    override def regulatorChangeToken(request: RegulatorSignedTokenChangeRequest): ContractResult[TokenChangeResponse]

    // =====================================================================================================================
    // Exchange
    // =====================================================================================================================

    /**
     * Offers token to sell
     *
     * @return either error message or "no-value" upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def putOffers(putOfferRequests: Array[PutOfferRequest]): ContractResult[Array[Offer]]

    /**
     * Close an offer
     *
     * @return either error message or "no-value" upon success
     */
    @ContractOperation(OperationType.Invoke)
    override def closeOffers(offerId: Array[String]): ContractResult[Unit]


    // =====================================================================================================================
    // Messages
    // =====================================================================================================================

    @ContractOperation(OperationType.Invoke)
    override def publishMessages(messages: Array[Message]): ContractResult[Array[MessageRequest]]

    @ContractOperation(OperationType.Query)
    override def listMessages(to: String): ContractResult[Array[MessageRequest]]

    @ContractOperation(OperationType.Query)
    override def listMessagesFrom(from: String): ContractResult[Array[MessageRequest]]

    // =====================================================================================================================
    // Address book section
    // =====================================================================================================================

    @ContractOperation(OperationType.Invoke)
    override def registerMember(request: RegisterMemberRequest): ContractResult[String]

    @ContractOperation(OperationType.Invoke)
    override def updateMember(request: UpdateMemberInformationRequest): ContractResult[Unit]

    @ContractOperation(OperationType.Query)
    override def getMember(name: String): ContractResult[MemberInformation]

    @ContractOperation(OperationType.Query)
    override def getMemberRegistrationBlock(memberId: String): ContractResult[Long]

    @ContractOperation(OperationType.Query)
    override def listMembers: ContractResult[Array[MemberInformation]]

    @ContractOperation(OperationType.Query)
    override def getTokenLinkedOperations(tokenId: String): ContractResult[Array[String]]

    // =====================================================================================================================
    // Tokens
    // =====================================================================================================================

    @ContractOperation(OperationType.Query)
    override def listTokens: ContractResult[Array[WalletToken]]

    @ContractOperation(OperationType.Query)
    override def listBurntTokens: ContractResult[Array[WalletToken]]

    @ContractOperation(OperationType.Query)
    override def getBurntToken(tokenId: String): ContractResult[WalletToken]

    @ContractOperation(OperationType.Query)
    override def getTokensByTypeId(typeId: String): ContractResult[Array[WalletToken]]

    // =====================================================================================================================
    // Operations
    // =====================================================================================================================

    @ContractOperation(OperationType.Query)
    override def getOperation(operationId: String): ContractResult[OperationHistory]

    @ContractOperation(OperationType.Query)
    override def listOperations: ContractResult[Array[OperationHistory]]

    @ContractOperation(OperationType.Query)
    override def getCurrentVersionInfo: ContractResult[PlatformVersion]

    @ContractOperation(OperationType.Query)
    override def getEngineVersion: ContractResult[String]

}
