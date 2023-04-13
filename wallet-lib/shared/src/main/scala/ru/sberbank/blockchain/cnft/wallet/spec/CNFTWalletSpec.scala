package ru.sberbank.blockchain.cnft.wallet.spec

import ru.sberbank.blockchain.cnft.common.types.{BigInt, Bytes, Collection}
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.model._
import ru.sberbank.blockchain.cnft.wallet.walletmodel.{OperationData => _, _}
import tools.http.service.annotations.{HttpGet, HttpHeaderValue, HttpPost}

import scala.language.higherKinds
import scala.scalajs.js.annotation.JSExport

/**
 * @author Alexey Polubelov
 */
trait CNFTWalletSpec[R[+_]] {

    @JSExport
    def chain: ChainServiceSpec[R]

    // Token types

    @JSExport
    @HttpPost("/register-token-type")
    @HttpHeaderValue("Content-Type", "application/json")
    def registerTokenType(
        tokenTypeId: String,
        meta: TokenTypeMeta,
        dna: DNA,
        regulation: Collection[RegulatorCapabilities],
        burnExtraData: Collection[FieldMeta]
    ): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/list-owned-token-types")
    @HttpHeaderValue("Content-Type", "application/json")
    def listOwnedTokenTypes: R[Collection[TokenType]]

    // Addresses

    @JSExport
    @HttpPost("/create-address")
    @HttpHeaderValue("Content-Type", "application/json")
    def createAddress: R[Bytes]

    @JSExport
    @HttpPost("/create-single-owner-address")
    @HttpHeaderValue("Content-Type", "application/json")
    def createSingleOwnerAddress: R[TokenOwner]

    @JSExport
    @HttpGet("/create-id")
    @HttpHeaderValue("Content-Type", "application/json")
    def createId: R[String]

    // Tokens

    @JSExport
    @HttpPost("/issue")
    @HttpHeaderValue("Content-Type", "application/json")
    def issue(requests: Collection[WalletIssueTokenRequest]): R[TxResult[String]]

    // Send and receive tokens

    @JSExport
    @HttpPost("/send-token-to-member")
    @HttpHeaderValue("Content-Type", "application/json")
    def sendTokenToMember(
        memberId: String, address: Bytes, dealId: String,
        tokenIds: Collection[String], cptyEndorsements: Collection[SignedEndorsement],
        extraData: Bytes): R[TxResult[String]]

    @JSExport
    @HttpPost("/send-token-to-smart-contract")
    @HttpHeaderValue("Content-Type", "application/json")
    def sendTokenToSmartContract(id: String, dealId: String, tokenIds: Collection[String], extraData: Bytes, requiredDealExtra: Collection[String]): R[TxResult[String]]

    @JSExport
    @HttpPost("/accept-token")
    @HttpHeaderValue("Content-Type", "application/json")
    def acceptToken(transactionId: String): R[TxResult[Unit]]

    // Burn token

    @JSExport
    @HttpPost("/burn-token")
    @HttpHeaderValue("Content-Type", "application/json")
    def burnTokens(tokens: Collection[String], extra: Bytes, extraFields: Collection[String]): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/change-token")
    @HttpHeaderValue("Content-Type", "application/json")
    def changeToken(tokenId: String, amounts: Collection[String]): R[TxResult[TokenChangeResponse]]

    @JSExport
    @HttpPost("/merge-tokens")
    @HttpHeaderValue("Content-Type", "application/json")
    def mergeTokens(tokens: Collection[String]): R[TxResult[TokenMergeResponse]]

    @JSExport
    @HttpPost("/list-tokens")
    @HttpHeaderValue("Content-Type", "application/json")
    def listTokens: R[Collection[WalletToken]]

    @JSExport
    @HttpPost("/list-tokens-filtered")
    @HttpHeaderValue("Content-Type", "application/json")
    def listTokensFiltered(tokenTypeFilter: TokenTypeFilter): R[Collection[WalletToken]]

    @JSExport
    @HttpPost("/listBurnt-issued-tokens")
    @HttpHeaderValue("Content-Type", "application/json")
    def listBurntIssuedTokens: R[Collection[WalletToken]]

