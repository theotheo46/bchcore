package ru.sberbank.blockchain.cnft.spec

import ru.sberbank.blockchain.cnft.model.{AcceptTokenRequest, _}

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
trait CNFTSpec[R[_]] {

    // =====================================================================================================================
    // Transaction regulation
    // =====================================================================================================================
    def approveTransaction(request: SignedTXRegulationRequest): R[OperationEffect]

    def addNotice(request: SignedTXRegulationNotification): R[OperationEffect]

    def rejectTransaction(request: SignedTXRegulationRequest): R[OperationEffect]


    // =====================================================================================================================
    // On-boarding
    // =====================================================================================================================

    def endorseMember(endorsement: SignedEndorsement): R[Unit]

    def endorseMemberPublic(endorsement: SignedPublicEndorsement): R[Unit]

    def revokePublicEndorsement(endorsement: SignedPublicEndorsement): R[Unit]

    def listEndorsements(memberId: String): R[Array[SignedEndorsement]]

    def listPublicEndorsements(memberId: String): R[Array[SignedPublicEndorsement]]

    // =====================================================================================================================
    // Public profiles
    // =====================================================================================================================

    /**
     * Creates public profile.
     *
     * @param profile public profile structure.
     * @return either error message or public profile upon success.
     */
    def createProfile(profile: Profile): R[Profile]

    /**
     * Updates public profile.
     *
     * @param profile public profile structure.
     * @return either error message or public profile upon success.
     */
    def updateProfile(profile: Profile): R[Profile]

    /**
     * Queries public profile by id.
     *
     * @param id public profile id.
     * @return either error message or public profile upon success.
     */
    def getProfile(id: String): R[Profile]

    /**
     * Queries a list of public profiles.
     *
     * @return either error message or list of public profiles upon success.
     */
    def listProfiles: R[Array[Profile]]

    /**
     * Links tokens to a public profile
     *
     * @param profileTokens profile address, with an array of token ids to be linked
     * @return either error message or ProfileTokens upon success
     */
    def linkTokensToProfile(profileTokens: ProfileTokens): R[Unit]

    /**
     * Get tokens linked to public profile
     *
     * @param profileId profile id
     * @return an array of token ids
     */
    def getLinkedToProfileTokenIds(profileId: String): R[Array[String]]

    /**
     * Unlinking tokens from public profile
     *
     * @param profileTokens profileTokens object with tokenIds to unlink
     * @return new ProfileTokens to save
     */
    def unlinkTokensFromProfile(profileTokens: ProfileTokens): R[Unit]

    /**
     * Get public profiles linked to token
     *
     * @param tokenId tokenId to query
     * @return an array of profileInfo objects
     */
    def getTokenProfiles(tokenId: String): R[Array[TokenProfileInfo]]

    // =====================================================================================================================
    // Smart contracts
    // =====================================================================================================================

    /** * Register Smart contract
     *
     * @param signedSmartContract Smart contract structure
     * @return either error message or  array of smart contract  upon success
     */
    def createSmartContract(signedSmartContract: SignedSmartContract): R[Unit]

    /**
     * Queries smart contract type registry for registered smart contract types by SmartContractTemplateId
     *
     * @param smartContractId id of smart contract type
     * @return either error message or SmartContract structure for this dataFeedId upon success
     */
    def getSmartContract(smartContractId: String): R[SmartContract]

    /**
     * Queries a list of registered smart contract types
     *
     * @return either error message or list of registered smart contract types upon success
     */
    def listSmartContracts: R[Array[SmartContract]]

    def getSmartContractState(id: String): R[SmartContractState]

    def getSmartContractRegulation(id: String): R[SmartContractRegulation]

    def approveSmartContract(signedSCRegulationRequest: SignedSCRegulationRequest): R[SmartContractRegulation]

    def rejectSmartContract(signedSCRegulationRequest: SignedSCRegulationRequest): R[String]

    // =====================================================================================================================
    // Smart contract types
    // =====================================================================================================================

    //    /**
    //     * Register Smart contract type
    //     *
    //     * @param request Smart contract type structure
    //     * @return either error message or  array of smart contract types upon success
    //     */
    //    def registerSmartContractTemplate(request: SmartContractTemplate): R[SmartContractTemplate]
    //

    /**
     * Queries smart contract type registry for registered smart contract types by SmartContractTemplateId
     *
     * @param templateId id of smart contract template
     * @return either error message or SmartContractTemplate structure for this dataFeedId upon success
     */
    def getSmartContractTemplate(templateId: String): R[SmartContractTemplate]

    /**
     * Queries a list of registered smart contract types
     *
     * @return either error message or list of registered smart contract types upon success
     */
    def listSmartContractTemplates: R[Array[SmartContractTemplate]]

    /**
     * Queries a list of accepted smart contract deals by smart contract address
     *
     * @param id smart contract id
     * @return either error message or list of accepted deals for smart contract address upon success
     */
    def listSmartContractAcceptedDeals(id: String): R[Array[AcceptedDeal]]

    // =====================================================================================================================
    // Token types
    // =====================================================================================================================

    /**
     * Register TokenType
     *
     * @param requests token type structures
     * @return either error message or custody url upon success
     */
    def registerTokenType(requests: SignedTokenTypeRegistration): R[Unit]

    /**
     * Queries token types registry for registered token type by typeId
     *
     * @param typeId name of token type
     * @return either error message or TokenType structure for this typeId upon success
     */
    def getTokenType(typeId: String): R[TokenType]

    /**
     * Queries a list of registered token types
     *
     * @return either error message or list of registered token types upon success
     */
    def listTokenTypes: R[Array[TokenType]]

