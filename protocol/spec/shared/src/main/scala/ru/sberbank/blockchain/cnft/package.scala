package ru.sberbank.blockchain

import ru.sberbank.blockchain.cnft.model._
import scalapb.GeneratedMessageCompanion
import upickle.core.Visitor
import upickle.default._

import java.nio.charset.StandardCharsets

/**
 * @author Alexey Polubelov
 */
package object cnft {

    private val OperationDataSerializersByName: Map[String, GeneratedMessageCompanion[_ <: OperationData]] = Map(
        classOf[IssueTokenRequest].getSimpleName -> IssueTokenRequest,
        classOf[PendingIssue].getSimpleName -> PendingIssue,
        classOf[DealRequest].getSimpleName -> DealRequest,
        classOf[PendingDeal].getSimpleName -> PendingDeal,
        classOf[PendingBurn].getSimpleName -> PendingBurn,
        classOf[BurnRequest].getSimpleName -> BurnRequest,
        classOf[BurnResponse].getSimpleName -> BurnResponse,
        classOf[TransferProposal].getSimpleName -> TransferProposal,
        classOf[TokenRequest].getSimpleName -> TokenRequest,
        classOf[RegulatorTransferRequest].getSimpleName -> RegulatorTransferRequest
    )

    implicit val OperationDataRW: ReadWriter[OperationData] = new ReadWriter[OperationData] with SimpleReader[OperationData] {

        override def expectedMsg: String = "expected string"

        override def visitString(s: CharSequence, index: Int): OperationData = {
            val Array(name, data) = s.toString.split(':')
            val ser = OperationDataSerializersByName.getOrElse(name, throw new Exception("Unsupported OperationData type"))
            ser.parseFrom(
                java.util.Base64.getDecoder.decode(
                    data.getBytes(StandardCharsets.UTF_8)
                )
            )
        }


        override def write0[V](out: Visitor[_, V], v: OperationData): V = {
            out.visitString(
                v.getClass.getSimpleName + ":" +
                    new String(
                        java.util.Base64.getEncoder.encode(v.toByteArray),
                        StandardCharsets.UTF_8
                    ),
                -1
            )
        }
    }

    implicit val TokenIdRW: ReadWriter[TokenId] = macroRW

    //    implicit val ApproveEndorsementRequestRW: ReadWriter[ApproveEndorsementRequest] = macroRW
    //    implicit val SignedApproveEndorsementRequestRW: ReadWriter[SignedApproveEndorsementRequest] = macroRW
    implicit val SignedEndorsementRW: ReadWriter[SignedEndorsement] = macroRW
    implicit val EndorsementRW: ReadWriter[Endorsement] = macroRW

    implicit val SignedPublicEndorsementRW: ReadWriter[SignedPublicEndorsement] = macroRW
    implicit val PublicEndorsementRW: ReadWriter[PublicEndorsement] = macroRW

    implicit val FieldMetaRW: ReadWriter[FieldMeta] = macroRW

    implicit val SmartContractRW: ReadWriter[SmartContract] = macroRW
    implicit val SignedSmartContractRW: ReadWriter[SignedSmartContract] = macroRW
    implicit val SmartContractTemplateRW: ReadWriter[SmartContractTemplate] = macroRW
    implicit val RequiredDealFieldsRW: ReadWriter[RequiredDealFields] = macroRW
    implicit val SmartContractAcceptedDealTokenRW: ReadWriter[AcceptedDeal] = macroRW
    implicit val SmartContractAcceptedTokenRW: ReadWriter[AcceptedToken] = macroRW
    implicit val SCRegulationRequestRW: ReadWriter[SCRegulationRequest] = macroRW
    implicit val SignedSCRegulationRequestRW: ReadWriter[SignedSCRegulationRequest] = macroRW
    implicit val SmartContractStateRW: ReadWriter[SmartContractState] = macroRW
    implicit val SmartContractRegulationRW: ReadWriter[SmartContractRegulation] = macroRW
    implicit val ApproveRW: ReadWriter[Approve] = macroRW

    implicit val XFeedTypeRW: ReadWriter[FeedType] = macroRW
    // TODO
    implicit val XDataFeedRW: ReadWriter[DataFeed] = macroRW
    implicit val XSignedDataFeedRW: ReadWriter[SignedDataFeed] = macroRW
    implicit val XFeedValueRequestRW: ReadWriter[FeedValueRequest] = macroRW
    implicit val XDataFeedValueRW: ReadWriter[DataFeedValue] = macroRW

    implicit val GenesRW: ReadWriter[DNA] = macroRW
    implicit val GeneRW: ReadWriter[Gene] = macroRW
    implicit val TokenTypeRW: ReadWriter[TokenType] = macroRW
    implicit val TokenTypeMetaRW: ReadWriter[TokenTypeMeta] = macroRW

    implicit val RequestActorRW: ReadWriter[RequestActor] = macroRW
    implicit val OwnerSignatureRW: ReadWriter[OwnerSignature] = macroRW
    implicit val MembersSignatureRW: ReadWriter[MemberSignature] = macroRW

    implicit val PendingAcceptRW: ReadWriter[PendingAccept] = macroRW
    implicit val AcceptTokenRequestRW: ReadWriter[AcceptTokenRequest] = macroRW
    implicit val DescriptionFieldRW: ReadWriter[DescriptionField] = macroRW
    implicit val OperationEffectRW: ReadWriter[OperationEffect] = macroRW
    implicit val PendingBurnRW: ReadWriter[PendingBurn] = macroRW
    implicit val TokenChangeResponseRW: ReadWriter[TokenChangeResponse] = macroRW
    implicit val TokenAddedRW: ReadWriter[TokenAdded] = macroRW

    implicit val PendingIssueRW: ReadWriter[PendingIssue] = macroRW
    implicit val IssueRequestRW: ReadWriter[IssueRequest] = macroRW

    implicit val TokenOwnerRW: ReadWriter[TokenOwner] = macroRW
    implicit val TokenChangeRequestRW: ReadWriter[TokenChangeRequest] = macroRW
    implicit val TokenChangeRequestSmartContractRW: ReadWriter[TokenChangeRequestSmartContract] = macroRW
    implicit val TokenToCreateRW: ReadWriter[TokenToCreate] = macroRW
    implicit val SignedTokenChangeRequestRW: ReadWriter[SignedTokenChangeRequest] = macroRW
    implicit val RegulatorySignedTokenChangeRequestRW: ReadWriter[RegulatorSignedTokenChangeRequest] = macroRW

    implicit val TokenMergeResponseRW: ReadWriter[TokenMergeResponse] = macroRW
    implicit val SingedTokenMergeRequestRW: ReadWriter[SignedTokenMergeRequest] = macroRW
    implicit val TokenMergeRequestRW: ReadWriter[TokenMergeRequest] = macroRW

    implicit val RegulatoryBurnRequestRW: ReadWriter[RegulatorBurnRequest] = macroRW
    implicit val TokenFreezeRequestRW: ReadWriter[TokenFreezeRequest] = macroRW
    implicit val RegulatoryTransferRW: ReadWriter[RegulatorTransferRequest] = macroRW

    implicit val IssueTokenRW: ReadWriter[IssueToken] = macroRW
    implicit val IssueTokensRW: ReadWriter[IssueTokens] = macroRW
    implicit val IssueTokenRequestRW: ReadWriter[IssueTokenRequest] = macroRW
    implicit val IssueExtraDataRW: ReadWriter[IssueExtraData] = macroRW

    // burn request
    implicit val SignedBurnRequestRequestRW: ReadWriter[SignedBurnRequest] = macroRW
    implicit val BurnRequestRW: ReadWriter[BurnRequest] = macroRW
    implicit val BurnExtraDataRW: ReadWriter[BurnExtraData] = macroRW
    // burn response
    implicit val BurnResponseRW: ReadWriter[BurnResponse] = macroRW
    implicit val BurntTokenDataRW: ReadWriter[BurntTokenData] = macroRW

    implicit val RelatedDealReferenceRW: ReadWriter[RelatedDealReference] = macroRW

    implicit val DealRW: ReadWriter[Deal] = macroRW

    implicit val DealRequestRW: ReadWriter[DealRequest] = macroRW
    implicit val DealExtraDataRW: ReadWriter[DealExtraData] = macroRW
    implicit val DealMemberRW: ReadWriter[DealMember] = macroRW
    implicit val LegInfoRW: ReadWriter[LegInfo] = macroRW

    implicit val OfferRW: ReadWriter[Offer] = macroRW
    implicit val PutOfferRequestRW: ReadWriter[PutOfferRequest] = macroRW
    implicit val ApplyForOfferRequestRW: ReadWriter[ApplyForOffer] = macroRW
    implicit val ApproveOfferRequestRW: ReadWriter[ApproveOffer] = macroRW

    implicit val SignedTokenTypeRegistrationRW: ReadWriter[SignedTokenTypeRegistration] = macroRW
    implicit val TokenTypeRegistrationRW: ReadWriter[TokenTypeRegistrationRequest] = macroRW
    implicit val TokenDescriptionRW: ReadWriter[TokenDescription] = macroRW
    implicit val TokenFieldValueRW: ReadWriter[TokenFieldValue] = macroRW

    implicit val XMemberInformationRW: ReadWriter[MemberInformation] = macroRW
    implicit val memberRegisterRequest: ReadWriter[RegisterMemberRequest] = macroRW
    implicit val UpdateMemberInformationRequestRW: ReadWriter[UpdateMemberInformationRequest] = macroRW

    implicit val MessageRequestRW: ReadWriter[MessageRequest] = macroRW
    implicit val MessageRW: ReadWriter[Message] = macroRW

    //
    //
    //

    implicit val WalletTokenRW: ReadWriter[WalletToken] = macroRW
    implicit val TokenContentRW: ReadWriter[TokenContent] = macroRW
    //    implicit val RichBurnRequestRW: ReadWriter[RichBurnRequest] = macroRW

    implicit val TokenRestrictionsRW: ReadWriter[TokenRestrictions] = macroRW
    implicit val RestrictionRW: ReadWriter[Restriction] = macroRW
    implicit val RegulatorCapabilitiesRW: ReadWriter[RegulatorCapabilities] = macroRW

    implicit val DealLegRW: ReadWriter[DealLeg] = macroRW
    implicit val PendingDealRW: ReadWriter[PendingDeal] = macroRW
    implicit val RegulatorApprovalRW: ReadWriter[RegulatorApproval] = macroRW
    implicit val TXRegulationRequestRW: ReadWriter[TXRegulationRequest] = macroRW
    implicit val SignedTXRegulationRequestRW: ReadWriter[SignedTXRegulationRequest] = macroRW
    implicit val TXRegulationNotificationRW: ReadWriter[TXRegulationNotification] = macroRW
    implicit val SignedTXRegulationNotificationRW: ReadWriter[SignedTXRegulationNotification] = macroRW

    implicit val PendingOperationRW: ReadWriter[PendingOperation] = macroRW

    implicit val ProfileRW: ReadWriter[Profile] = macroRW

    implicit val ProfileTokensRW: ReadWriter[ProfileTokens] = macroRW

    implicit val TokenProfileInfoRW: ReadWriter[TokenProfileInfo] = macroRW

    implicit val TokenTypeFilterRW: ReadWriter[TokenTypeFilter] = macroRW

    implicit val EndorsementRequestRW: ReadWriter[EndorsementRequest] = macroRW
    implicit val TransferProposalRW: ReadWriter[TransferProposal] = macroRW
    implicit val GenericMessageRW: ReadWriter[GenericMessage] = macroRW
    implicit val SignedRejectEndorsementRequestRW: ReadWriter[SignedRejectEndorsementRequest] = macroRW
    implicit val TokenRequestRW: ReadWriter[TokenRequest] = macroRW
    implicit val RejectEndorsementRequestRW: ReadWriter[RejectEndorsementRequest] = macroRW
    implicit val BurntTokenRW: ReadWriter[BurntToken] = macroRW

    implicit val OperationRW: ReadWriter[Operation] = macroRW
    implicit val OperationStateRW: ReadWriter[OperationState] = macroRW
    implicit val OperationHistoryRW: ReadWriter[OperationHistory] = macroRW

    implicit val MemberMessagesRW: ReadWriter[MemberMessages] = macroRW
    implicit val PlatformVersionRW: ReadWriter[PlatformVersion] = macroRW

    val TokenIdFieldsSeparator = ':'

    val RelatedDealReferenceEmpty: RelatedDealReference = RelatedDealReference(-1, -1)

    val CurrentPlatformVersion = "4.2.12"

    val SupportedMessagesIndex: Map[GeneratedMessageCompanion[_], Int] =
        Map(
            TransferProposal -> 1,
            TokenRequest -> 2,
            IssueRequest -> 3,
            EndorsementRequest -> 4,
            SignedRejectEndorsementRequest -> 5,
            ApplyForOffer -> 6,
            ApproveOffer -> 7
        )
}