    @JSExport
    @HttpPost("/get-tokens-by-type-id")
    @HttpHeaderValue("Content-Type", "application/json")
    def getTokensByTypeId(typeId: String): R[Collection[WalletToken]]

    @JSExport
    @HttpGet("/list-tokens-issued")
    def listIssuedTokens: R[Collection[WalletToken]]

    @JSExport
    @HttpGet("/list-tokens-burnt")
    def listBurntTokens: R[Collection[WalletToken]]

    //    // Offers - i.e. OTC exchange
    //
    //    def putOffer(supply: TokenDescription, demand: TokenDescription): R[TxResult[Offer]]
    //
    //    def listTokenSupplyCandidates(offerId: String): R[Collection[String]]
    //
    //    def listTokenDemandCandidates(offerId: String): R[Collection[String]]
    //
    //    def applyForOffer(offerId: String, dealId: String, signedToken: String): R[Unit]
    //
    //    def approveOffer(offerId: String, dealId: String, signedToken: String): R[Unit]
    //
    //    def finalizeOffer(offerId: String, dealId: String): R[Unit]
    //
    //    def closeOffer(offerId: String): R[TxResult[Unit]]
    //
    //    def listOffers: R[Collection[WalletOffer]]
    //
    //    def getOffer(offerId: String): R[Optional[WalletOffer]]

    // Data feeds

    @JSExport
    @HttpPost("/register-data-feed")
    @HttpHeaderValue("Content-Type", "application/json")
    def registerDataFeed(description: Collection[DescriptionField], fields: Collection[FieldMeta]): R[TxResult[DataFeed]]

    @JSExport
    @HttpPost("/submit-data-feed-value")
    @HttpHeaderValue("Content-Type", "application/json")
    def submitDataFeedValue(values: Collection[DataFeedValue]): R[TxResult[Unit]]

    //    // Export data
    //
    //    def exportData: R[String]

    // Smart contract template

    //def registerSmartContractTemplate(feeds: Collection[FeedType], description: Collection[DescriptionField], attributes: Collection[FieldMeta], stateModel: Collection[FieldMeta], classImplementation: String): R[TxResult[SmartContractTemplate]]

    // Smart contract

    @JSExport
    @HttpPost("/create-smart-contract")
    @HttpHeaderValue("Content-Type", "application/json")
    def createSmartContract(
        id: String,
        templateId: String,
        attributes: Collection[String],
        dataFeeds: Collection[String],
        regulators: Collection[RegulatorCapabilities]
    ): R[TxResult[SmartContract]]

    @JSExport
    @HttpPost("/get-smart-contract-regulation")
    @HttpHeaderValue("Content-Type", "application/json")
    def getSmartContractRegulation(id: String): R[SmartContractRegulation]

    @JSExport
    @HttpPost("/approve-smart-contract")
    @HttpHeaderValue("Content-Type", "application/json")
    def approveSmartContract(id: String): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/reject-smart-contract")
    @HttpHeaderValue("Content-Type", "application/json")
    def rejectSmartContract(id: String, reason: String): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/extract-membersignature")
    @HttpHeaderValue("Content-Type", "application/json")
    def extractMemberSignature(signature: Bytes): R[MemberSignature]

    @JSExport
    @HttpPost("/extract-deal-extra-data")
    @HttpHeaderValue("Content-Type", "application/json")
    def extractDealExtraData(deal: Deal): R[DealExtraData]

    @JSExport
    @HttpPost("/extract-burn-extra-data")
    @HttpHeaderValue("Content-Type", "application/json")
    def extractBurnExtraData(burn: BurnRequest): R[BurnExtraData]

    @JSExport
    @HttpPost("/extract-issue-extra-data")
    @HttpHeaderValue("Content-Type", "application/json")
    def extractIssueTokenExtraData(issue: IssueToken): R[IssueExtraData]

    @JSExport
    @HttpPost("/extract-genericmessage")
    @HttpHeaderValue("Content-Type", "application/json")
    def extractGenericMessage(request: MessageRequest): R[GenericMessage]

