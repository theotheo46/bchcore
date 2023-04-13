package ru.sberbank.blockchain.cnft.gate.service

import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.model._
import tools.http.service.annotations.{HttpGet, HttpHeaderValue}

import scala.language.higherKinds
import scala.scalajs.js.annotation.JSExport

/**
 * @author Alexey Polubelov
 */
trait ChainServiceSpec[R[_]] extends CNFTBlocksSpec[R] with POWServiceSpec[R] {

    // =====================================================================================================================
    // Endorsements
    // =====================================================================================================================

    @JSExport
    @HttpGet("/endorsement/list")
    def listEndorsements(memberId: String): R[Collection[SignedEndorsement]]


    @JSExport
    @HttpGet("/endorsement/public-list")
    def listPublicEndorsements(memberId: String):  R[Collection[SignedPublicEndorsement]]

    // =====================================================================================================================
    // Profiles
    // =====================================================================================================================

    /**
     * Queries public profile by id.
     *
     * @param id public profile id.
     * @return either error message or public profile upon success.
     */
    @JSExport
    @HttpGet("/profile")
    def getProfile(id: String): R[Profile]

    /**
     * Queries a list of public profiles.
     *
     * @return either error message or list of public profiles upon success.
     */
    @JSExport
    @HttpGet("/profile/list")
    def listProfiles: R[Collection[Profile]]

    /**
     * Getting tokens linked to public profile
     *
     * @param profileId public profile id
     * @return an array of token ids
     */
    @JSExport
    @HttpGet("/profile/tokens")
    def getLinkedToProfileTokenIds(profileId: String): R[Collection[String]]

    /**
     * Getting public profiles linked to token
     *
     * @param tokenId tokenId to query
     * @return an array of profileInfo objects
     */
    @JSExport
    @HttpGet("/profile/token-profiles")
    @HttpHeaderValue("Content-Type", "application/json")
    def getTokenProfiles(tokenId: String): R[Collection[TokenProfileInfo]]

    // =====================================================================================================================
    // Smart Contracts
    // =====================================================================================================================

    /**
     * Queries smart contracts registry for registered smart contracts by smartContractId
     *
     * @param id of smart contract
     * @return either error message or SmartContract structure for this smartContractId upon success
     */
    @JSExport
    @HttpGet("/smart-contract")
    def getSmartContract(id: String): R[SmartContract]

    /**
     * Queries a list of registered smart contracts
     *
     * @return either error message or list of registered smart contracts upon success
     */
    @JSExport
    @HttpGet("/smart-contract/list")
    def listSmartContracts: R[Collection[SmartContract]]

    @JSExport
    @HttpGet("/smart-contract/state")
    def getSmartContractState(id: String): R[SmartContractState]

    @JSExport
    @HttpGet("/smart-contract/regulation")
    def getSmartContractRegulation(id: String): R[SmartContractRegulation]


    /**
     * Queries smart contract type registry for registered smart contract types by SmartContractTemplateId
     *
     * @param templateId name of smart contract template
     * @return either error message or SmartContract structure for this smartContractId upon success
     */
    @JSExport
    @HttpGet("/smart-contract-template")
    def getSmartContractTemplate(templateId: String): R[SmartContractTemplate]

    /**
     * Queries a list of registered smart contract types
     *
     * @return either error message or list of registered smart contract types upon success
     */
    @JSExport
    @HttpGet("/smart-contract-template/list")
    def listSmartContractTemplates: R[Collection[SmartContractTemplate]]

    /**
     * Queries a list of accepted smart contract deals by smart contract address
     *
     * @param id smart contract id
     * @return either error message or list of accepted deals for smart contract address upon success
     */
    @JSExport
    @HttpGet("/smart-contract-accepted-deals/list")
    def listSmartContractAcceptedDeals(id: String): R[Collection[AcceptedDeal]]

    // =====================================================================================================================
    // Data Feeds
    // =====================================================================================================================

    /**
     * Queries data feed registry for registered data feed by dataFeedId
     *
     * @param id data feed id
     * @return either error message or DataFeed structure for this dataFeedId upon success
     */
    @JSExport
    @HttpGet("/data-feed")
    def getDataFeed(id: String): R[DataFeed]

    /**
     * Queries data feed value by dataFeedId
     *
     * @param id of data feed
     * @return either error message or DataFeedValue structure for this dataFeedId upon success
     */
    @JSExport
    @HttpGet("/data-feed-value")
    def getDataFeedValue(id: String): R[DataFeedValue]

