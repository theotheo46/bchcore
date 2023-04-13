package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.common.types.collectionFromIterable
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{LoggingSupport, ROps, asByteArray, collectionFromArray}
import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.model.{MemberSignature, RequestActorType, TokenId}
import ru.sberbank.blockchain.cnft.wallet.WalletCommonOps
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec.{RegulatorEvents, ValidatedEvent}
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity

import scala.language.higherKinds

class RegulatorEventsExtractor[R[+_]](
    val id: WalletIdentity,
    val crypto: WalletCrypto[R],
    val chain: ChainServiceSpec[R],
)(
    implicit val R: ROps[R]
) extends WalletCommonOps[R] with LoggingSupport {

    def extract(block: BlockEvents, skipSignaturesCheck: Boolean): R[RegulatorEvents] =
        for {
            issueRegulationNeeded <-
                block.pendingIssues.toSeq
                    .filter(_.event.approvals.forall(approve => !approve.approve && approve.reason.isEmpty))
                    .filterR { pendingEvent =>
                        val request = pendingEvent.event.request

                        request.issue.tokens.toSeq.findR { issueToken =>
                            TokenId.from(issueToken.tokenId).map(_.typeId).flatMap { tokenTypeId =>
                                chain.getTokenType(tokenTypeId).map { theType =>
                                    theType.regulation.exists(_.regulatorId == id.id)
                                }
                            }
                        }.map(_.nonEmpty)
                    }.flatMap {
                    _.mapR { pendingEvent =>
                        val pending = pendingEvent.event
                        val valid =
                            if (skipSignaturesCheck) {
                                R(())
                            } else {
                                pending.request.actors.toSeq.mapR { actor =>
                                    actor.theType match {
                                        case RequestActorType.Member =>
                                            for {
                                                memberSignature <- R(MemberSignature.parseFrom(asByteArray(actor.value)))
                                                memberInfo <- chain.getMember(memberSignature.memberId)
                                                signatureOK <- crypto.identityOperations.verifySignature(memberInfo.signingPublic, pending.request.issue.toBytes, memberSignature.value)
                                                _ <- R.expect(signatureOK, s"wrong member signature")
                                            } yield ()

                                        case RequestActorType.SmartContract =>
                                            R(())
                                    }
                                }
                            }

                        valid match {
                            case Right(_) =>
                                R(ValidatedEvent(pending, ValidatedEvent.Valid))
                            case Left(msg: String) =>
                                R(ValidatedEvent(pending, msg))
                        }
                    }
                }


            dealRegulationNeeded <-
                block.pendingDeals.toSeq
                    .map(_.event)
                    .filter { e =>
                        e.approvals.forall(approve => !approve.approve && approve.reason.isEmpty) &&
                            e.approvals.exists(_.regulatorId == id.id)
                    }
                    .mapR { pendingDeal =>

                        val valid =
                            if (skipSignaturesCheck) {
                                R(())
                            } else pendingDeal.deal.actors.toSeq.mapR { actor =>
                                actor.theType match {
                                    case RequestActorType.Member =>
                                        for {
                                            decryptedMemberSignature <- crypto.encryptionOperations.decrypt(actor.value, id.encryptionKey)
                                            memberSignature <- R(MemberSignature.parseFrom(asByteArray(decryptedMemberSignature)))
                                            memberInfo <- chain.getMember(memberSignature.memberId)
                                            signatureOK <- crypto.identityOperations.verifySignature(memberInfo.signingPublic, pendingDeal.deal.deal.toBytes, memberSignature.value)
                                            _ <- R.expect(signatureOK, s"wrong member signature")
                                        } yield ()

                                    case RequestActorType.SmartContract =>
                                        R(())
                                }
                            }
                        //FIXME: this one will not work for JS !!!
                        valid match {
                            case Right(_) =>
                                R(ValidatedEvent(pendingDeal, ValidatedEvent.Valid))
                            case Left(msg: String) =>
                                R(ValidatedEvent(pendingDeal, msg))
                        }

                    }


            burnRegulationNeeded <-
                block.pendingBurns.toSeq
                    .filter(_.event.approvals.forall(approve => !approve.approve && approve.reason.isEmpty)) // take only those that not yet approved (and not rejected)
                    .mapR { pendingTransactionEvent =>
                        val pendingBurn = pendingTransactionEvent.event
                        val signedRequest = pendingBurn.burnRequest
                        val burnRequest = signedRequest.request
                        burnRequest.tokens.toSeq
                            .findR { tokenId =>
                                TokenId.from(tokenId).flatMap { identity =>
                                    chain.getTokenType(identity.typeId).map { theType =>
                                        theType.regulation.exists(_.regulatorId == id.id)
                                    }
                                }
                            }.flatMap { // try find token where we regulator
                            case Some(_) =>
                                val valid =
                                    if (skipSignaturesCheck) {
                                        R(())
                                    } else {
                                        for {
                                            decryptedMemberSignature <- crypto.encryptionOperations.decrypt(signedRequest.memberSignature, id.encryptionKey)
                                            memberSignature <- R(MemberSignature.parseFrom(asByteArray(decryptedMemberSignature)))
                                            memberInfo <- chain.getMember(memberSignature.memberId)
                                            signatureOK <- crypto.identityOperations.verifySignature(memberInfo.signingPublic, burnRequest.toBytes, memberSignature.value)
                                            _ <- R.expect(signatureOK, s"wrong member signature")
                                        } yield ()
                                    }

                                {
                                    valid match {
                                        case Right(_) =>
                                            R(ValidatedEvent(pendingBurn, ValidatedEvent.Valid))
                                        case Left(msg: String) =>
                                            R(ValidatedEvent(pendingBurn, msg))
                                    }
                                }.map(a => Option(a))

                            case None => R(None)
                        }
                    }.map(_.flatten)

            scRegulationNeeded = block.smartContractsAdded.flatMap { smartContract =>
                val sc = smartContract.event
                sc.regulators.flatMap { regulator =>
                    if (id.id == regulator.regulatorId) {
                        Some(sc.id)
                    } else None

                }
            }

            tokenChangedForRegulator <-
                block.tokensChanged.map(_.event).toSeq.filterR { tokenChanged =>
                    val tokenId = tokenChanged.deleted.head // TODO: Check all tokens
                    TokenId.from(tokenId).flatMap { t =>
                        chain.getTokenType(t.typeId).map(tokenType =>
                            tokenType.regulation.exists(_.regulatorId == id.id)
                        )
                    }
                }

            tokenMergedForRegulator <-
                block.tokensMerged.map(_.event).toSeq.filterR { tokenMerged =>
                    tokenMerged.toDelete.toSeq.findR { tokenId =>
                        TokenId.from(tokenId).flatMap { t =>
                            chain.getTokenType(t.typeId).map(tokenType =>
                                tokenType.regulation.exists(_.regulatorId == id.id)
                            )
                        }
                    }.map(_.isDefined)
                }

        } yield
            RegulatorEvents(
                pendingIssues = collectionFromIterable(issueRegulationNeeded),
                pendingDeals = collectionFromIterable(dealRegulationNeeded),
                pendingBurn = collectionFromIterable(burnRegulationNeeded),
                pendingSmartContracts = collectionFromArray(scRegulationNeeded),
                tokenChanged = collectionFromIterable(tokenChangedForRegulator),
                tokenMerged = collectionFromIterable(tokenMergedForRegulator)
            )

}