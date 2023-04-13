package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{Collection, ROps, asByteArray, collectionFromIterable}
import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.model.{OwnerType, Signatures, TokenId}
import ru.sberbank.blockchain.cnft.wallet.WalletCommonOps
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec.OperationsListEvents
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

import scala.language.higherKinds

class OperationsListEventsExtractor[R[+_]](
    val id: WalletIdentity,
    val crypto: WalletCrypto[R],
    val chain: ChainServiceSpec[R],
)(implicit
    val R: ROps[R]
) extends WalletCommonOps[R] {

    def extract(block: BlockEvents, tokensWithOperations: TokensWithOperations): R[OperationsListEvents] = {
        val exchangedOperations = tokensWithOperations.tokensExchangedWithOperations.flatMap { case (added, burnt) => added.map { case (_, addedOperation) => addedOperation } ++ burnt.map { case (_, burntOperation) => burntOperation } }
        val burntOwnedTokensOperations = tokensWithOperations.burntOwnedTokensWithOperations.map { case (_, burntOwnedTokensOperations) => burntOwnedTokensOperations }
        val issuedForUsOperations = tokensWithOperations.tokensIssuedWithOperations.flatMap { case (_, operation) => operation }

        for {
            pendingDealOperations <-
                block.pendingDeals.toSeq
                    .filterR { transactionEvent =>
                        transactionEvent.event.deal.deal.legs.toSeq.findR { leg =>
                            for {
                                iamAnOwner <- amIAnOwner(leg.previousOwner)
                                iamNewOwner <- leg.newOwner.ownerType match {
                                    case OwnerType.Signatures => {
                                        for {
                                            keys <- R(Signatures.parseFrom(asByteArray(leg.newOwner.address))).map(_.keys)
                                            result <-
                                                crypto.addressOperations
                                                    .findKeysByPublic(keys)
                                                    .map(_.nonEmpty)
                                        } yield result
                                    }.recover(_ => R(false))

                                    case _ => R(false)
                                }
                            } yield iamAnOwner || iamNewOwner
                        }.map(_.nonEmpty)
                    }
                    .map {
                        _.map { pendingTransactionEvent =>
                            pendingTransactionEvent.event.deal.deal.operationId
                        }
                    }

            pendingBurnOperations <-
                block.pendingBurns.toSeq
                    .filterR(pending =>
                        //FIXME: obtain the owner in a right way
                        pending.event.owners.toSeq
                            .findR { owner => amIAnOwner(owner)
                            }.map(_.nonEmpty)
                    )
                    .map {
                        _.map(pendingTransactionEvent => pendingTransactionEvent.event.burnRequest.request.operationId)
                    }

            pendingOutgoingAcceptOperations <-
                block.pendingAccepts.toSeq.mapR { pendingTransactionEvent =>
                    val pendingAccept = pendingTransactionEvent.event
                    pendingAccept.dealRequest.deal.legs.toSeq.mapR { leg =>
                        val owner = leg.previousOwner
                        for {
                            keys <- owner.ownerType match {
                                case OwnerType.Signatures =>
                                    R(Signatures.parseFrom(asByteArray(owner.address)))
                                        .flatMap(signatures => crypto.addressOperations.findKeysByPublic(signatures.keys))
                                case _ =>
                                    R(Collection.empty[KeyIdentifier])
                            }
                            result <-
                                if (keys.nonEmpty) {
                                    R(Some(pendingAccept.dealRequest.deal.operationId))
                                } else
                                    R(None)
                        } yield result
                    }
                }.map(_.flatten).map(_.flatten)

            pendingIncomingAcceptOperations <-
                block.pendingAccepts.toSeq.mapR { pendingTransactionEvent =>
                    val pendingAccept = pendingTransactionEvent.event
                    pendingAccept.dealRequest.deal.legs.toSeq.mapR { leg =>
                        leg.newOwner.ownerType match {
                            case OwnerType.Signatures =>
                                for {
                                    signatures <- R(Signatures.parseFrom(asByteArray(leg.newOwner.address)))
                                    keys <- crypto.addressOperations.findKeysByPublic(signatures.keys)
                                    result <-
                                        if (keys.nonEmpty) {
                                            R(Some(pendingAccept.dealRequest.deal.operationId))
                                        } else R(None)

                                } yield result
                            case _ => R(None)
                        }
                    }
                }.map(_.flatten).map(_.flatten)

            pendingIssueOperations <-
                block.pendingIssues.toSeq
                    .filterR { transactionEvent =>
                        val pending = transactionEvent.event
                        for {
                            isOwner <-
                                pending.request.issue.tokens.toSeq.findR { issue =>
                                    issue.owner.ownerType match {
                                        case OwnerType.Signatures => {
                                            for {
                                                keys <-
                                                    R(Signatures.parseFrom(asByteArray(issue.owner.address)))
                                                        .map(_.keys)
                                                result <-
                                                    crypto.addressOperations
                                                        .findKeysByPublic(keys)
                                                        .map(_.nonEmpty)
                                            } yield result
                                        }.recover(_ => R(false))
                                        case _ => R(false)
                                    }
                                }.map(_.nonEmpty)
                            isIssuer <-
                                pending.request.issue.tokens.toSeq.findR { issue =>
                                    TokenId.from(issue.tokenId).map(_.typeId).flatMap { tokenType =>
                                        chain.getTokenType(tokenType)
                                            .map(theType => theType.issuerId == id.id)
                                    }
                                }.map(_.nonEmpty)
                        } yield isOwner || isIssuer
                    }
                    .map {
                        _.map { pendingTransactionEvent =>
                            pendingTransactionEvent.event.request.issue.operationId
                        }
                    }

            result = OperationsListEvents(
                exchangeOperations = exchangedOperations,
                burnMyTokenOperations = burntOwnedTokensOperations,
                issueForMeTokenOperations = issuedForUsOperations,
                dealPendingOperations = collectionFromIterable(pendingDealOperations),
                burnPendingOperations = collectionFromIterable(pendingBurnOperations),
                outgoingAcceptPendingOperations = collectionFromIterable(pendingOutgoingAcceptOperations),
                incommingAcceptPendingOperations = collectionFromIterable(pendingIncomingAcceptOperations),
                issuePendingsOperations = collectionFromIterable(pendingIssueOperations)
            )

        } yield result
    }
}