package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{Collection, ROps, asByteArray, collectionFromIterable}
import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.model.{OwnerType, Signatures}
import ru.sberbank.blockchain.cnft.wallet.WalletCommonOps
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity

import scala.language.higherKinds

class TokensWithOperationsExtractor[R[+_]](
    val id: WalletIdentity,
    val crypto: WalletCrypto[R],
    val chain: ChainServiceSpec[R],
)(implicit
    val R: ROps[R]
) extends WalletCommonOps[R] {

    def extract(block: BlockEvents): R[TokensWithOperations] = {
        for {
            tokensExchangedWithOperations <-
                block.tokensExchanged.toSeq.mapR { dealEvent =>
                            dealEvent.event.deal.legs.toSeq.mapR { leg =>
                                    // The tokenId has changed it's owner, following is possible:
                                    val tokenId = leg.tokenId
                                    amIAnOwner(leg.previousOwner)
                                        .flatMap { isMy =>
                                        // 1. we owner:
                                        if (isMy) {
                                            val owner = leg.newOwner
                                            owner.ownerType match {
                                                case OwnerType.Signatures =>
                                                    for {
                                                        signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
                                                        newTokenOwnerKeys = signatures.keys
                                                        result <- crypto.addressOperations.findKeysByPublic(newTokenOwnerKeys).flatMap { keys =>
                                                            keys.headOption match {
                                                                case Some(newKey) => // 1.1 we sent token to other address owned by us
                                                                    R(Option((tokenId, dealEvent.event.deal.operationId)) -> None)
                                                                case None => // 1.2 we sent token to someone i.e. lost it:
                                                                    R(None -> Option((tokenId, dealEvent.event.deal.operationId)))
                                                            }
                                                        }
                                                    } yield result

                                                case OwnerType.SmartContractId =>
                                                    R(None -> Option((tokenId, dealEvent.event.deal.operationId)))
                                                case _ =>
                                                    R(None -> None)
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
                                                        result <- crypto.addressOperations.findKeysByPublic(newTokenOwnerKeys).flatMap { keys =>
                                                            keys.headOption match {
                                                                case Some(key) => // 2.1 someone sent token to us
                                                                        R(Option((tokenId, dealEvent.event.deal.operationId)) -> None)
                                                                case None => // 2.2 this token is not for us
                                                                    R(None -> None)
                                                            }
                                                        }
                                                    } yield result

                                                case _ => R(None -> None)
                                            }
                                        }
                                    }
                            }
                }.map(_.flatten)

            tokensIssuedForUsWithOperations <- {
                block.tokensIssued.toSeq.mapR { tokenIssueEvent =>
                    for {
                        tokens <-
                            tokenIssueEvent.event.issue.tokens.toSeq.mapR { issueToken =>
                                val owner = issueToken.owner
                                owner.ownerType match {
                                    case OwnerType.Signatures =>
                                        for {
                                            signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
                                            keys <- crypto.addressOperations.findKeysByPublic(signatures.keys)
                                            result <- keys.headOption match {
                                                case Some(keyIdentifier) =>
                                                    val tokenId = issueToken.tokenId
                                                    R(Option(tokenId))
                                                case None => R(None)
                                            }
                                        } yield result
                                    case _ => R(None)
                                }
                            }.map(_.flatten)
                    } yield (collectionFromIterable(tokens), Option(tokenIssueEvent.event.issue.operationId))
                }
            }

            burntOwnedTokensWithOperations <-
                block.tokensBurned.toSeq.mapR { burnEvent =>
                    val burnResponse = burnEvent.event
                    val burnRequest = burnResponse.request.request
                    burnResponse.request.request.tokens.toSeq.mapR { tokenId =>
                        isMyBurntToken(tokenId)
                            .flatMap { isMy =>
                                if (isMy)
                                    R(Option((tokenId, burnRequest.operationId)))
                                else
                                    R(None)
                            }
                    }.map(_.flatten)
                }.map(_.flatten)

            result = TokensWithOperations(
                collectionFromIterable(tokensExchangedWithOperations),
                collectionFromIterable(tokensIssuedForUsWithOperations),
                collectionFromIterable(burntOwnedTokensWithOperations)
            )
        } yield result
    }
}

case class TokensWithOperations(
    tokensExchangedWithOperations: Collection[(Option[(String, String)], Option[(String, String)])],
    tokensIssuedWithOperations: Collection[(Collection[String], Option[String])],
    burntOwnedTokensWithOperations: Collection[(String, String)],
)