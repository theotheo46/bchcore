package ru.sberbank.blockchain.cnft.gate.service

import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.model._
import tools.http.service.annotations.{Body, HttpHeaderValue, HttpPost}

import scala.language.higherKinds
import scala.scalajs.js.annotation.JSExport

/**
 * @author Alexey Polubelov
 */
trait ChainTxServiceSpec[R[_]] {

    // =====================================================================================================================
    // Transaction regulation
    // =====================================================================================================================

    @JSExport
    @HttpPost("/transaction/approve")
    @HttpHeaderValue("Content-Type", "application/json")
    def approveTransaction(@Body request: SignedTXRegulationRequest): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/transaction/addnotice")
    @HttpHeaderValue("Content-Type", "application/json")
    def addNotice(@Body request: SignedTXRegulationNotification): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/transaction/reject")
    @HttpHeaderValue("Content-Type", "application/json")
    def rejectTransaction(@Body request: SignedTXRegulationRequest): R[TxResult[Unit]]

    // =====================================================================================================================
    // Endorsements
    // =====================================================================================================================

    @JSExport
    @HttpPost("/endorsement/approve")
    @HttpHeaderValue("Content-Type", "application/json")
    def endorseMember(@Body request: SignedEndorsement): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/endorsement/endorse-public")
    @HttpHeaderValue("Content-Type", "application/json")
    def endorseMemberPublic(@Body request: SignedPublicEndorsement): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/endorsement/revoke")
    @HttpHeaderValue("Content-Type", "application/json")
    def revokePublicEndorsement(@Body request: SignedPublicEndorsement): R[TxResult[Unit]]

    // =====================================================================================================================
    // Profiles
    // =====================================================================================================================

    /**
     * Creates public profile.
     *
     * @param profile public profile structure.
     * @return either error message or public profile upon success.
     */
    @JSExport
    @HttpPost("/profile")
    @HttpHeaderValue("Content-Type", "application/json")
    def createProfile(@Body profile: Profile): R[TxResult[Profile]]

    /**
     * Updates public profile.
     *
     * @param profile public profile structure.
     * @return either error message or public profile upon success.
     */
    @JSExport
    @HttpPost("/profile/update")
    @HttpHeaderValue("Content-Type", "application/json")
    def updateProfile(@Body profile: Profile): R[TxResult[Profile]]

    /**
     * Links tokens to a public profile
     *
     * @param profileTokens profile address, with an array of token ids to be linked
     */
    @JSExport
    @HttpPost("/profile/link")
    @HttpHeaderValue("Content-Type", "application/json")
    def linkTokensToProfile(@Body profileTokens: ProfileTokens): R[TxResult[Unit]]

    /**
     * Unlink tokens from public profile
     *
     * @param profileTokens profileTokens to unlink
     * @return either error message or ProfileTokens on success
     */
    @JSExport
    @HttpPost("/profile/unlink")
    @HttpHeaderValue("Content-Type", "application/json")
    def unlinkTokensFromProfile(@Body profileTokens: ProfileTokens): R[TxResult[Unit]]

    // =====================================================================================================================
    // Smart Contracts
    // =====================================================================================================================

    /**
     * Register Smart contracts
     *
     * @param signedSmartContract Smart contracts structure
     * @return either error message or  array of smart contracts upon success
     */
    @JSExport
    @HttpPost("/smart-contract")
    @HttpHeaderValue("Content-Type", "application/json")
    def createSmartContract(@Body signedSmartContract: SignedSmartContract): R[TxResult[Unit]]

    /**
     *
     * @param signedSCRegulationRequest
     * @return
     */
    @JSExport
    @HttpPost("/smart-contract/approve")
    @HttpHeaderValue("Content-Type", "application/json")
    def approveSmartContract(@Body signedSCRegulationRequest: SignedSCRegulationRequest): R[TxResult[SmartContractRegulation]]

    /**
     *
     * @param signedSCRegulationRequest
     * @return
     */
    @JSExport
    @HttpPost("/smart-contract/reject")
    @HttpHeaderValue("Content-Type", "application/json")
    def rejectSmartContract(@Body signedSCRegulationRequest: SignedSCRegulationRequest): R[TxResult[String]]

    // =====================================================================================================================
    // Smart Contract templates
    // =====================================================================================================================

    //    /**
    //     * Register Smart contract type
    //     *
    //     * @param request Smart contract type structure
    //     * @return either error message or  array of smart contract types upon success
    //     */
    //    @JSExport
    //    @HttpPost("/smart-contract-template")
    //    @HttpHeaderValue("Content-Type", "application/json")
    //    def registerSmartContractTemplate(@Body request: SmartContractTemplate): R[TxResult[SmartContractTemplate]]


    // =====================================================================================================================
    // Data Feeds
    // =====================================================================================================================

    /**
     * Register Data feed
     *
     * @param request data feed structure
     * @return either error message or  array of data feed upon success
     */
    @JSExport
    @HttpPost("/register-data-feed")
    @HttpHeaderValue("Content-Type", "application/json")
    def registerDataFeed(@Body request: SignedDataFeed): R[TxResult[DataFeed]]

    /**
     * Sets the value of data feed
     *
     * @param request request of data feed
     * @return either error message or "no-value" upon success
     */
    @JSExport
    @HttpPost("/submit-data-feed")
    @HttpHeaderValue("Content-Type", "application/json")
    def submitDataFeedValue(@Body request: FeedValueRequest): R[TxResult[Unit]]

