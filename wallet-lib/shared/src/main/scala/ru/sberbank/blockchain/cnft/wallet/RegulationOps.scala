package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.RelatedDealReferenceEmpty
import ru.sberbank.blockchain.cnft.common.types.{Bytes, Collection}
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{asByteArray, asBytes, collectionFromIterable}
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.model.{BurnRequest, Deal, DealLeg, MemberSignature, RegulatorBurnRequest, RegulatorSignedTokenChangeRequest, RegulatorTransferRequest, TokenChangeRequest, TokenChangeResponse, TokenFreezeRequest, TokenOwner}
import ru.sberbank.blockchain.cnft.spec.CNFTChallenge
import ru.sberbank.blockchain.cnft.wallet.walletmodel.{FreezeInfo, RegulatoryBurnTokenRequest}

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
class RegulationOps[R[+_]](wallet: CNFTWalletInternal[R]) {

    import wallet._

    def freezeToken(requests: Collection[FreezeInfo]): R[TxResult[Unit]] =
        _freezeToken(requests, freeze = true)

    def unfreezeToken(requests: Collection[FreezeInfo]): R[TxResult[Unit]] =
        _freezeToken(requests, freeze = false)

    private def _freezeToken(requests: Collection[FreezeInfo], freeze: Boolean): R[TxResult[Unit]] =
        for {
            res <- requests.toSeq.mapR { request =>
                val challenge = asBytes(
                    CNFTChallenge.freezeTokenByRegulator(freeze, request.tokenIds)
                )
                crypto.identityOperations.createSignature(wallet.id.signingKey, challenge)
                    .map { regulatorySignature =>
                        TokenFreezeRequest(
                            regulatorId = wallet.id.id,
                            restrictionId = request.restrictionId,
                            freeze = freeze,
                            signature = regulatorySignature,
                            tokenIds = request.tokenIds
                        )
                    }
            }
                .map(collectionFromIterable)
            result <- chainTx.freezeToken(res)
        } yield result

    def regulatoryBurnToken(request: RegulatoryBurnTokenRequest): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity

            //            requests <-
            //                request.tokenId.mapR { tokenId =>
            //                    for {
            //                        //TODO: add regulators
            //                        issuerId <- chain.getTokenType(tokenId.typeId).map(_.issuerId)
            //                        issuer <- chain.getMember(issuerId)
            //                        // TODO: encrypt extra data on keys:
            //                        // 0) issuers
            //                        // 1) current owner(s)
            //                        // 2) regulators
            //                    } yield ()
            //                }

            operationId = generateId
            timestamp = generateTimestamp

            burnRequest =
                BurnRequest(
                    operationId = operationId,
                    timestamp = timestamp,
                    tokens = request.tokens,
                    extra = Bytes.empty
                )

            signature <-
                crypto.identityOperations
                    .createSignature(
                        myId.signingKey, burnRequest.toBytes
                    )

            regulatorBurnRequest = RegulatorBurnRequest(
                burnRequest,
                MemberSignature(myId.id, signature)
            )

            response <- chainTx.regulatoryBurnToken(regulatorBurnRequest)

        } yield response

    def regulatoryTransfer(memberId: String, dealId: String, tokenIds: Collection[String], to: Bytes): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            legs <-
                tokenIds.toSeq.mapR { tokenId =>
                    for {
                        currentOwner <- chain.getTokenOwner(tokenId)
                        newOwner <- R(TokenOwner.parseFrom(asByteArray(to)))
                    } yield DealLeg(tokenId, newOwner, currentOwner, RelatedDealReferenceEmpty, Collection.empty)
                }
            operationId = generateId
            timestamp = generateTimestamp
            deal = Deal(
                operationId = operationId,
                timestamp = timestamp,
                dealId = dealId,
                legs = collectionFromIterable(legs),
                extra = Bytes.empty
            )
            dealSign <- crypto.identityOperations.createSignature(myId.signingKey, deal.toBytes)
            dealRequest =
                RegulatorTransferRequest(
                    deal = deal,
                    signature = MemberSignature(myId.id, dealSign)
                )
            result <- chainTx.regulatoryTransfer(dealRequest)
        } yield result

    def regulatoryChangeToken(tokenId: String, amounts: Collection[String]): R[TxResult[TokenChangeResponse]] = {
        val request =
            TokenChangeRequest(
                operationId = generateId,
                timestamp = generateTimestamp,
                tokenId = tokenId,
                amounts = amounts
            )
        for {
            signature <- crypto.identityOperations.createSignature(wallet.id.signingKey, request.toBytes)
            memberSignature = MemberSignature(wallet.id.id, signature)
            result <- chainTx.regulatoryChangeToken(
                RegulatorSignedTokenChangeRequest(
                    tokenChangeRequest = request,
                    memberSignature = memberSignature
                ))
        } yield result
    }
}
