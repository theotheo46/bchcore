package ru.sberbank.blockchain.cnft.engine

import ru.sberbank.blockchain.cnft.common.types.{Collection, Result}
import ru.sberbank.blockchain.cnft.model._

/**
 * @author Alexey Polubelov
 */
trait CNFTStore {

    def saveTokenLinkedOperation(tokenId: String, operationId: String): Unit

    def getTokenLinkedOperation(tokenId: String): Option[String]

    def removeTokenLinkedOperation(tokenId: String): Unit

    // Endorsement
    def saveEndorsement(memberId: String, endorsement: SignedEndorsement): Unit

    def savePublicEndorsement(memberId: String, endorsement: SignedPublicEndorsement): Unit

    def revokePublicEndorsement(memberId: String, endorserId: String, kindId: String): Unit

    def getEndorsements(memberId: String): Array[SignedEndorsement]

    def getPublicEndorsements(memberId: String): Array[SignedPublicEndorsement]

    // Public profiles
    def getProfile(id: String): Option[Profile]

    def listProfiles: Array[Profile]

    def saveProfile(profile: Profile): Unit

    def saveProfileTokens(profileTokens: ProfileTokens): Unit

    def removeProfileTokens(profileId: String): Unit

    def getLinkedToProfileTokenIds(profileId: String): Array[String]

    def saveTokenProfiles(tokenProfiles: TokenProfiles): Unit

    def getTokenProfiles(tokenId: String): Array[TokenProfileInfo]

    //
    // Smart Contracts
    //

    def saveSmartContract(smartContract: SmartContract): Unit

    def getSmartContract(id: String): Option[SmartContract]

    def deleteSmartContract(smartContractAddress: String): Unit

    def listSmartContracts: Array[SmartContract]

    // SC State

    def saveSmartContractState(id: SmartContractState): Unit

    def getSmartContractState(id: String): Option[SmartContractState]

    def deleteSmartContractState(id: String): Unit

    // SC Tokens

    def getSmartContractAcceptedDeals(id: String): Collection[AcceptedDeal]

    def appendSmartContractAcceptedDeal(id: String, acceptedDeal: AcceptedDeal): Result[Unit]

    def saveSmartContractRegulation(smartContractRegulation: SmartContractRegulation): Unit

    def getSmartContractRegulation(id: String): Option[SmartContractRegulation]

    //    def deleteSmartContractRegulation(smartContractAddress: Array[Byte]): Unit

    // Smart Contract types
    //    def getSmartContractTemplate(smartContractTemplateAddress: Array[Byte]): Option[SmartContractTemplate]
    //    def saveSmartContractTemplate(smartContractTemplate: SmartContractTemplate): Unit
    //    def listSmartContractTemplates: Array[SmartContractTemplate]

    // Token Types

    def getTokenType(typeId: String): Option[TokenType]

    def saveTokenType(tokenType: TokenType): Unit

    def listTokenTypes: Array[TokenType]

    def listTokenTypesWithChangeGene(changeGeneId: String, negation: Boolean): Array[TokenType]

    // Tokens
    def createToken(tokenId: String, owner: TokenOwner, body: TokenContent): Result[Unit]

    def tokenExist(tokenId: String): Result[Boolean]

    def getTokenOwner(tokenId: String): Result[TokenOwner]

    def getTokenBody(tokenId: String): Option[TokenContent]

    def updateTokenOwner(tokenId: String, owner: TokenOwner): Result[Unit]

    def deleteToken(tokenId: String): Result[Unit]

    def listTokens: Array[String]

    def listBurntTokens: Array[BurntToken]

    def getBurntToken(tokenId: String): Option[BurntToken]

    def saveBurntToken(token: BurntToken): Unit

    // Token regulation

    def getTokenRestrictions(tokenId: String): Result[TokenRestrictions]

    def addTokenRestriction(tokenId: String, restriction: Restriction): Result[Unit]

    def addTokenRestrictions(tokenId: String, restrictions: TokenRestrictions): Result[Unit]

    // TODO: can the regulator have multiple restrictions?
    def removeTokenRestriction(tokenId: String, regulatorId: String): Result[Unit]

    def clearRestrictions(tokenId: String): Result[Unit]

    // Offers

    def saveOffer(offerId: String, offer: Offer): Unit

    def deleteOffer(offerId: String): Unit

    // MemberInformation
    def getMemberInfo(id: String): Option[MemberInformation]

    def saveMemberInfo(memberInformation: MemberInformation): Unit

    def listMemberInfo: Array[MemberInformation]

    def saveMemberRegistrationBlock(memberId: String, blockNumber: Long): Unit

    def getMemberRegistrationBlock(memberId: String): Option[Long]


    // Data feeds

    def getDataFeed(dataFeedId: String): Option[DataFeed]

    def saveDataFeed(dataFeed: DataFeed): Unit

    def saveDataFeedValue(dataFeedValue: DataFeedValue): Unit

    def getDataFeedValue(dataFeedId: String): Option[DataFeedValue]

    def listDataFeeds: Array[DataFeed]


    def addDataFeedSubscriber(dataFeedId: String, subscriberAddress: String): Unit

    def deleteDataFeedSubscriber(dataFeedId: String, subscriberAddress: String): Unit

    def listDataFeedSubscribers(dataFeedId: String): Result[Array[String]]

    // Messages

    def saveMessages(id: String, msg: MessageRequest): Unit

    def listMessages(to: String): Array[MessageRequest]

    def listMessagesFrom(from: String): Array[MessageRequest]

    // Operations

    def saveOperation(operationId: String, operationData: OperationHistory): Unit

    def getOperation(operationId: String): Option[OperationHistory]

    def listOperations: Array[OperationHistory]

    // Data migration

    def saveCurrentPlatformVersionInfo(v: PlatformVersion): Unit

    def getPlatformVersionInfo: PlatformVersion

}