    /**
     * Queries a list of registered data feeds
     *
     * @return either error message or list of registered data feeds upon success
     */
    @JSExport
    @HttpGet("/data-feed/list")
    def listDataFeeds: R[Collection[DataFeed]]


    // =====================================================================================================================
    // Token types
    // =====================================================================================================================

    /**
     * Queries token types registry for registered token type by typeId
     *
     * @param typeId name of token type
     * @return either error message or TokenType structure for this typeId upon success
     */
    @JSExport
    @HttpGet("/token-type")
    def getTokenType(typeId: String): R[TokenType]

    /**
     * Queries a list of registered token types
     *
     * @return either error message or list of registered token types upon success
     */
    @JSExport
    @HttpGet("/token-type/list")
    def listTokenTypes: R[Collection[TokenType]]

    /**
     * Queries a list of registered token types filtered by fungible/non-fungible
     *
     * @return either error message or list of registered token types upon success
     */
    @JSExport
    @HttpGet("/token-type/list-filtered")
    def listTokenTypesFiltered(filter: TokenTypeFilter): R[Collection[TokenType]]

    // =====================================================================================================================
    // Tokens
    // =====================================================================================================================

    /**
     * Queries for the token (WalletToken structure)
     *
     * @param tokenId identifier of token
     * @return either error message or token upon success
     */
    @JSExport
    @HttpGet("/token")
    def getToken(tokenId: String): R[WalletToken]

    /**
     * Queries for the owner of registered ID
     *
     * @param id to query for
     * @return either error message or owner public key bytes upon success
     */
    @JSExport
    @HttpGet("/token-id/owner")
    def getTokenOwner(id: String): R[TokenOwner]

    /**
     * Queries  information for token
     *
     * @return either error message or
     */
    @JSExport
    @HttpGet("/token-content")
    @HttpHeaderValue("Content-Type", "application/json")
    def getTokenContent(tokenId: String): R[TokenContent]

    /**
     * Queries  information for token
     *
     * @return either error message or
     */
    @JSExport
    @HttpGet("/token-restrictions")
    @HttpHeaderValue("Content-Type", "application/json")
    def getTokenRestrictions(tokenId: String): R[TokenRestrictions]

    /**
     * Get linked operation Id for a token
     *
     * @param tokenId the token identified
     * @return linked operation id if any otherwise - an empty array
     */
    @JSExport
    @HttpGet("/token/linked-operations")
    def getTokenLinkedOperation(tokenId: String): R[Collection[String]]

    /**
     *
     * @return
     */
    @JSExport
    @HttpGet("/token/list")
    def listTokens: R[Collection[WalletToken]]

    @JSExport
    @HttpGet("/token/list-archived")
    def listBurntTokens: R[Collection[WalletToken]]

    @JSExport
    @HttpGet("/token/archived")
    def getBurntToken(tokenId: String): R[WalletToken]

    @JSExport
    @HttpGet("/token/get-by-type-id")
    def getTokensByTypeId(typeId: String): R[Collection[WalletToken]]

    //==================================================================================================================
    // Regulatory section
    //==================================================================================================================


    //==================================================================================================================
    // Offers
    //==================================================================================================================


    //==================================================================================================================
    // Messages
    //==================================================================================================================

    @JSExport
    @HttpGet("/messages/list")
    def listMessages(to: String): R[Collection[MessageRequest]]

    @JSExport
    @HttpGet("/messages/list-from")
    def listMessagesFrom(from: String): R[Collection[MessageRequest]]


    //==================================================================================================================
    // Members
    //==================================================================================================================

    //TODO make description
    @JSExport
    @HttpGet("/member")
    def getMember(id: String): R[MemberInformation]

    //TODO make description
    @JSExport
    @HttpGet("/member-registration-block")
    def getMemberRegistrationBlock(memberId: String): R[Long]

    //TODO make description
    @JSExport
    @HttpGet("/member/list")
    def listMembers: R[Collection[MemberInformation]]

    //==================================================================================================================
    // Operations
    //==================================================================================================================


    @JSExport
    @HttpGet("/operation")
    def getOperation(operationId: String): R[OperationHistory]

    @JSExport
    @HttpGet("/operation/list")
    def listOperations: R[Collection[OperationHistory]]

    //==================================================================================================================
    // Data Migration
    //==================================================================================================================

    @JSExport
    @HttpGet("/version/current")
    def getCurrentVersionInfo: R[PlatformVersion]

    @JSExport
    @HttpGet("/version/gate")
    def getGateVersion: R[String]

    @JSExport
    @HttpGet("/version/engine")
    def getEngineVersion: R[String]

}
