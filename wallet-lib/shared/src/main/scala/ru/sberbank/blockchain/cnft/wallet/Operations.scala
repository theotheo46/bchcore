package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.SupportedMessagesIndex
import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{asByteArray, collectionFromIterable}
import ru.sberbank.blockchain.cnft.model.{GenericMessage, MessageRequest, Operation, OperationData, OperationState, OperationStatus, SignedPayload, TokenRequest, TransferProposal}
import ru.sberbank.blockchain.cnft.wallet.walletmodel.OperationData

import java.time.Instant
import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
class Operations[R[+_]](wallet: CNFTWalletInternal[R]) {

    import wallet._

    def listOperations: R[Collection[Operation]] = {
        for {
            // get all operations from messages
            myMessages <- chain.listMessages(id.id)
            fromMessages <- chain.listMessagesFrom(id.id)
            injectedMsgsOperations <- getOperationsFromMessages(myMessages ++ fromMessages)

            //
            allOperations <- chain.listOperations
            myOperationsHistory <- allOperations.toSeq.filterR { history =>
                for {
                    isMy <- history.addresses.toSeq.findR(amIAnOwner).map(_.nonEmpty)
                    foundRegulator = history.regulators.contains(id.id)
                    foundIssuer = history.issuers.contains(id.id)
                } yield isMy || foundRegulator || foundIssuer
            }
            myOperations = myOperationsHistory.map(_.data)

        } yield
            collectionFromIterable(
                (injectedMsgsOperations ++ myOperations)
                    .groupBy(_.operationId)
                    .map(o => mergeOperations(o._1, o._2))
            )
    }

    def getOperation(operationId: String): R[Option[Operation]] = {
        for {
            msgs <- chain.listMessages(id.id)
            myMsgs <- getOperationsFromMessages(msgs).map(_.filter(_.operationId == operationId))
            op <- chain.getOperation(operationId).map(h => Collection(h.data)).recover(_ => R(Collection.empty[Operation]))
        } yield {
            (myMsgs ++ op)
                .groupBy(_.operationId)
                .get(operationId).map(ops => mergeOperations(operationId, ops))
        }
    }

    private def getOperationsFromMessages(messages: Collection[MessageRequest]): R[Collection[Operation]] =
        messages.toSeq.mapR { msg =>
            crypto.encryptionOperations
                .decrypt(msg.message.payload, id.encryptionKey)
                .flatMap(bytes => R(SignedPayload.parseFrom(asByteArray(bytes))))
                .map { signedPayload =>
                    val genericMessage = GenericMessage.parseFrom(asByteArray(signedPayload.data))
                    if (genericMessage.systemId == 0 && genericMessage.messageType == SupportedMessagesIndex(TransferProposal)) {
                        val proposal = TransferProposal.parseFrom(asByteArray(genericMessage.data))
                        Collection(
                            Operation(
                                operationId = proposal.operationId,
                                history = Collection(
                                    OperationState(
                                        timestamp = proposal.timestamp,
                                        state = OperationStatus.TransferProposed,
                                        data = proposal.toBytes,
                                        block = "",
                                        txId = msg.txId
                                    )
                                )
                            )
                        )
                    } else if (genericMessage.systemId == 0 && genericMessage.messageType == SupportedMessagesIndex(TokenRequest)) {
                        val request = TokenRequest.parseFrom(asByteArray(genericMessage.data))
                        Collection(
                            Operation(
                                operationId = request.operationId,
                                history = Collection(
                                    OperationState(
                                        timestamp = request.timestamp,
                                        state = OperationStatus.TokenRequested,
                                        data = request.toBytes,
                                        block = "",
                                        txId = msg.txId
                                    )
                                )
                            )
                        )
                    } else Collection.empty[Operation]
                }.recover(_ => R(Collection.empty[Operation]))
        }.map(collectOperations)

    private def collectOperations(ops: Iterable[Collection[Operation]]): Collection[Operation] = {
        collectionFromIterable(
            ops.flatten
                .groupBy(_.operationId)
                .map(o => mergeOperations(o._1, o._2))
        )
    }

    private def mergeOperations(id: String, ops: Iterable[Operation]): Operation =
        Operation(
            id,
            collectionFromIterable(ops.flatMap(_.history))
                .sortWith { (state1, state2) =>
                    Instant.parse(state1.timestamp).compareTo(Instant.parse(state2.timestamp)) <= 0
                }
        )

    def getOperationDetails(state: OperationState): R[OperationData] =
        R(OperationData.from(state.state, state.data))

}