    @JSExport
    @HttpPost("/utils/decrypt-text")
    @HttpHeaderValue("Content-Type", "application/json")
    def decryptText(encryptedString: String): R[String]

    @JSExport
    @HttpPost("/utils/encrypt-text")
    @HttpHeaderValue("Content-Type", "application/json")
    def encryptText(data: String, members: Collection[String]): R[String]


    //Token Proof

    //    def getProof(tokenId: String): R[Collection[BlockInfo]]

    //Messaging

    // askForAddress: proposeToken(id,TokenTypeRequest.Any,Collection.Empty, Collection.empty)

    @JSExport
    @HttpPost("/propose-token")
    @HttpHeaderValue("Content-Type", "application/json")
    def proposeToken(to: String, tokenType: String, tokenContent: Collection[String], extraData: Bytes): R[String]

    @JSExport
    @HttpPost("/accept-transfer-proposal")
    @HttpHeaderValue("Content-Type", "application/json")
    def acceptTransferProposal(operationId: String, extraData: Bytes): R[Bytes]

    @JSExport
    @HttpPost("/request-token")
    @HttpHeaderValue("Content-Type", "application/json")
    def requestToken(from: String, address: Bytes, tokenType: String, tokenContent: Collection[String], extraData: Bytes): R[Bytes]

    @JSExport
    @HttpPost("/request-token")
    @HttpHeaderValue("Content-Type", "application/json")
    def requestToken(from: String, tokenType: String, tokenContent: Collection[String], extraData: Bytes): R[Bytes]

    //    def requestIssue(tokenType: Bytes, content: Collection[String], extraData: Bytes): R[String]

    @JSExport
    @HttpPost("/accept-token-request")
    @HttpHeaderValue("Content-Type", "application/json")
    def acceptTokenRequest(operationId: String, tokenIds: Collection[String], extraData: Bytes): R[TxResult[Unit]]

    // Messaging
    @JSExport
    @HttpPost("/message/send")
    @HttpHeaderValue("Content-Type", "application/json")
    def sendGenericMessage(to: String, systemId: Int, messageType: Int, messageData: Bytes): R[TxResult[Unit]]


    //Address book

    @JSExport
    @HttpPost("/get-identity")
    @HttpHeaderValue("Content-Type", "application/json")
    def getIdentity: R[String]

    @JSExport
    @HttpPost("/member")
    @HttpHeaderValue("Content-Type", "application/json")
    def registerMember(member: MemberInformation): R[TxResult[String]]

    @JSExport
    @HttpPost("/member/update")
    @HttpHeaderValue("Content-Type", "application/json")
    def updateMember(update: MemberInformation): R[TxResult[Unit]]

    // TokenId

    @JSExport
    @HttpGet("/create-token-id")
    @HttpHeaderValue("Content-Type", "application/json")
    def createTokenId(typeId: String): R[String]

    @JSExport
    @HttpPost("/get-wallet-information")
    @HttpHeaderValue("Content-Type", "application/json")
    def getWalletInformation: R[WalletIdentity]

    // Profiles

    @JSExport
    @HttpPost("/create-profile")
    @HttpHeaderValue("Content-Type", "application/json")
    def createProfile(profile: CreateProfileInfo): R[TxResult[Profile]]

    @JSExport
    @HttpPost("/update-profile")
    @HttpHeaderValue("Content-Type", "application/json")
    def updateProfile(profile: Profile): R[TxResult[Profile]]

    @JSExport
    @HttpPost("/list-profiles")
    @HttpHeaderValue("Content-Type", "application/json")
    def listProfiles: R[Collection[Profile]]

    @JSExport
    @HttpPost("/link-tokens-to-profile")
    @HttpHeaderValue("Content-Type", "application/json")
    def linkTokensToProfile(profileId: String, tokenIds: Collection[String]): R[TxResult[Unit]]

    def unlinkTokensFromProfile(profileId: String, tokenIds: Collection[String]): R[TxResult[Unit]]

    //Regulatory section

