package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.SupportedMessagesIndex
import ru.sberbank.blockchain.cnft.common.types.Bytes
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{ROps, asByteArray}
import ru.sberbank.blockchain.cnft.gate.model.{BlockEvent, BlockEvents}
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.model.{EndorsementRequest, GenericMessage, MemberInformation, MessageRequest, SignedPayload, SignedRejectEndorsementRequest, TokenRequest, TransferProposal}
import ru.sberbank.blockchain.cnft.wallet.blocks.MessagesAggregation.AppendFor
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec.IncomingMessage
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import scala.language.higherKinds

class MessagesExtractor[R[+_]](
    id: WalletIdentity,
    val crypto: WalletCrypto[R],
    val chain: ChainServiceSpec[R],
)(implicit
    val R: ROps[R]
) {

    type MPF = (MessagesAggregation, MemberInformation, BlockEvent[MessageRequest], Array[Byte]) => MessagesAggregation

    private implicit def processorFor[M <: GeneratedMessage](mc: GeneratedMessageCompanion[M])
        (implicit appendFor: AppendFor[MessagesAggregation, M]): (Int, MPF) =
        SupportedMessagesIndex(mc) -> { (a, mi, e, bytes) =>
            val m = mc.parseFrom(bytes)
            appendFor.append(a, m) { m => IncomingMessage(mi, BlockEvent(m, e.txId, e.blockNumber)) }
        }


    private val SupportedMessagesMPF = Map[Int, MPF](
        TransferProposal,
        TokenRequest,
        EndorsementRequest,
        SignedRejectEndorsementRequest
    )


    def extract(block: BlockEvents, skipSignaturesCheck: Boolean): R[MessagesAggregation] =
        for {
            result <-
                block.messages
                    .filter(_.event.message.to == id.id).toSeq
                    .foldLeftR(MessagesAggregation.empty) {
                        case (resultOfAggregation, blockEvent) =>
                            for {
                                decryptedPayload <- decryptMessage(blockEvent.event.message.payload)
                                    .map(Some(_))
                                    .recover(_ => R(None))
                                signedPayload <- decryptedPayload match {
                                    case Some(payload) =>
                                        R(Some(SignedPayload.parseFrom(asByteArray(payload))))
                                    case None =>
                                        R(None)
                                }
                                maybeMember <- chain.getMember(blockEvent.event.message.from)
                                    .map(member => Some(member))
                                    .recover(_ => R(None))
                                result <-
                                    (maybeMember, signedPayload) match {
                                        case (Some(member), Some(payload)) => {
                                            if (!skipSignaturesCheck) {
                                                for {
                                                    sender <- chain.getMember(member.id)
                                                    signatureOk <-
                                                        crypto.identityOperations.verifySignature(
                                                            sender.signingPublic,
                                                            payload.data,
                                                            payload.signature
                                                        )
                                                    _ <- R.expect(signatureOk, s"wrong message signature")
                                                } yield ()
                                            } else {
                                                R(())
                                            }
                                        }.map { _ =>
                                            val genericMessage = GenericMessage.parseFrom(asByteArray(payload.data))
                                            if (genericMessage.systemId == 0) {
                                                SupportedMessagesMPF.get(genericMessage.messageType)
                                                    .map(_(resultOfAggregation, member, blockEvent, asByteArray(genericMessage.data)))
                                                    .getOrElse(resultOfAggregation)
                                            } else {
                                                resultOfAggregation.copy(
                                                    genericMessages = resultOfAggregation.genericMessages :+
                                                        IncomingMessage(
                                                            member,
                                                            BlockEvent(blockEvent.event, blockEvent.txId, blockEvent.blockNumber)
                                                        )
                                                )
                                            }
                                        }
                                        case _ => R(resultOfAggregation)
                                    }
                            } yield result
                    }
        } yield result

    private def decryptMessage(message: Bytes): R[Bytes] =
        crypto.encryptionOperations.decrypt(message, id.encryptionKey)

}