    /**
     * Queries a list of registered token types filtered by fungible/non-fungible
     *
     * @return either error message or list of registered token types upon success
     */
    def listTokenTypesFiltered(filter: TokenTypeFilter): R[Array[TokenType]]

    // =====================================================================================================================
    // Tokens
    // =====================================================================================================================

    /**
     * Queries for the token (WalletToken structure)
     *
     * @param tokenId identifier of token
     * @return either error message or token upon success
     */
    def getToken(tokenId: String): R[WalletToken]

    def getTokenOwner(tokenId: String): R[TokenOwner]

    def getTokenContent(tokenId: String): R[TokenContent]

    def getTokenRestrictions(tokenId: String): R[TokenRestrictions]

    def listTokens: R[Array[WalletToken]]

    def listBurntTokens: R[Array[WalletToken]]

    def getBurntToken(tokenId: String): R[WalletToken]

    def getTokensByTypeId(typeId: String): R[Array[WalletToken]]

    /**
     * Issue token
     *
     * @return either error message or
     */
    def issueToken(issueTokenRequest: IssueTokenRequest): R[OperationEffect]

    def changeToken(request: SignedTokenChangeRequest): R[TokenChangeResponse]

    def mergeTokens(request: SignedTokenMergeRequest): R[TokenMergeResponse]

    /**
     * Tries to apply DealRequest to the IDs registry
     *
     * @param request describes deal
     * @return either error message or Token upon success
     */
    def acceptToken(request: AcceptTokenRequest): R[OperationEffect]

    /**
     * Tries to apply DealRequest to the IDs registry
     *
     * @param dealRequest describes deal
     * @return either error message or Token upon success
     */
    def makeDeal(dealRequest: DealRequest): R[OperationEffect]

    /**
     * Tries to burn token in registry
     *
     * @param burnRequest to burn token id
     * @return either error message or "no-value" upon success
     */
    def burnTokens(burnRequest: SignedBurnRequest): R[OperationEffect]

    // =====================================================================================================================
    // Regulatory section
    // =====================================================================================================================

    /**
     * Tries to freeze token due regulatory actions
     *
     * @param request to freeze token
     * @return either error message or "no-value" upon success
     */
    def freezeToken(request: Array[TokenFreezeRequest]): R[Unit]

    /**
     * Tries to burn token in registry by regulator
     *
     * @param request to burn token id
     * @return either error message or "no-value" upon success
     */
    def regulatorBurnToken(request: RegulatorBurnRequest): R[Unit]

    /**
     * Transfer tokens to new owner
     *
     * @param request to tokens transfer
     * @return either error message or "no-value" upon success
     */
    def regulatorTransfer(request: RegulatorTransferRequest): R[Unit]

    /**
     * Transfer tokens to new owner
     *
     * @param request to tokens transfer
     * @return either error message or "no-value" upon success
     */
    def regulatorChangeToken(request: RegulatorSignedTokenChangeRequest): R[TokenChangeResponse]

    // =====================================================================================================================
    // Offers
    // =====================================================================================================================

    /**
     * Offers token to sell
     *
     * @return either error message or "no-value" upon success
     */
    def putOffers(putOfferRequests: Array[PutOfferRequest]): R[Array[Offer]]

    /**
     * Finalizes an offer
     *
     * @return either error message or "no-value" upon success
     */
    def closeOffers(offerIds: Array[String]): R[Unit]

    // =====================================================================================================================
    // Messaging
    // =====================================================================================================================

    def publishMessages(messages: Array[Message]): R[Array[MessageRequest]]

    def listMessages(to: String): R[Array[MessageRequest]]

    def listMessagesFrom(from: String): R[Array[MessageRequest]]

    // =====================================================================================================================
    // Address book section
    // =====================================================================================================================

    def registerMember(request: RegisterMemberRequest): R[String]

    def updateMember(request: UpdateMemberInformationRequest): R[Unit]

    def getMember(name: String): R[MemberInformation]

    def listMembers: R[Array[MemberInformation]]

    def getMemberRegistrationBlock(memberId: String): R[Long]


    // =====================================================================================================================
    // Data feeds
    // =====================================================================================================================

    /**
     * Register Data feed
     *
     * @param request data feed structure
     * @return either error message or  array of data feed upon success
     */
    def registerDataFeed(request: SignedDataFeed): R[DataFeed]

    /**
     * Queries data feed registry for registered data feed by dataFeedId
     *
     * @param dataFeedAddress name of data feed
     * @return either error message or DataFeed structure for this dataFeedId upon success
     */
    def getDataFeed(dataFeedId: String): R[DataFeed]

    /**
     * Queries a list of registered data feeds
     *
     * @return either error message or list of registered data feeds upon success
     */
    def listDataFeeds: R[Array[DataFeed]]


    /**
     * Sets the value of data feed
     *
     * @param request request of DataFeedValue
     * @return either error message or "no-value" upon success
     */
    def submitDataFeedValue(request: FeedValueRequest): R[OperationEffect]

    /**
     * Gets current data feed value
     *
     * @param dataFeedAddress unique identificator for feed
     * @return either error message or DataFeedValue upon success
     */
    def getDataFeedValue(dataFeedId: String): R[DataFeedValue]

    def getTokenLinkedOperations(tokenId: String): R[Array[String]]

    // =====================================================================================================================
    // Operations
    // =====================================================================================================================

    def getOperation(operationId: String): R[OperationHistory]

    def listOperations: R[Array[OperationHistory]]

    def getCurrentVersionInfo: R[PlatformVersion]

    def getEngineVersion: R[String]
}