    @JSExport
    @HttpPost("/freeze-token")
    @HttpHeaderValue("Content-Type", "application/json")
    def freezeToken(requests: Collection[FreezeInfo]): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/unfreeze-token")
    @HttpHeaderValue("Content-Type", "application/json")
    def unfreezeToken(requests: Collection[FreezeInfo]): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/regulatory-burn-token")
    @HttpHeaderValue("Content-Type", "application/json")
    def regulatoryBurnToken(request: RegulatoryBurnTokenRequest): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/regulatory-transfer")
    @HttpHeaderValue("Content-Type", "application/json")
    def regulatoryTransfer(memberId: String, dealId: String, tokenIds: Collection[String], to: Bytes): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/regulatory-change-token")
    @HttpHeaderValue("Content-Type", "application/json")
    def regulatoryChangeToken(tokenId: String, amounts: Collection[String]): R[TxResult[TokenChangeResponse]]

    // On-boarding

    @JSExport
    @HttpPost("/list-endorsements")
    @HttpHeaderValue("Content-Type", "application/json")
    def listEndorsements: R[Collection[SignedEndorsement]]

    @JSExport
    @HttpPost("/request-endorsement")
    @HttpHeaderValue("Content-Type", "application/json")
    def requestEndorsement(regulatorId: String, data: Bytes): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/endorse-member")
    @HttpHeaderValue("Content-Type", "application/json")
    def endorseMember(memberId: String, certificate: Bytes): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/endorse-member-public")
    @HttpHeaderValue("Content-Type", "application/json")
    def endorseMemberPublic(memberId: String, kindId: String, data: Bytes): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/revoke-public-endorsement")
    @HttpHeaderValue("Content-Type", "application/json")
    def revokePublicEndorsement(memberId: String, kindId: String): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/reject-endorsement")
    @HttpHeaderValue("Content-Type", "application/json")
    def rejectEndorsement(memberId: String, reason: String): R[TxResult[Unit]]

    // Regulation

    @JSExport
    @HttpPost("/approve-transaction")
    @HttpHeaderValue("Content-Type", "application/json")
    def approveTransaction(transactionId: String): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/addnotice-transaction")
    @HttpHeaderValue("Content-Type", "application/json")
    def addNotice(transactionId: String, message: String, membersIds: Collection[String]): R[TxResult[Unit]]

    @JSExport
    @HttpPost("/reject-transaction")
    @HttpHeaderValue("Content-Type", "application/json")
    def rejectTransaction(transactionId: String, reason: String, membersIds: Collection[String]): R[TxResult[Unit]]

    // Operations

    @JSExport
    @HttpPost("/list-operations")
    @HttpHeaderValue("Content-Type", "application/json")
    def listOperations: R[Collection[Operation]]

    @JSExport
    @HttpPost("/get-operation-by-id")
    @HttpHeaderValue("Content-Type", "application/json")
    def getOperation(operationId: String): R[Operation]

    @JSExport
    @HttpPost("/get-operation-details")
    @HttpHeaderValue("Content-Type", "application/json")
    def getOperationDetails(state: OperationState): R[OperationData]

    @JSExport
    @HttpGet("/registered")
    @HttpHeaderValue("Content-Type", "application/json")
    def isRegistered: R[Boolean]

    // Messages

    @JSExport
    @HttpGet("/message/list")
    @HttpHeaderValue("Content-Type", "application/json")
    def listMessages: R[Collection[MessageRequest]]

    // Wallet Events

    @JSExport
    @HttpPost("/events")
    @HttpHeaderValue("Content-Type", "application/json")
    def events(block: BigInt, skipSignaturesCheck: Boolean): R[WalletEvents]


    @JSExport
    @HttpGet("/version/wallet")
    @HttpHeaderValue("Content-Type", "application/json")
    def walletVersion: R[String]

    //    @JSExport
    //    @HttpGet("/registration-block")
    //    def registrationBlock: R[Long]
    //
    //    @JSExport
    //    @HttpGet("/latest-block")
    //    def latestBlockNumber: R[Long]

}
