package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.gate.model.BlockEvent
import ru.sberbank.blockchain.cnft.model.{EndorsementRequest, MessageRequest, SignedRejectEndorsementRequest, TokenRequest, TransferProposal}
import ru.sberbank.blockchain.cnft.wallet.spec.IncomingMessage
import scalapb.GeneratedMessage

/**
 * @author Alexey Polubelov
 */
case class MessagesAggregation(
    transfersProposed: Collection[IncomingMessage[BlockEvent[TransferProposal]]],
    tokensRequested: Collection[IncomingMessage[BlockEvent[TokenRequest]]],
    endorsementsRequested: Collection[IncomingMessage[BlockEvent[EndorsementRequest]]],
    endorsementsRejected: Collection[IncomingMessage[BlockEvent[SignedRejectEndorsementRequest]]],
    genericMessages: Collection[IncomingMessage[BlockEvent[MessageRequest]]]
)

object MessagesAggregation {

    def empty: MessagesAggregation =
        MessagesAggregation(Collection.empty, Collection.empty, Collection.empty, Collection.empty, Collection.empty)


    trait AppendFor[A, M <: GeneratedMessage] {
        type M2IM = M => IncomingMessage[BlockEvent[M]]

        def append(a: A, m: M)(wrap: M2IM): A
    }

    implicit val AppenderForTransferProposal: AppendFor[MessagesAggregation, TransferProposal] =
        new AppendFor[MessagesAggregation, TransferProposal] {
            override def append(a: MessagesAggregation, m: TransferProposal)(wrap: M2IM): MessagesAggregation =
                a.copy(transfersProposed = a.transfersProposed :+ wrap(m))
        }

    implicit val AppenderForTokensRequests: AppendFor[MessagesAggregation, TokenRequest] =
        new AppendFor[MessagesAggregation, TokenRequest] {
            override def append(a: MessagesAggregation, m: TokenRequest)(wrap: M2IM): MessagesAggregation =
                a.copy(tokensRequested = a.tokensRequested :+ wrap(m))
        }

    implicit val AppenderForEndorsementsRequests: AppendFor[MessagesAggregation, EndorsementRequest] =
        new AppendFor[MessagesAggregation, EndorsementRequest] {
            override def append(a: MessagesAggregation, m: EndorsementRequest)(wrap: M2IM): MessagesAggregation =
                a.copy(endorsementsRequested = a.endorsementsRequested :+ wrap(m))
        }

    implicit val AppenderForEndorsementsRejects: AppendFor[MessagesAggregation, SignedRejectEndorsementRequest] =
        new AppendFor[MessagesAggregation, SignedRejectEndorsementRequest] {
            override def append(a: MessagesAggregation, m: SignedRejectEndorsementRequest)(wrap: M2IM): MessagesAggregation =
                a.copy(endorsementsRejected = a.endorsementsRejected :+ wrap(m))
        }

}