    // =====================================================================================================================
    // Token types
    // =====================================================================================================================

    /**
     * Register TokenType
     *
     * @param requests token type structure
     * @return either error message or custody url upon success
     */
    @JSExport
    @HttpPost("/token-type")
    @HttpHeaderValue("Content-Type", "application/json")
    def registerTokenType(@Body requests: SignedTokenTypeRegistration): R[TxResult[Unit]]


    // =====================================================================================================================
    // Tokens
    // =====================================================================================================================

    /**
     * Issue token
     *
     * @return either error message or
     */
    @JSExport
    @HttpPost("/token")
    @HttpHeaderValue("Content-Type", "application/json")
    def issueToken(@Body issueTokenRequest: IssueTokenRequest): R[TxResult[Unit]]

    /**
     *
     * @param request
     * @return
     */
    @JSExport
    @HttpPost("/token/change")
    @HttpHeaderValue("Content-Type", "application/json")
    def changeToken(@Body request: SignedTokenChangeRequest): R[TxResult[TokenChangeResponse]]

    /**
     *
     * @param request
     * @return
     */
    @JSExport
    @HttpPost("/tokens/merge")
    @HttpHeaderValue("Content-Type", "application/json")
    def mergeTokens(@Body request: SignedTokenMergeRequest): R[TxResult[TokenMergeResponse]]

    /**
     * Tries to apply DealRequest to the IDs registry
     *
     * @param dealRequest describes deal
     * @return either error message or "no-value" upon success
     */
    @JSExport
    @HttpPost("/deal")
    @HttpHeaderValue("Content-Type", "application/json")
    def makeDeal(@Body dealRequest: DealRequest): R[TxResult[Unit]]

    /**
     * Tries to burn token in registry
     *
     * @param request to burn token id
     * @return either error message or "no-value" upon success
     */
    @JSExport
    @HttpPost("/token/burn")
    @HttpHeaderValue("Content-Type", "application/json")
    def burnTokens(@Body request: SignedBurnRequest): R[TxResult[OperationEffect]]


    @JSExport
    @HttpPost("/token/accept")
    @HttpHeaderValue("Content-Type", "application/json")
    def acceptToken(@Body request: AcceptTokenRequest): R[TxResult[Unit]]


    //==================================================================================================================
    // Regulatory section
    //==================================================================================================================

    /**
     * Tries to freeze token due regulatory actions
     *
     * @param request to freeze token
     * @return either error message or "no-value" upon success
     */
    @JSExport
    @HttpPost("/regulatory/token/freeze")
    @HttpHeaderValue("Content-Type", "application/json")
    def freezeToken(@Body request: Collection[TokenFreezeRequest]): R[TxResult[Unit]]

    /**
     * Tries to burn token in registry by regulator
     *
     * @param requests to burn token id
     * @return either error message or "no-value" upon success
     */
    @JSExport
    @HttpPost("/regulatory/token/burn")
    @HttpHeaderValue("Content-Type", "application/json")
    def regulatoryBurnToken(@Body requests: RegulatorBurnRequest): R[TxResult[Unit]]

    /**
     * Transfer tokens to new owner
     *
     * @param request to tokens transfer
     * @return either error message or "no-value" upon success
     */
    @JSExport
    @HttpPost("/regulatory/token/transfer")
    @HttpHeaderValue("Content-Type", "application/json")
    def regulatoryTransfer(@Body request: RegulatorTransferRequest): R[TxResult[Unit]]

    /**
     * Regulatory change token
     *
     * @param request to change token
     * @return either error message or "no-value" upon success
     */
    @JSExport
    @HttpPost("/regulatory/token/change")
    @HttpHeaderValue("Content-Type", "application/json")
    def regulatoryChangeToken(@Body request: RegulatorSignedTokenChangeRequest): R[TxResult[TokenChangeResponse]]

    //==================================================================================================================
    // Offers
    //==================================================================================================================

    //    @JSExport
    //    @HttpPost("/exchange/offer")
    //    @HttpHeaderValue("Content-Type", "application/json")
    //    def putOffers(@Body putOfferRequests: Collection[PutOfferRequest]): R[TxResult[Collection[Offer]]]
    //
    //    @JSExport
    //    @HttpPost("/exchange/offer/close")
    //    @HttpHeaderValue("Content-Type", "application/json")
    //    def closeOffers(@Body offerId: Collection[String]): R[TxResult[Unit]]

    //==================================================================================================================
    // Messages
    //==================================================================================================================

    /**
     *
     * @param messages
     * @return
     */
    @JSExport
    @HttpPost("/messages")
    @HttpHeaderValue("Content-Type", "application/json")
    def publishMessages(@Body messages: Collection[Message]): R[TxResult[Unit]]

    //==================================================================================================================
    // Members
    //==================================================================================================================

    /**
     *
     * @param request
     * @return
     */
    @JSExport
    @HttpPost("/member")
    @HttpHeaderValue("Content-Type", "application/json")
    def registerMember(@Body request: RegisterMemberRequest): R[TxResult[String]]

    /**
     *
     * @param request
     * @return
     */
    @JSExport
    @HttpPost("/member/update")
    @HttpHeaderValue("Content-Type", "application/json")
    def updateMember(@Body request: UpdateMemberInformationRequest): R[TxResult[Unit]]

}
