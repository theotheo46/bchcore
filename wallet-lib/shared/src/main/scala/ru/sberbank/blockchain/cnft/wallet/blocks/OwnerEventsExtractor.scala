package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.common.types.{Collection, collectionFromIterable}
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{ROps, asByteArray}
import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.model.{OwnerType, Signatures, TokenChangeResponse, TokenId}
import ru.sberbank.blockchain.cnft.wallet.WalletCommonOps
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec.OwnerEvents
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

import scala.language.higherKinds

class OwnerEventsExtractor[R[+_]](
    val id: WalletIdentity,
    val crypto: WalletCrypto[R],
    val chain: ChainServiceSpec[R],
)(implicit
    val R: ROps[R]
) extends WalletCommonOps[R] {

    def extract(block: BlockEvents, messages: MessagesAggregation, tokensWithOperations: TokensWithOperations): R[OwnerEvents] =
        for {
            tokensIssuedByMe <-
                block.tokensIssued.toSeq
                    .map(_.event.issue.tokens)
                    .mapR { tokens => {
                        tokens.toSeq.mapR { token =>
                            TokenId.from(token.tokenId).map(_.typeId).flatMap { tokenTypeId =>
                                chain.getTokenType(tokenTypeId).map { tokenType =>
                                    if (tokenType.issuerId == id.id) Option(token.tokenId)
                                    else None
                                }
                            }
                        }.map(_.flatten)
                    }
                    }.map(_.flatten).map(collectionFromIterable)

            incomingTokens <-
                block.pendingAccepts.toSeq.mapR { pendingAcceptEvent =>
                    val pendingAccept = pendingAcceptEvent.event
                    pendingAccept.dealRequest.deal.legs.toSeq.mapR { leg =>
                        leg.newOwner.ownerType match {
                            case OwnerType.Signatures =>
                                for {
                                    signatures <- R(Signatures.parseFrom(asByteArray(leg.newOwner.address)))
                                    keys <- crypto.addressOperations.findKeysByPublic(signatures.keys)
                                    result <- if (keys.nonEmpty)
                                        R(Option(pendingAccept))
                                    else
                                        R(None)
                                } yield result
                            case _ => R(None)
                        }
                    }
                }
                    .map(_.flatten).map(_.flatten)

//            tokenChangeAdded <-
//                block.tokensChanged.toSeq.mapR { tokenChangeEvent =>
//                    val added = tokenChangeEvent.event.added
//                    added.toSeq.mapR { tokenAdded =>
//                        val owner = tokenAdded.owner
//                        owner.ownerType match {
//                            case OwnerType.Signatures =>
//                                for {
//                                    signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
//                                    keys <- crypto.addressOperations.findKeysByPublic(signatures.keys)
//                                    result <- keys.headOption match {
//                                        case Some(_) =>
//                                            R(Option(tokenAdded.tokenId))
//                                        case None => R(None)
//                                    }
//                                } yield result
//
//                            case _ => R(None)
//                        }
//                    }
//                }.map(_.flatten).map(_.flatten)

            // implemented for case if change performed for same owner
            tokenChangeResponse <-
                block.tokensChanged.toSeq.mapR { tokenChangeEvent =>
                    val change = tokenChangeEvent.event
                    change.added.toSeq.mapR { tokenAdded =>
                        val owner = tokenAdded.owner
                        owner.ownerType match {
                            case OwnerType.Signatures =>
                                for {
                                    signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
                                    keys <- crypto.addressOperations.findKeysByPublic(signatures.keys)
                                    result <- keys.headOption match {
                                        case Some(_) =>
                                            R(Option(change))
                                        case None => R(None)
                                    }
                                } yield result
                            case _ => R(None)
                        }
                    }
                }.map(_.flatten).map(_.flatten).map { c =>
                    if (c.isEmpty) Collection.empty[TokenChangeResponse]
                    else collectionFromIterable(c.groupBy(_.deleted).map(_._2.head))
                }

            regulatoryTokensTransferred <-
                block.regulatorTransferred.toSeq.mapR { dealEvent =>
                    dealEvent.event.deal.legs.toSeq.mapR { leg =>
                        // The tokenId has changed it's owner, following is possible:
                        val tokenId = leg.tokenId
                        R(leg.previousOwner.ownerType).flatMap {
                            case OwnerType.Signatures =>
                                for {
                                    signatures <- R(Signatures.parseFrom(asByteArray(leg.previousOwner.address)))
                                    keys <- crypto.addressOperations.findKeysByPublic(signatures.keys)
                                } yield keys

                            case _ => R(Collection.empty[KeyIdentifier])
                        }.flatMap { keys =>
                            // 1. we owner:
                            if (keys.nonEmpty) {
                                val owner = leg.newOwner
                                owner.ownerType match {
                                    case OwnerType.Signatures =>
                                        for {
                                            signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
                                            newTokenOwnerKeys = signatures.keys
                                            result <-
                                                crypto.addressOperations.findKeysByPublic(newTokenOwnerKeys)
                                                    .flatMap { keys =>
                                                        keys.headOption match {
                                                            case Some(_) => // 1.1 we sent token to other address owned by us
                                                                R(Option(tokenId) -> None)
                                                            case None => // 1.2 we sent token to someone i.e. lost it:
                                                                R(None -> Option(tokenId))
                                                        }
                                                    }
                                        } yield result

                                    case OwnerType.SmartContractId =>
                                        R(None -> Option(tokenId))
                                    case _ => R(None -> None)
                                }
                            }
                            // 2. we not owner:
                            else {
                                val owner = leg.newOwner
                                owner.ownerType match {
                                    case OwnerType.Signatures =>
                                        for {
                                            signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
                                            newTokenOwnerKeys = signatures.keys
                                            result <- crypto.addressOperations.findKeysByPublic(newTokenOwnerKeys)
                                                .flatMap { keys =>
                                                    keys.headOption match {
                                                        case Some(_) => // 2.1 someone sent token to us
                                                            R(Option(tokenId) -> None)

                                                        case None => // 2.2 this token is not for us
                                                            R(None -> None)
                                                    }
                                                }
                                        } yield result

                                    case _ => R(None -> None)
                                }
                            }
                        }

                        //                                            }.toSeq
                        //                                        )
                    }
                }

                    .map(_.flatten)
                    .map(_.unzip)
                    .map { case (found, lost) =>
                        (found.flatten, lost.flatten)
                    }

//            tokenChangeBurnt <-
//                block.tokensChanged.toSeq.mapR { tokenChangeEvent =>
//                    val deleted = tokenChangeEvent.event.deleted
//                    deleted.toSeq.mapR { tokenBurnt =>
//                        val tokenId = tokenBurnt
//                        isMyToken(tokenId)
//                            .flatMap(res =>
//                                if (res)
//                                    R(Option(tokenId))
//                                else
//                                    R(None)
//                            )
//                    }
//                }.map(_.flatten).map(_.flatten)

            burntOwnedTokensWithRegulatorId <-
                block.regulatorBurned.toSeq.mapR { regulatoryBurnEvent =>
                    val regulatoryBurnRequest = regulatoryBurnEvent.event
                    val tokens = regulatoryBurnRequest.request.tokens
                    val regulatorId = regulatoryBurnRequest.signature.memberId
                    tokens.toSeq.mapR { tokenId =>
                        isMyBurntToken(tokenId)
                            .map(res =>
                                if (res)
                                    Option(tokenId -> regulatorId)
                                else
                                    None
                            )
                    }.map(_.flatten)
                }.map(_.flatten)

            burntByRegulator = burntOwnedTokensWithRegulatorId.map(_._1)
            (added, deleted) = tokensWithOperations.tokensExchangedWithOperations.unzip match {
                case (found, lost) =>
                    (
                        found.flatten.map(_._1) ++
                            tokensWithOperations.tokensIssuedWithOperations.flatMap(_._1) ++
                           // tokenChangeAdded ++
                            regulatoryTokensTransferred._1,

                        lost.flatten.map(_._1) ++
                            tokensWithOperations.burntOwnedTokensWithOperations.map(_._1) ++
                         //   tokenChangeBurnt ++
                            regulatoryTokensTransferred._2 ++
                            burntByRegulator
                    )
            }

            tokenChangedForRegulator <-
                block.tokensChanged.toSeq.mapR { tokenChanged =>
                    val tokenId = tokenChanged.event.deleted.head // TODO: Check all tokens
                    TokenId.from(tokenId).flatMap { identity =>
                        chain.getTokenType(identity.typeId).flatMap(tokenType =>
                            if (tokenType.regulation.exists(_.regulatorId == id.id)) {
                                R(Option(tokenChanged.event))
                            } else {
                                R(None)
                            }
                        )
                    }
                }.map(_.flatten)

            result = OwnerEvents(
                tokensIssued = tokensIssuedByMe,
                transfersProposed = messages.transfersProposed,
                tokensRequested = messages.tokensRequested,
                tokensPending = collectionFromIterable(incomingTokens),
                tokensReceived = added,
                tokensBurn = deleted, // ++ collectionFromIterable(tokenChangeBurnt),
                tokenChanged = collectionFromIterable(tokenChangedForRegulator) ++ tokenChangeResponse
            )
        } yield result
}