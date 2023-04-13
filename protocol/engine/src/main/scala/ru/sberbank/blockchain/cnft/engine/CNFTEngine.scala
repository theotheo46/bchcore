package ru.sberbank.blockchain.cnft.engine

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECPoint
import org.enterprisedlt.general.codecs.proto._
import ru.sberbank.blockchain.cnft.CurrentPlatformVersion
import ru.sberbank.blockchain.cnft.common.types.{collectionFromSequence, _}
import ru.sberbank.blockchain.cnft.commons.ROps.IterableR_Ops
import ru.sberbank.blockchain.cnft.commons.{LoggingSupport, Result, ResultOps, collectionFromArray, collectionToArray, isEqualBytes}
import ru.sberbank.blockchain.cnft.engine.contract._
import ru.sberbank.blockchain.cnft.engine.dna._
import ru.sberbank.blockchain.cnft.model._
import ru.sberbank.blockchain.cnft.spec.{CNFTChallenge, CNFTSpec}
import ru.sberbank.blockchain.common.cryptography.SignatureOperations
import ru.sberbank.blockchain.common.cryptography.bouncycastle.{BouncyCastleHasher, EllipticOps}
import ru.sberbank.blockchain.common.cryptography.sag.RingSigner

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import scala.util.Try

/**
 * @author Alexey Polubelov
 * @author Maxim Fedin
 * @author Andrew Pudovikov
 * @author Vladimir Sidorov
 */
trait CNFTEngine extends CNFTSpec[Result] with SignatureVerify
    with LoggingSupport {

    def store: CNFTStore

    def txContext: TransactionContext

    def cryptography: SignatureOperations[Result]

    // =====================================================================================================================
    // Token types
    // =====================================================================================================================

    override def registerTokenType(requests: SignedTokenTypeRegistration): Result[Unit] = {
        for {
            issuers <- Result(requests.request.tokenType.map(_.issuerId))
            _ <- Result.expect(issuers.toSet.size == 1, s"Request shall be submitted by the same issuer for all types")
            _ <- logger.debugR(s"test message")
            issuer <- getMember(issuers.head)
            signatureOk <-
                cryptography.verifySignature(
                    issuer.signingPublic,
                    requests.request.toByteArray,
                    requests.signature
                )
            _ <- Result.expect(signatureOk, s"Wrong token type registration request signature")

            _ <- requests.request.tokenType.mapR { tokenType =>
                val description =
                    tokenType.meta.description
                        .map(f => s"${f.name} = ${f.value}")
                        .mkString("\n\t", "\n\t", "\n")

                logger.debug(s"Registering token type with description:$description")
                for {
                    _ <- tokenType.dna.change.mapR { gene =>
                        ResultOps.fromOption(GenesRegistry.ChangeGenes.get(gene.id), s"No such Change Genes: ${gene.id}")
                    }
                    _ <- tokenType.dna.emission.mapR { gene =>
                        ResultOps.fromOption(GenesRegistry.EmissionGenes.get(gene.id), s"No such Emission Genes: ${gene.id}")
                    }
                    _ <- tokenType.dna.transfer.mapR { gene =>
                        ResultOps.fromOption(GenesRegistry.TransferGenes.get(gene.id), s"No such Transfer Genes: ${gene.id}")
                    }
                    _ <- tokenType.dna.burn.mapR { gene =>
                        ResultOps.fromOption(GenesRegistry.BurnGenes.get(gene.id), s"No such Burn Genes: ${gene.id}")
                    }
                    _ <- tokenType.regulation.mapR { regCapabilities =>
                        getMember(regCapabilities.regulatorId)
                    }
                    _ <- Util.expect(
                        store.getTokenType(tokenType.typeId).isEmpty,
                        s"Type with id : ${tokenType.typeId} already exist"
                    )
                    _ <- Util.expect(
                        tokenType.issuerId.nonEmpty,
                        "issuerId is missing"
                    )
                    _ <- getMember(tokenType.issuerId)
                    _ <- Result(store.saveTokenType(tokenType))
                } yield ()
            }
        } yield ()
    }


    override def getTokenType(typeId: String): Result[TokenType] = {
        logger.debug(s"Querying for $typeId token type")
        store.getTokenType(typeId).toRight(s"No token type with id: $typeId")
    }

    override def listTokenTypes: Result[Array[TokenType]] = Try {
        logger.debug(s"Querying for token types list")
        store.listTokenTypes
    }.toEither.left.map(_.getMessage)

    override def listTokenTypesFiltered(filter: TokenTypeFilter): Result[Collection[TokenType]] = {
        Try {
            val TokenTypeFilter(changeGeneId, negation) = filter
            logger.debug(s"Querying for token types filtered by changeGeneId=$changeGeneId, with negation: $negation")
            store.listTokenTypesWithChangeGene(changeGeneId, negation)
        }.toEither.left.map(_.getMessage)
    }


    // =====================================================================================================================
    // Tokens
    // =====================================================================================================================

    override def issueToken(issueTokenRequest: IssueTokenRequest): Result[OperationEffect] = {

        for {
            _ <- verifyIssueTokens(issueTokenRequest, OperationInitiator.Client)

            approvalsRequired <-
                getRequiredRegulation(
                    issueTokenRequest.issue.tokens.map(_.tokenId),
                    Collection(RegulatorOperation.IssueControl)
                )

            issuers <-
                issueTokenRequest.issue.tokens
                    .mapR(token => TokenId.from(token.tokenId))
                    .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                    .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))

            result <-
                if (approvalsRequired.nonEmpty) {
                    val pendingIssue =
                        PendingIssue(
                            timestamp = txContext.timestamp,
                            request = issueTokenRequest,
                            approvals = collectionFromSequence(
                                approvalsRequired.map { regulatorId =>
                                    RegulatorApproval(regulatorId, approve = false, reason = "", Bytes.empty, notice = "")
                                })
                        )
                    for {
                        _ <-
                            saveOperation(
                                issueTokenRequest.issue.operationId,
                                OperationStatus.IssuePendingRegulation,
                                pendingIssue,
                                issueTokenRequest.issue.tokens.map(_.owner).distinct,
                                collectionFromSequence(approvalsRequired),
                                issuers
                            )
                    } yield {
                        issueTokenRequest.issue.tokens.foreach { issueToken =>
                            store.saveTokenLinkedOperation(issueToken.tokenId, issueTokenRequest.issue.operationId)
                        }
                        OperationEffect
                            .defaultInstance
                            .withPendingIssues(Collection(pendingIssue))
                    }
                } else
                    _issueToken(issueTokenRequest)
        } yield
            OperationEffect
                .defaultInstance
                .withIssued(result.issued)
                .withPendingIssues(result.pendingIssues)
    }

    private def saveOperation(
        operationId: String,
        status: String,
        data: OperationData,
        addresses: Collection[TokenOwner],
        regulators: Collection[String],
        issuers: Collection[String]
    ): Result[Operation] =
        for {
            maybeOperation <- Result(store.getOperation(operationId))
            operation <- Result {
                maybeOperation match {
                    // if operation already exist:
                    case Some(previous) =>
                        Operation(
                            operationId = operationId,
                            history = collectionFromArray(
                                collectionToArray(previous.data.history) :+
                                    OperationState(
                                        timestamp = txContext.timestamp,
                                        state = status,
                                        data = data.toBytes,
                                        block = "", //todo
                                        txId = txContext.txId
                                    )
                            )
                        )

                    // if no operation exist yet, create new:
                    case None =>
                        Operation(
                            operationId = operationId,
                            history = Collection(
                                OperationState(
                                    timestamp = txContext.timestamp,
                                    state = status,
                                    data = data.toBytes,
                                    block = "", //todo
                                    txId = txContext.txId
                                )
                            )
                        )
                }
            }
            _ <- Result(store.saveOperation(operationId, OperationHistory(operation, addresses, regulators, issuers)))
        } yield operation


    private def verifyIssueTokens(issueTokenRequest: IssueTokenRequest, operationInitiator: OperationInitiator): Result[Collection[Unit]] = {
        issueTokenRequest.issue.tokens.mapR { issueToken =>
            for {
                exist <- store.tokenExist(issueToken.tokenId)
                _ <- Result.expect(!exist, s"token ${issueToken.tokenId} already exist")

                _ <- issueToken.owner.ownerType match {
                    case OwnerType.Signatures =>
                        Result(Signatures.parseFrom(issueToken.owner.address))
                            .left.map(_ => "Invalid TokenOwner address")
                    case OwnerType.SmartContractId =>
                        getSmartContract(issueToken.owner.address.toUTF8)
                    case other =>
                        Result.Fail(s"Invalid Token owner type [$other]")
                }

                tokenTypeId <- TokenId.from(issueToken.tokenId).map(_.typeId)
                tokenType <- store.getTokenType(tokenTypeId).toRight(s"Unknown typeId: $tokenTypeId")

                _ <- Result.expect(tokenType.dna.emission.nonEmpty, "Emission is not allowed by Genes")

                _ <- tokenType.dna.emission.mapR { g =>
                    for {
                        gene <- GenesRegistry.EmissionGenes.get(g.id).toRight(s"Invalid Gene ID: ${g.id}")
                        canIssue <- gene.canIssue(
                            GeneExecutionContextImpl(store, cryptography, tokenType, operationInitiator, g.parameters, Map.empty),
                            issueTokenRequest
                        )
                        _ <- Result.expect(canIssue, s"Issue rejected by Gene ${g.id}")
                    } yield ()
                }
                _ <- Result.expect(tokenType.meta.fields.length == issueToken.body.fields.length,
                    s"Invalid count of fields supplied in body for token type")
            } yield ()
        }
    }

    private def _issueToken(issueTokenRequest: IssueTokenRequest): Result[OperationEffect] = {
        issueTokenRequest.issue.tokens.mapR { issueToken =>
            for {
                tokenTypes <-
                    issueTokenRequest.issue.tokens
                        .mapR(token => TokenId.from(token.tokenId))
                        .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                regulators <- Result(tokenTypes.flatMap(typ => typ.regulation.map(_.regulatorId)))
                issuers <- Result(tokenTypes.map(typ => typ.issuerId))
                _ <- store.createToken(issueToken.tokenId, issueToken.owner, issueToken.body)
                _ <-
                    saveOperation(
                        issueTokenRequest.issue.operationId,
                        OperationStatus.IssueDone,
                        issueTokenRequest,
                        issueTokenRequest.issue.tokens.map(_.owner).distinct,
                        regulators,
                        issuers
                    )
            } yield ()
        }.map { _ =>
            OperationEffect
                .defaultInstance
                .withIssued(
                    Collection(
                        issueTokenRequest
                    )
                )
        }
    }


    override def changeToken(request: SignedTokenChangeRequest): Result[TokenChangeResponse] = {
        val Amount = "amount"
        for {
            tokenTypeId <- TokenId.from(request.tokenChangeRequest.tokenId).map(_.typeId)
            tokenType <- store.getTokenType(tokenTypeId)
                .toRight(s"No such typeId: $tokenTypeId")
            tokenRestrictions <- getTokenRestrictions(request.tokenChangeRequest.tokenId)
            _ <- Util.expect(tokenRestrictions.restrictions.isEmpty, s"Operations with this token have been restricted by ${tokenRestrictions.restrictions.map(_.toProtoString).mkString(";\n\t")}")
            tokenOwner <- getTokenOwner(request.tokenChangeRequest.tokenId)
            signatureOk <- tokenOwner.ownerType match {
                case OwnerType.Signatures =>
                    for {
                        address <- Result(Signatures.parseFrom(tokenOwner.address))
                        tokenId = request.tokenChangeRequest.tokenId
                        isValid =
                            verifySignatures(
                                request.tokenChangeRequest.toBytes,
                                address,
                                Collection(request.signature)
                            )
                        _ <- if (isValid) Result(()) else Result.Fail(s"No signatures for $tokenId")
                    } yield true

                case OwnerType.SmartContractId =>
                    Result.Fail("Only SmartContract allowed to move tokens from SmartContract address")
            }

            _ <- Util.expect(
                signatureOk, "Invalid signature"
            )
            _ <- Either.cond(tokenType.dna.change.nonEmpty, (), "Change is not allowed by Genes")
            tokenId = request.tokenChangeRequest.tokenId
            tokenContent <- store.getTokenBody(tokenId).toRight(s"Token $tokenId does not exist")
            amountIndex <- findTokenFieldIndex(tokenType, Amount)
            _ <- tokenType.dna.change.mapR { g =>
                for {
                    gene <- GenesRegistry.ChangeGenes.get(g.id).toRight(s"No such gene: ${g.id}")
                    context =
                        GeneExecutionContextImpl(
                            store, cryptography, tokenType, OperationInitiator.Client, g.parameters, Map.empty
                        )
                    canChange <- gene.canChange(context, request.tokenChangeRequest)
                    _ <- Either.cond(canChange, (), s"Change is not allowed by gen ${g.id}")
                } yield ()
            }

            tokenTypeId <- TokenId.from(request.tokenChangeRequest.tokenId).map(_.typeId)
            added <- request.tokenChangeRequest.amounts.mapR { amount =>
                val newTokenContent =
                    TokenContent(
                        collectionFromArray(
                            tokenContent.fields.updated(amountIndex, amount)
                        )
                    )

                TokenId.encode(tokenTypeId, txContext.nextUniqueId).flatMap { tokenId =>
                    store.createToken(
                        tokenId = tokenId,
                        owner = tokenOwner,
                        body = newTokenContent
                    ).map(_ =>
                        TokenAdded(
                            tokenId = tokenId,
                            owner = tokenOwner,
                            tokenContent = newTokenContent,
                            restrictions = tokenRestrictions
                        )
                    )
                }
            }

            _ <- deleteToken(request.tokenChangeRequest.tokenId, Collection.empty, WalletTokenStatus.Changed)
        } yield
            TokenChangeResponse(
                added = added,
                deleted = Collection(request.tokenChangeRequest.tokenId)
            )
    }

    override def getToken(tokenId: String): Result[WalletToken] =
        for {
            content <- getTokenContent(tokenId).map(_.fields)
            restrictions <- getTokenRestrictions(tokenId).map(_.restrictions)
            operations <- getTokenLinkedOperations(tokenId)
            tokenOwner <- getTokenOwner(tokenId)
        } yield
            WalletToken(
                tokenId,
                content,
                restrictions,
                operations,
                tokenOwner,
                WalletTokenStatus.Issued
            )

    override def mergeTokens(request: SignedTokenMergeRequest): Result[TokenMergeResponse] = {
        val Amount = "amount"
        for {
            _ <- Util.expect(request.tokenMergeRequest.tokens.length > 1, s"can not merge less than 2 tokens")

            firstTokenId <- Result(request.tokenMergeRequest.tokens.head)
            firstTokenTypeId <- TokenId.from(firstTokenId).map(_.typeId)
            firstTokenType <- store.getTokenType(firstTokenTypeId)
                .toRight(s"No such typeId: $firstTokenId")
            firstTokenOwner <- getTokenOwner(request.tokenMergeRequest.tokens.head)
            firstTokenRestrictions <- getTokenRestrictions(request.tokenMergeRequest.tokens.head)
            firstTokenAmountIndex <- findTokenFieldIndex(firstTokenType, Amount)
            firstTokenContent <- store.getTokenBody(firstTokenId).toRight(s"Token $firstTokenId does not exist")

            merging <- request.tokenMergeRequest.tokens.zipWithIndex.mapR { case (tokenId, index) =>
                for {
                    tokenTypeId <- TokenId.from(tokenId).map(_.typeId)
                    tokenType <- store.getTokenType(tokenTypeId)
                        .toRight(s"No such typeId: $tokenTypeId")
                    _ <- Util.expect(tokenTypeId == firstTokenTypeId, s"can not merge tokens of different types")
                    // dna check
                    _ <- Util.expect(tokenType.dna.change.nonEmpty, "Change is not allowed by Genes")

                    tokenRestrictions <- getTokenRestrictions(tokenId)
                    _ <-
                        Util.expect(
                            tokenRestrictions.restrictions.isEmpty,
                            s"Operations with this token have been restricted by ${tokenRestrictions.restrictions.map(_.toProtoString).mkString(";\n\t")}"
                        )

                    tokenOwner <- getTokenOwner(tokenId)
                    _ <- tokenOwner.ownerType match {
                        case OwnerType.Signatures =>
                            for {
                                address <- Result(Signatures.parseFrom(tokenOwner.address))
                                isValid =
                                    verifySignatures(
                                        request.tokenMergeRequest.toByteArray,
                                        address,
                                        request.signatures
                                    )
                                _ <- if (isValid) Result(()) else Result.Fail(s"No signatures for $tokenId")
                            } yield true
                        case OwnerType.SmartContractId =>
                            Result.Fail("Only SmartContract allowed to move tokens from SmartContract address")
                    }
                    tokenContent <- store.getTokenBody(tokenId).toRight(s"Token $tokenId does not exist")
                    value <- Try(tokenContent.fields(firstTokenAmountIndex).toLong).toOption.toRight(s"Invalid $Amount value")
                } yield value
            }.map(_.sum)

            newTokenContent =
                TokenContent(
                    collectionFromArray(
                        firstTokenContent.fields.updated(firstTokenAmountIndex, merging.toString)
                    )
                )

            tokenId <- TokenId.encode(firstTokenTypeId, txContext.nextUniqueId)
            _ <- store.createToken(tokenId, firstTokenOwner, newTokenContent)
            _ <- request.tokenMergeRequest.tokens.mapR { toDel =>
                deleteToken(toDel, Collection.empty, WalletTokenStatus.Merged)
            }
        } yield
            TokenMergeResponse(
                added = TokenAdded(tokenId, firstTokenOwner, newTokenContent, firstTokenRestrictions),
                toDelete = request.tokenMergeRequest.tokens
            )
    }

    override def getTokenOwner(tokenId: String): Result[TokenOwner] = store.getTokenOwner(tokenId)

    override def getTokenContent(tokenId: String): Result[TokenContent] =
        store.getTokenBody(tokenId).toRight(s"Token Body does not exist $tokenId")

    override def getTokenRestrictions(tokenId: String): Result[TokenRestrictions] = store.getTokenRestrictions(tokenId)

    override def getTokenLinkedOperations(tokenId: String): Result[Array[String]] = Result {
        store.getTokenLinkedOperation(tokenId).toArray
    }

    override def acceptToken(acceptTokenRequest: AcceptTokenRequest): Result[OperationEffect] =
        for {
            operation <- store.getOperation(acceptTokenRequest.transactionId).toRight(s"No operation with id ${acceptTokenRequest.transactionId}")
            _ <- Util.expect(
                operation.data.history.lastOption.exists(_.state == OperationStatus.AcceptPending),
                s"Operation with id ${acceptTokenRequest.transactionId} is not in pending accept state"
            )
            dealRequest <- Result(DealRequest.parseFrom(operation.data.history.last.data))
            // status will be updated by "make deal" below:
            result <- _makeDeal(
                dealRequest.copy(
                    recipientSignatures = dealRequest.recipientSignatures ++ acceptTokenRequest.dealSignatures,
                    actors =
                        dealRequest.actors :+
                            RequestActor(
                                theType = RequestActorType.Member,
                                value = acceptTokenRequest.memberSignature
                            )
                )
            )
        } yield result

    override def listTokens: Result[Array[WalletToken]] = store.listTokens.mapR(tokenId => getToken(tokenId))

    //check if deal correct:
    //    private def verifyDeal(dealRequest: DealRequest): Result[Unit] = {
    //        for {
    //            // 0. Check that there are some legs in deal:
    //            _ <- Result.expect(dealRequest.deal.legs.nonEmpty, "Empty deals are not allowed")
    //
    //            // 1. check that all specified destination
    //            // addresses are correct (i.e. valid signatures or valid smart contracts):
    //            _ <- Util.foldEither {
    //                dealRequest.deal.legs.map { leg =>
    //                    leg.newOwner.ownerType match {
    //                        case OwnerType.Signatures =>
    //                            for {
    //                                signatures <- Result(Signatures.parseFrom(leg.newOwner.address))
    //                                    .left.map(_ => "Invalid TokenOwner address")
    //
    //                                _ <- Result.expect(
    //                                    signatures.require > 0,
    //                                    "Invalid address: required keys count must be greater than 0"
    //                                )
    //
    //                            } yield ()
    //
    //                        case OwnerType.SmartContractId =>
    //                            for {
    //                                contract <- getSmartContract(leg.newOwner.toBytes)
    //                                regulation <- getSmartContractRegulation(contract.address)
    //                                _ <- Result.expect(
    //                                    regulation.approves.forall(_.approved == true),
    //                                    s"Smart contract [${contract.address.toB64}] is not yet approved by regulators"
    //                                )
    //                            } yield ()
    //
    //                        case other =>
    //                            Result.Fail(s"Unsupported owner type: $other")
    //                    }
    //                }
    //            }
    //            // 2. check that none of the tokens are restricted
    //            _ <- ResultOps.foldFailFast {
    //                dealRequest.deal.legs.map { leg =>
    //                    Lazy[Result, Unit] {
    //                        for {
    //                            trs <- store.getTokenRestrictions(leg.tokenId)
    //                            _ <-
    //                                Util.expect(
    //                                    trs.restrictions.isEmpty,
    //                                    s"Token ${leg.tokenId} is frozen"
    //                                )
    //                        } yield ()
    //                    }
    //                }
    //            }
    //            // 3. check if any of the tokens is already under operation
    //            _ <- Result.expect(
    //                dealRequest.deal.legs.forall { leg =>
    //                    store.getTokenLinkedOperation(leg.tokenId).isEmpty
    //                },
    //                "There are operations with some of the tokens already in progress"
    //            )
    //        } yield ()
    //    }

    //    private def canTransfer(dealRequest: DealRequest): Result[Seq[(String, Boolean)]] =
    //    // verify against DNA:
    //        ResultOps.foldFailFast {
    //            dealRequest.deal.legs.map { dealLeg =>
    //                Lazy[Result, Seq[(String, Boolean)]] {
    //                    for {
    //                        tokenType <-
    //                            store.getTokenType(dealLeg.tokenId.typeId)
    //                                .toRight(s"No such typeId: ${dealLeg.tokenId.typeId.toB64}")
    //
    //                        _ <- Result.expect(tokenType.dna.transfer.nonEmpty, "Transfer is not allowed by Genes")
    //                        canTransfer <-
    //                            ResultOps.foldFailFast {
    //                                tokenType.dna.transfer.map { g =>
    //                                    Lazy[Result, (String, Boolean)] {
    //                                        for {
    //                                            gene <- GenesRegistry.TransferGenes.get(g.id).toRight(s"No such gene: ${g.id}")
    //                                            context = GeneExecutionContextImpl(store, cryptography, tokenType, OperationInitiator.Client, g.parameters)
    //                                            canTransfer <- gene.canTransfer(context, dealLeg, dealRequest)
    //                                        } yield g.id -> canTransfer
    //                                    }
    //                                }
    //                            }
    //                    } yield canTransfer
    //                }
    //            }
    //        }.map(_.flatten)


    override def makeDeal(dealRequest: DealRequest): Result[OperationEffect] = {
        logger.debug(s"Make deal:\n ${dealRequest.toProtoString}")
        for {
            // pre-validate legs:
            _ <- dealRequest.deal.legs.mapR { leg =>
                for {
                    trs <- store.getTokenRestrictions(leg.tokenId)
                    _ <-
                        Util.expect(
                            trs.restrictions.isEmpty,
                            s"${leg.tokenId} is frozen"
                        )
                    owner <- getTokenOwner(leg.tokenId)
                    verified <- Result(isEqualBytes(leg.previousOwner.address, owner.address) && leg.previousOwner.ownerType == owner.ownerType)
                    _ <-
                        Util.expect(
                            verified,
                            s"${leg.tokenId} old owner is incorrect. Current owner is ${owner.address.mkString(", ")} but not ${leg.previousOwner.address.mkString(", ")}"
                        )
                } yield ()
            }
            _ <- Result.expect(dealRequest.deal.legs.length == dealRequest.deal.legs.map(_.tokenId).toSet.size,
                "repeated token ids in deal not permitted")
            _ <- dealRequest.deal.legs.mapR { leg =>
                leg.newOwner.ownerType match {
                    case OwnerType.Signatures =>
                        for {
                            signatures <- Result(Signatures.parseFrom(leg.newOwner.address))
                            _ <- Result.expect(signatures.require > 0, "Invalid address: required keys count must be greater than 0")
                        } yield ()

                    case OwnerType.SmartContractId =>
                        for {
                            smartContract <- getSmartContract(leg.newOwner.address.toUTF8)
                            state <- getSmartContractState(smartContract.id)
                            _ <- Result.expect(state.alive, "smart contract is not alive")
                        } yield ()

                    case other =>
                        Result.Fail(s"Unsupported owner type: $other")
                }
            }

            // verify against DNA:
            canTransfers <- dealRequest.deal.legs.mapR { dealLeg =>
                for {
                    tokenTypeId <- TokenId.from(dealLeg.tokenId).map(_.typeId)
                    tokenType <-
                        store.getTokenType(tokenTypeId)
                            .toRight(s"No such typeId: $tokenTypeId")

                    _ <- Either.cond(tokenType.dna.transfer.nonEmpty, (), "Transfer is not allowed by Genes")
                    canTransfer <- tokenType.dna.transfer.mapR { g =>
                        for {
                            gene <- GenesRegistry.TransferGenes.get(g.id).toRight(s"No such gene: ${g.id}")
                            context = GeneExecutionContextImpl(store, cryptography, tokenType, OperationInitiator.Client, g.parameters, Map.empty)
                            canTransfer <- gene.canTransfer(context, dealLeg, dealRequest)
                        } yield g.id -> canTransfer
                    }

                } yield canTransfer
            }

            notMeet = canTransfers.flatten.filter(!_._2)
            // if there are some requirements not meet yet - suspend for accepts
            result <- if (notMeet.nonEmpty) {
                logger.debug(s"Suspending deal ${dealRequest.deal.operationId} by Genes: ${notMeet.map(_._1).mkString(", ")}")
                val operationId = dealRequest.deal.operationId
                val pendingAccept =
                    PendingAccept(
                        transactionId = operationId,
                        dealRequest = dealRequest
                    )

                for {
                    tokenTypes <-
                        dealRequest.deal.legs
                            .map(_.tokenId).mapR(id => TokenId.from(id))
                            .flatMap(_.mapR(tokenId => getTokenType(tokenId.typeId)))

                    regulators <- Result(tokenTypes.flatMap(_.regulation.map(_.regulatorId)))
                    issuers <- Result(Collection.empty[String]) //Result(tokenTypes.map(typ => typ.issuerId))
                    operation <-
                        saveOperation(
                            operationId,
                            OperationStatus.AcceptPending,
                            dealRequest,
                            dealRequest.deal.legs.flatMap(leg => Seq(leg.newOwner, leg.previousOwner)).distinct,
                            regulators,
                            issuers //empty issuer does not track
                        )
                } yield operation

                Result(
                    OperationEffect
                        .defaultInstance
                        .withPendingAccepts(
                            Collection(pendingAccept)
                        )
                )
            } else _makeDeal(dealRequest)
        } yield result
    }

    def _makeDeal(dealRequest: DealRequest): Result[OperationEffect] =
        for {

            _ <- Result.expect(dealRequest.deal.legs.length == dealRequest.deal.legs.map(_.tokenId).toSet.size,
                "repeated token ids in deal not permitted")
            //  B) check that there is no linked operations for tokens in legs
            _ <- Either.cond(
                dealRequest.deal.legs.forall { leg =>
                    store.getTokenLinkedOperation(leg.tokenId).isEmpty
                },
                (),
                "There are operations with token already in progress"
            )
            approvalsRequired <- getRequiredRegulation(dealRequest.deal.legs.map(_.tokenId), Collection(RegulatorOperation.DealControl))
            operationEffects <-
                //  check if regulation require
                if (approvalsRequired.nonEmpty) {
                    // and if so put to pending
                    val pendingDeal = PendingDeal(
                        deal = dealRequest,
                        approvals = collectionFromSequence(
                            approvalsRequired.map { regulatorId =>
                                RegulatorApproval(regulatorId, approve = false, reason = "", Bytes.empty, notice = "")
                            }
                        )
                    )
                    for {
                        issuers <- Result(Collection.empty[String])
                        //dealRequest.deal.legs
                        // .map(_.tokenId).mapR(tokenId => TokenId.from(tokenId))
                        // .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                        // .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))
                        _ <-
                            saveOperation(
                                operationId = dealRequest.deal.operationId,
                                status = OperationStatus.DealPendingRegulation,
                                data = pendingDeal,
                                addresses =
                                    dealRequest.deal.legs.flatMap(leg => Seq(leg.newOwner, leg.previousOwner)).distinct,
                                regulators = collectionFromSequence(approvalsRequired),
                                issuers = issuers // empty as issuer don't need to track deals
                            )

                    } yield {
                        dealRequest.deal.legs.foreach { leg =>
                            store.saveTokenLinkedOperation(leg.tokenId, dealRequest.deal.operationId)
                        }
                        OperationEffect
                            .defaultInstance
                            .withPendingDeals(Collection(pendingDeal))
                    }
                } else {
                    // or execute if not
                    for {
                        scResult <- processSCTransfers(dealRequest)
                        ownersOverride <- applyOwnerChanges(dealRequest)
                        effects <- scResult.mapR(r => applySmartContractResult(r._1, r._2, ownersOverride))
                    } yield {
                        val effect = collectEffects(effects)
                        val transfers = effect.transferred
                        effect
                            .clearTransferred
                            .withTransferred(dealRequest +: transfers)
                    }
                }
        } yield operationEffects

    private def collectEffects(effects: Iterable[OperationEffect]): OperationEffect =
        OperationEffect
            .defaultInstance
            .withPendingIssues(collectionFromIterable(effects.flatMap(_.pendingIssues)))
            .withIssued(collectionFromIterable(effects.flatMap(_.issued)))
            .withPendingAccepts(collectionFromIterable(effects.flatMap(_.pendingAccepts)))
            .withPendingDeals(collectionFromIterable(effects.flatMap(_.pendingDeals)))
            .withChange(collectionFromIterable(effects.flatMap(_.change)))
            .withTransferred(collectionFromIterable(effects.flatMap(_.transferred)))
            .withPendingBurns(collectionFromIterable(effects.flatMap(_.pendingBurns)))
            .withBurned(collectionFromIterable(effects.flatMap(_.burned)))
            .withSmartContractUpdates(collectionFromIterable(effects.flatMap(_.smartContractUpdates)))

    private def processSCTransfers(signedDeal: DealRequest): Result[Seq[(SmartContract, SmartContractResult)]] =
        signedDeal.deal.legs.zipWithIndex
            .filter(_._1.newOwner.ownerType == OwnerType.SmartContractId) // take only those that are targeting SC
            .groupBy(a => a._1.newOwner.address.toUTF8) // group legs by address of smart contract
            .mapR { case (sc, legs) =>
                val scAddress = sc
                for {
                    contract <- getSmartContract(scAddress)
                    smartContractImpl <-
                        SmartContractRegistry.get(contract.templateId)
                            .toRight(s"Smart contract $scAddress has invalid template id ${contract.templateId}")

                    contractState <- getSmartContractState(scAddress)

                    tokens <- legs.mapR { case (leg, index) =>
                        getTokenOwner(leg.tokenId).map { owner =>
                            AcceptedToken(
                                id = leg.tokenId,
                                from = owner,
                                leg = index
                            )
                        }
                    }

                    currentDeal = AcceptedDeal(signedDeal, tokens)
                    context =
                        SmartContractOperationContextImpl(
                            contract, contractState, store, txContext, Option(currentDeal)
                        )
                    // execute Smart Contract for each transfer in deal using shared context:
                    result <- smartContractImpl.acceptTransfer(context, tokens)

                    _ <- store.appendSmartContractAcceptedDeal(scAddress, currentDeal)

                } yield (contract, result.copy(stateUpdate = context.stateUpdate))
            }.map(_.toSeq)


    private def applyOwnerChanges(signedDeal: DealRequest): Result[Map[String, TokenOwner]] = {
        for {
            tokenTypes <-
                signedDeal.deal.legs
                    .map(_.tokenId).mapR(token => TokenId.from(token))
                    .flatMap(_.mapR(tokenId => getTokenType(tokenId.typeId)))

            regulators <- Result(tokenTypes.flatMap(_.regulation.map(_.regulatorId)))
            issuers <- Result(Collection.empty[String]) //Result(tokenTypes.map(typ => typ.issuerId))
            _ <-
                saveOperation(
                    signedDeal.deal.operationId,
                    OperationStatus.DealDone,
                    signedDeal,
                    signedDeal.deal.legs.flatMap(leg => Seq(leg.newOwner, leg.previousOwner)).distinct,
                    regulators,
                    issuers // empty as issuer don't need to track deals
                )
            _ <- signedDeal.deal.legs.mapR { leg =>
                store.updateTokenOwner(leg.tokenId, leg.newOwner)
            }
        } yield
            signedDeal.deal.legs.map { leg =>
                (leg.tokenId, leg.newOwner)
            }.toMap
    }

    private val SmartContractRegistry: Map[String, ISmartContract] =
        Seq(ICOSmartContract, IndexTradeSmartContract, IndexTradeRedeemSmartContract)
            .map(x => x.templateInformation.id -> x)
            .toMap

    private def verifyChangeRequest(request: TokenChangeRequestSmartContract, operationInitiator: OperationInitiator): Result[Unit] = {
        for {
            exist <- store.tokenExist(request.toChange)
            _ <- Result.expect(exist, s"token ${request.toChange} not exist")

            tokenOwner <- getTokenOwner(request.toChange)
            _ <- tokenOwner.ownerType match {
                case OwnerType.Signatures =>
                    Result(Signatures.parseFrom(tokenOwner.address))
                        .left.map(_ => "Invalid TokenOwner address")
                case OwnerType.SmartContractId =>
                    getSmartContract(tokenOwner.address.toUTF8)
                case other =>
                    Result.Fail(s"Invalid Token owner type [$other]")
            }

            tokenTypeId <- TokenId.from(request.toChange).map(_.typeId)
            tokenType <- store.getTokenType(tokenTypeId).toRight(s"Unknown typeId: $tokenTypeId")

            _ <- Result.expect(tokenType.dna.change.nonEmpty, "Change is not allowed by Genes")

            _ <- tokenType.dna.change.mapR { g =>
                for {
                    gene <- GenesRegistry.ChangeGenes.get(g.id).toRight(s"Invalid Gene ID: ${g.id}")
                    canChange <- gene.canChange(
                        GeneExecutionContextImpl(store, cryptography, tokenType, operationInitiator, g.parameters, Map.empty),
                        TokenChangeRequest(
                            operationId = "",
                            timestamp = txContext.timestamp,
                            tokenId = request.toChange,
                            amounts = request.toCreate.map(_.value)
                        )
                    )
                    _ <- Result.expect(canChange, s"Change rejected by Gene ${g.id}")
                } yield ()
            }
        } yield ()
    }


    private def applySmartContractResult(
        contract: SmartContract,
        contractResult: SmartContractResult,
        ownersOverride: Map[String, TokenOwner]
    ): Result[OperationEffect] =
        for {
            // apply change tokens:
            changeEffects <- contractResult.change.mapR { request =>
                for {
                    _ <- verifyChangeRequest(request, OperationInitiator.SmartContract(contract.id))

                    tokenContent <- getTokenContent(request.toChange)
                    amountIndex = 0

                    added <- request.toCreate.mapR { token =>
                        val newTokenContent =
                            TokenContent(
                                collectionFromArray(
                                    tokenContent.fields.updated(amountIndex, token.value)
                                )
                            )

                        val owner = TokenOwner(OwnerType.SmartContractId, contract.id.getBytes(StandardCharsets.UTF_8))

                        store.createToken(
                            tokenId = token.id,
                            owner = owner,
                            body = newTokenContent
                        ).map(_ =>
                            TokenAdded(
                                tokenId = token.id,
                                owner = owner,
                                tokenContent = newTokenContent,
                                restrictions = TokenRestrictions.defaultInstance // @ToDo check if set correctly
                            )
                        )

                    }

                    _ <- store.deleteToken(request.toChange)

                } yield TokenChangeResponse(
                    Collection(request.toChange),
                    added
                )
            }

            // apply burn tokens:
            burnEffects <- contractResult.burn.mapR { burnRequest =>
                for {
                    signedRequest <- Result(
                        SignedBurnRequest(
                            request = burnRequest,
                            acceptedAt = txContext.timestamp,
                            signatures = Collection.empty,
                            memberSignature = Bytes.empty
                        )
                    )
                    _ <- validateBurnRequest(signedRequest, OperationInitiator.SmartContract(contract.id), ownersOverride)

                    approvalsRequired <- getRequiredRegulation(
                        burnRequest.tokens,
                        Collection(RegulatorOperation.BurnControl)
                    )
                    owners <- burnRequest.tokens.mapR(tokenId => getTokenOwner(tokenId))
                    effect <-
                        if (approvalsRequired.nonEmpty) {
                            val pendingBurn = PendingBurn(
                                burnRequest = signedRequest,
                                owners = owners,
                                approvals = collectionFromSequence(
                                    approvalsRequired.map { regulatorId =>
                                        RegulatorApproval(regulatorId, approve = false, reason = "", Bytes.empty, notice = "")
                                    }
                                )
                            )

                            for {
                                _ <- saveOperation(
                                    burnRequest.operationId,
                                    OperationStatus.BurnPendingRegulation,
                                    pendingBurn,
                                    owners.distinct,
                                    collectionFromSequence(approvalsRequired),
                                    issuers = Collection.empty[String]
                                )

                                _ <- burnRequest.tokens.mapR { id =>
                                    Result {
                                        store.saveTokenLinkedOperation(id, burnRequest.operationId)
                                    }
                                }
                            } yield OperationEffect
                                .defaultInstance
                                .withPendingBurns(Collection(pendingBurn))
                        } else
                            _burnToken(signedRequest)
                } yield effect
            }

            // apply emission:
            emissionEffects <- contractResult.emission.mapR { request =>
                for {
                    _ <- verifyIssueTokens(request, OperationInitiator.SmartContract(contract.id))
                    approvalsRequired <-
                        getRequiredRegulation(
                            request.issue.tokens.map(_.tokenId),
                            Collection(RegulatorOperation.IssueControl)
                        )
                    effect <-
                        if (approvalsRequired.nonEmpty) {
                            val pendingIssue = PendingIssue(
                                timestamp = txContext.timestamp,
                                request = request,
                                approvals = collectionFromSequence(
                                    approvalsRequired.map { regulatorId =>
                                        RegulatorApproval(regulatorId, approve = false, reason = "", Bytes.empty, notice = "")
                                    }
                                )
                            )

                            saveOperation(
                                request.issue.operationId,
                                OperationStatus.IssuePendingRegulation,
                                pendingIssue,
                                request.issue.tokens.map(_.owner).distinct,
                                collectionFromSequence(approvalsRequired),
                                issuers = Collection(contract.issuerId)
                            ).map { _ =>
                                request.issue.tokens.foreach { issue =>
                                    store.saveTokenLinkedOperation(issue.tokenId, request.issue.operationId)
                                }
                                OperationEffect
                                    .defaultInstance
                                    .withPendingIssues(Collection(pendingIssue))
                            }
                        } else
                            _issueToken(request)
                } yield effect

            }

            // apply transfer:
            transferEffects <- contractResult.transfer.mapR { request =>
                for {
                    approvalsRequired <- getRequiredRegulation(collectionFromSequence(request.deal.legs.map(leg => leg.tokenId)), Collection(RegulatorOperation.DealControl))
                    operationEffects <- {
                        if (approvalsRequired.nonEmpty) {
                            val pendingDeal = PendingDeal(
                                request,
                                approvals = collectionFromSequence(approvalsRequired.map { regulatorId =>
                                    RegulatorApproval(regulatorId, approve = false, reason = "", Bytes.empty, notice = "") // ???
                                })
                            )
                            for {
                                issuers <- Result(Collection.empty[String])
                                //                                    request.deal.legs
                                //                                        .map(_.tokenId)
                                //                                        .mapR(tokenId => TokenId.from(tokenId))
                                //                                        .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                                //                                        .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))

                                _ <-
                                    saveOperation(
                                        request.deal.operationId,
                                        OperationStatus.DealPendingRegulation,
                                        pendingDeal,
                                        request.deal.legs.flatMap(leg => Seq(leg.newOwner, leg.previousOwner)).distinct,
                                        collectionFromSequence(approvalsRequired),
                                        issuers = issuers // empty, issuer does not track
                                    )
                            } yield {
                                request.deal.legs.foreach { leg => store.saveTokenLinkedOperation(leg.tokenId, request.deal.operationId) }
                                OperationEffect
                                    .defaultInstance
                                    .withPendingDeals(Collection(pendingDeal))
                            }
                        } else {
                            // or execute if not
                            for {
                                issuers <- Result(Collection.empty[String])
                                //                                    request.deal.legs
                                //                                        .map(_.tokenId).mapR(tokenId => TokenId.from(tokenId))
                                //                                        .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                                //                                        .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))

                                _ <- request.deal.legs.mapR(leg => store.updateTokenOwner(leg.tokenId, leg.newOwner))
                                operation <-
                                    saveOperation(
                                        request.deal.operationId,
                                        OperationStatus.DealDone,
                                        request,
                                        request.deal.legs.flatMap(leg => Seq(leg.newOwner, leg.previousOwner)).distinct,
                                        collectionFromSequence(approvalsRequired),
                                        issuers // empty, issuer does not track
                                    )
                            } yield operation

                            Result(
                                OperationEffect
                                    .defaultInstance
                                    .withTransferred(Collection(request))
                            )
                        }
                    }

                } yield operationEffects
            }

            _ <- Result {
                contractResult.stateUpdate.foreach { state =>
                    store.saveSmartContractState(state)

                    if (!state.alive) {
                        contract.feeds.foreach { feed =>
                            store.deleteDataFeedSubscriber(feed, contract.id)
                        }
                    }
                }
            }
        } yield
            OperationEffect
                .defaultInstance
                .withChange(changeEffects)
                .withBurned(burnEffects.flatMap(_.burned))
                .withPendingBurns(burnEffects.flatMap(_.pendingBurns))
                .withIssued(emissionEffects.flatMap(_.issued))
                .withPendingIssues(emissionEffects.flatMap(_.pendingIssues))
                .withTransferred(transferEffects.flatMap(_.transferred))
                .withPendingDeals(transferEffects.flatMap(_.pendingDeals))
                .withSmartContractUpdates(contractResult.stateUpdate.toArray)


    private def validateBurnRequestFromClient(request: SignedBurnRequest, operationInitiator: OperationInitiator): Result[Unit] =
        for {
            _ <- request.request.tokens.mapR { tokenId =>
                for {
                    trs <- store.getTokenRestrictions(tokenId)
                    _ <- Util.expect(
                        trs.restrictions.isEmpty,
                        s"$tokenId is frozen"
                    )
                    _ <- Either.cond(
                        store.getTokenLinkedOperation(tokenId).isEmpty,
                        (),
                        "There are already operations on tokenId in progress"
                    )
                } yield ()
            }
            _ <- validateBurnRequest(request, operationInitiator, Map.empty)
        } yield ()

    private def validateBurnRequest(request: SignedBurnRequest, operationInitiator: OperationInitiator, ownersOverride: Map[String, TokenOwner]): Result[Unit] =
        request.request.tokens.mapR { tokenId =>
            for {
                tokenTypeId <- TokenId.from(tokenId).map(_.typeId)
                tokenType <-
                    store.getTokenType(tokenTypeId)
                        .toRight(s"Invalid typeId for token: $tokenId")

                _ <- Either.cond(tokenType.dna.burn.nonEmpty, (), "Burn is not allowed by Genes")
                _ <-
                    tokenType.dna.burn.mapR { g =>
                        for {

                            gene <- GenesRegistry.BurnGenes.get(g.id).toRight(s"No such gene: ${g.id}")
                            context = GeneExecutionContextImpl(store, cryptography, tokenType, operationInitiator, g.parameters, ownersOverride)
                            canBurn <- gene.canBurn(context, tokenId, request)
                            _ <- Either.cond(canBurn, (), s"Burn rejected by genes for token $tokenId")
                        } yield canBurn
                    }
            } yield ()
        }.map(_ => ())

    override def burnTokens(request: SignedBurnRequest): Result[OperationEffect] = {
        // set request "Accepted At" timestamp (to Tx timestamp which is from the Gate)
        val burnRequest = request.withAcceptedAt(txContext.timestamp)

        for {
            owners <- burnRequest.request.tokens.mapR(tokenId => getTokenOwner(tokenId))
            _ <- validateBurnRequestFromClient(request, OperationInitiator.Client) // Note: there is signature check under the hood so use original request here
            approvalsRequired <- getRequiredRegulation(burnRequest.request.tokens, Collection(RegulatorOperation.BurnControl))
            result <-
                if (approvalsRequired.nonEmpty) {
                    val pendingBurn = PendingBurn(
                        burnRequest = burnRequest,
                        owners = owners,
                        approvals = collectionFromSequence(
                            approvalsRequired.map { regulatorId =>
                                RegulatorApproval(regulatorId, approve = false, reason = "", Bytes.empty, notice = "")
                            }
                        )
                    )
                    for {
                        issuers <-
                            burnRequest.request.tokens
                                .mapR(tokenId => TokenId.from(tokenId))
                                .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                                .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))

                        _ <-
                            saveOperation(
                                burnRequest.request.operationId,
                                OperationStatus.BurnPendingRegulation,
                                pendingBurn,
                                owners,
                                collectionFromIterable(approvalsRequired),
                                issuers
                            )
                    } yield {

                        burnRequest.request.tokens.foreach { tokenId =>
                            store.saveTokenLinkedOperation(tokenId, burnRequest.request.operationId)
                        }

                        OperationEffect
                            .defaultInstance
                            .withPendingBurns(Collection(pendingBurn))
                    }

                } else _burnToken(burnRequest)

        } yield result
    }


    private def _burnToken(burnRequest: SignedBurnRequest): Result[OperationEffect] =
        burnRequest.request.tokens.mapR { tokenId =>
            for {
                content <- getTokenContent(tokenId)
                restrictions <- getTokenRestrictions(tokenId)
                owner <- getTokenOwner(tokenId)
                _ <- deleteToken(tokenId, Collection(burnRequest.request.operationId), WalletTokenStatus.Burnt)
            } yield BurntTokenData(content, restrictions, owner)
        }.flatMap { tokens =>
            for {
                tokenTypes <-
                    burnRequest.request.tokens
                        .mapR(token => TokenId.from(token))
                        .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))

                regulators <- Result(tokenTypes.flatMap(_.regulation.map(_.regulatorId)))
                issuers <- Result(tokenTypes.map(_.issuerId))
                response <- Result(BurnResponse(burnRequest, tokens))
                _ <-
                    saveOperation(
                        burnRequest.request.operationId,
                        OperationStatus.BurnDone,
                        response,
                        tokens.map(_.owner),
                        regulators,
                        issuers
                    )
            } yield OperationEffect
                .defaultInstance
                .withBurned(
                    Collection(response)
                )
        }

    override def listBurntTokens: Result[Collection[WalletToken]] =
        store.listBurntTokens.mapR(burntToken =>
            getBurntToken(burntToken.tokenId)
        )

    override def getBurntToken(tokenId: String): Result[WalletToken] =
        ResultOps.fromOption(store.getBurntToken(tokenId), s"Token $tokenId is not burnt")
            .flatMap(
                burntToken =>
                    for {
                        content <- getTokenContent(burntToken.tokenId).map(_.fields)
                        restrictions <- getTokenRestrictions(burntToken.tokenId).map(_.restrictions)
                        operations <- getTokenLinkedOperations(burntToken.tokenId)
                    } yield WalletToken(
                        id = burntToken.tokenId,
                        content = content,
                        restrictions = restrictions,
                        tokenOwner = burntToken.lastOwner,
                        operations = operations,
                        status = burntToken.status
                    )
            )

    override def getTokensByTypeId(typeId: String): Result[Collection[WalletToken]] =
        store
            .listTokens
            .filterR { tokenId =>
                TokenId.from(tokenId).map(_.typeId == typeId)
            }
            .flatMap(_.mapR(getToken))

    // =====================================================================================================================
    // Regulator
    // =====================================================================================================================

    override def regulatorBurnToken(request: RegulatorBurnRequest): Result[Unit] = {
        val regulatorId = request.signature.memberId
        val signature = request.signature.value
        for {
            owners <- request.request.tokens.mapR(tokenId => getTokenOwner(tokenId))
            regulator <- getMember(regulatorId)
            requestBytes = request.request.toBytes
            _ <-
                request.request.tokens.mapR { tokenId =>
                    for {
                        tokenTypeId <- TokenId.from(tokenId).map(_.typeId)
                        tType <- store.getTokenType(tokenTypeId).toRight("unknown type")
                        _ <- Util.expect(
                            tType.regulation.exists(r =>
                                r.regulatorId == regulatorId &&
                                    r.capabilities.contains(RegulatorOperation.Burn)
                            ),
                            s"$regulatorId is not permitted to burn token $tokenId"
                        )
                        trs <- store.getTokenRestrictions(tokenId)
                        restriction = trs.restrictions.find(_.regulatorId != regulatorId)
                        _ <- Util.expect(restriction.isEmpty, s"Token  $tokenId is frozen by ${restriction.map(_.regulatorId).getOrElse("")}")

                        signatureOk <- cryptography.verifySignature(
                            regulator.signingPublic, requestBytes, signature
                        )
                        _ <- Util.expect(signatureOk, "Invalid signature")
                        _ <- deleteToken(tokenId, Collection(request.request.operationId), WalletTokenStatus.Burnt)
                    } yield ()
                }

            issuers <-
                request.request.tokens
                    .mapR(tokenId => TokenId.from(tokenId))
                    .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                    .map(tokenTypes => tokenTypes.map(_.issuerId))

            _ <-
                saveOperation(
                    request.request.operationId,
                    OperationStatus.BurnDoneByRegulator,
                    request.request,
                    owners,
                    Collection(regulatorId),
                    issuers
                )
        } yield ()
    }


    override def freezeToken(requests: Array[TokenFreezeRequest]): Result[Unit] =
        requests.mapR { request =>
            request.tokenIds.mapR { tokenId =>
                val challenge = CNFTChallenge.freezeTokenByRegulator(request.freeze, request.tokenIds)
                for {
                    regulatorInfo <- getMember(request.regulatorId)
                    signatureOk <- cryptography.verifySignature(regulatorInfo.signingPublic, challenge, request.signature)
                    _ <- Util.expect(signatureOk, "Invalid signature")

                    tokenTypeId <- TokenId.from(tokenId).map(_.typeId)
                    tType <- store.getTokenType(tokenTypeId).toRight(s"unknown type $tokenTypeId")
                    _ <- Util.expect(
                        // is there Regulator with such Id and Capability to Freeze/Unfreeze ?
                        tType.regulation.exists(r =>
                            r.regulatorId == request.regulatorId &&
                                r.capabilities.contains(RegulatorOperation.Freeze)
                        ),
                        s"${request.regulatorId} is not permitted to un/freeze token $tokenId"
                    )

                    trs <- store.getTokenRestrictions(tokenId)

                    _ <- if (request.freeze) // to freeze token must not be frozen
                        Util.expect(
                            trs.restrictions.isEmpty,
                            s"$tokenId is already frozen"
                        ).flatMap { _ =>
                            store.addTokenRestriction(
                                tokenId,
                                Restriction(
                                    request.regulatorId,
                                    request.restrictionId
                                )
                            )
                        }
                    else // to un freeze token must be frozen by this regulator
                        Util.expect(
                            trs.restrictions.exists(_.regulatorId == request.regulatorId),
                            s"$tokenId is not frozen by ${request.regulatorId}"
                        ).flatMap { _ =>
                            store.removeTokenRestriction(
                                tokenId,
                                request.regulatorId
                            )

                        }
                } yield ()
            }
        }.map(_.flatten).map(_ => ())


    override def regulatorTransfer(request: RegulatorTransferRequest): Result[Unit] = {
        val owners = request.deal.legs.map(leg => leg.newOwner) ++ request.deal.legs.map(leg => leg.previousOwner)
        for {
            regulatorInfo <- getMember(request.signature.memberId)
            dealBytes = request.deal.toByteArray
            dealSignature = request.signature.value
            signatureOk <- cryptography.verifySignature(regulatorInfo.signingPublic, dealBytes, dealSignature)
            _ <- Util.expect(signatureOk, "Invalid signature")

            changesToApply <- request.deal.legs.mapR { case DealLeg(tokenId, newOwner, _, _, _) =>
                val regulatorId = request.signature.memberId
                for {
                    tokenTypeId <- TokenId.from(tokenId).map(_.typeId)
                    tType <- store.getTokenType(tokenTypeId).toRight(s"unknown type $tokenTypeId")
                    _ <- Util.expect(
                        tType.regulation.exists(r =>
                            r.regulatorId == regulatorId &&
                                r.capabilities.contains(RegulatorOperation.Transfer)
                        ),
                        s"$regulatorId is not permitted to transfer token $tokenId"
                    )
                    trs <- store.getTokenRestrictions(tokenId)
                    restriction = trs.restrictions.find(_.regulatorId != regulatorId)
                    _ <- Util.expect(restriction.isEmpty, s"Token  $tokenId is frozen by ${restriction.map(_.regulatorId).getOrElse("")}")

                } yield (tokenId, newOwner)
            }

            issuers <- Result(Collection.empty[String])
            //request.deal.legs
            // .map(_.tokenId).mapR(tokenId => TokenId.from(tokenId))
            // .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
            // .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))
            _ <-
                saveOperation(
                    request.deal.operationId,
                    OperationStatus.DealDoneByRegulator,
                    request,
                    owners.distinct,
                    Collection(request.signature.memberId),
                    issuers // empty, issuer does not track movement of the tokens
                )
        } yield {
            changesToApply.foreach { case (tokenId, newOwner) =>
                store.updateTokenOwner(tokenId, newOwner)
            }
        }
    }

    override def regulatorChangeToken(request: RegulatorSignedTokenChangeRequest): Result[TokenChangeResponse] = {
        val Amount = "amount"
        for {
            regulatorIdentity <- getMember(request.memberSignature.memberId)
            signatureOk <- cryptography.verifySignature(
                regulatorIdentity.signingPublic,
                request.tokenChangeRequest.toBytes,
                request.memberSignature.value
            )
            _ <- Util.expect(signatureOk, "Invalid signature for approve transaction")
            tokenTypeId <- TokenId.from(request.tokenChangeRequest.tokenId).map(_.typeId)
            tokenType <- store.getTokenType(tokenTypeId)
                .toRight(s"No such typeId: $tokenTypeId")
            tokenOwner <- getTokenOwner(request.tokenChangeRequest.tokenId)

            _ <- Either.cond(tokenType.dna.change.nonEmpty, (), "Change is not allowed by Genes")
            tokenId = request.tokenChangeRequest.tokenId
            tokenContent <- store.getTokenBody(tokenId).toRight(s"Token $tokenId does not exist")
            amountIndex <- findTokenFieldIndex(tokenType, Amount)
            _ <- tokenType.dna.change.mapR { g =>
                for {
                    gene <- GenesRegistry.ChangeGenes.get(g.id).toRight(s"No such gene: ${g.id}")
                    context =
                        GeneExecutionContextImpl(
                            store, cryptography, tokenType, OperationInitiator.Client, g.parameters, Map.empty
                        )
                    canChange <- gene.canChange(context, request.tokenChangeRequest)
                    _ <- Either.cond(canChange, (), s"Change is not allowed by gen ${g.id}")
                } yield ()
            }

            restrictions <- getTokenRestrictions(request.tokenChangeRequest.tokenId)

            _ <-
                if (restrictions.restrictions.nonEmpty)
                    Util.expect(
                        restrictions.restrictions.exists(r => r.regulatorId == regulatorIdentity.id),
                        s"This regulator ${regulatorIdentity.id} did not set restrictions for this token $tokenId"
                    )
                else
                    Result(())

            tokenTypeId <- TokenId.from(request.tokenChangeRequest.tokenId).map(_.typeId)

            added <- request.tokenChangeRequest.amounts.mapR { amount =>
                val newTokenContent = TokenContent(
                    collectionFromArray(
                        tokenContent.fields.updated(
                            amountIndex, amount)
                    )
                )

                TokenId.encode(tokenTypeId, txContext.nextUniqueId).flatMap { tokenId =>
                    store.createToken(
                        tokenId = tokenId,
                        owner = tokenOwner,
                        body = newTokenContent
                    ).map(_ =>
                        TokenAdded(
                            tokenId = tokenId,
                            owner = tokenOwner,
                            tokenContent = newTokenContent,
                            restrictions = restrictions
                        )
                    )
                }
            }

            _ <- updateRestrictions(request.tokenChangeRequest, added)
            _ <- deleteToken(request.tokenChangeRequest.tokenId, Collection.empty, WalletTokenStatus.Changed)
        } yield TokenChangeResponse(
            added = added,
            deleted = Collection(request.tokenChangeRequest.tokenId)
        )
    }


    // =====================================================================================================================
    // Offers
    // =====================================================================================================================

    override def putOffers(putOfferRequests: Array[PutOfferRequest]): Result[Array[Offer]] = Result {
        putOfferRequests.map { request =>
            val offerId = txContext.nextUniqueId
            val offer = Offer(
                id = offerId,
                owner = request.offerOwner,
                supply = request.supply,
                demand = request.demand
            )
            store.saveOffer(offerId, offer)
            offer
        }
    }

    override def closeOffers(offerIds: Array[String]): Result[Unit] = Result {
        offerIds.foreach {
            offerId =>
                logger.debug(s"Deleting offer $offerId")
                store.deleteOffer(offerId)
        }
    }

    // =====================================================================================================================
    // Messages
    // =====================================================================================================================

    override def publishMessages(messages: Array[Message]): Result[Collection[MessageRequest]] =
        for {
            _ <- Util.expect(messages.nonEmpty, "Messages are empty")
            messagesRequest =
                messages.map { m =>
                    MessageRequest(
                        message = m,
                        txId = txContext.txId,
                        timestamp = txContext.timestamp
                    )
                }
            _ <- Result {
                messagesRequest.foreach { request =>
                    store.saveMessages(
                        txContext.nextUniqueId,
                        request
                    )
                }
            }
        } yield messagesRequest


    override def listMessages(to: String): Result[Collection[MessageRequest]] =
        Result(store.listMessages(to))

    override def listMessagesFrom(from: String): Result[Collection[MessageRequest]] =
        Result(store.listMessagesFrom(from))

    // =====================================================================================================================
    // Members
    // =====================================================================================================================

    private val curve = ECNamedCurveTable.getParameterSpec("secp256k1")
    private val rs = new RingSigner[Result, ECPoint](
        BouncyCastleHasher,
        new EllipticOps(curve, compressed = false),
        (BigInteger.ZERO, null) // TODO: split the Signer
    )

    private def verifySignature(key: Bytes, data: Bytes, signature: Bytes): Result[Unit] =
        for {
            signatureOk <- cryptography.verifySignature(key, data, signature)
            _ <- Result.expect(signatureOk, "Invalid signature")
        } yield ()

    private def registerMemberInternal(member: MemberInformation, block: Long): Result[String] =
        for {
            _ <- rs.isPublicKeyValid(member.accessPublic)
            _ <- Result(store.saveMemberInfo(member))
            _ <- Result(store.saveMemberRegistrationBlock(member.id, block))
        } yield member.id


    override def registerMember(request: RegisterMemberRequest): Result[String] =
        store.getMemberInfo(request.memberId) match {
            case Some(registrar) =>
                if (registrar.isAdmin) {
                    if (store.getMemberInfo(request.memberInformation.id).isEmpty) {
                        verifySignature(registrar.signingPublic, request.memberInformation.toBytes, request.signature).flatMap { _ =>
                            registerMemberInternal(request.memberInformation, request.block)
                        }
                    } else Result.Fail(s"Member already registered [${request.memberInformation.id}]")
                } else Result.Fail(s"Member is not an admin [${request.memberId}]")

            case None =>
                if (store.listMemberInfo.isEmpty) {
                    // the member who trying to register does not exist, the only allowed case is initial admin registration:
                    verifySignature(request.memberInformation.signingPublic, request.memberInformation.toBytes, request.signature).flatMap { _ =>
                        registerMemberInternal(
                            request.memberInformation.copy(isAdmin = true),
                            request.block
                        )
                    }
                } else Result.Fail(s"Member does not exist [${request.memberId}] ")
        }


    override def updateMember(request: UpdateMemberInformationRequest): Result[Unit] =
        store.getMemberInfo(request.update.id) match {
            case Some(_) =>
                for {
                    requester <- ResultOps.fromOption(store.getMemberInfo(request.signature.memberId), s"Member [${request.signature.memberId}] does not exists")
                    _ <- Result.expect(requester.isAdmin, s"Requester [${request.signature.memberId}] is not an admin")
                    signatureOk <- cryptography.verifySignature(requester.signingPublic, request.update.toBytes, request.signature.value)
                    _ <- Result.expect(signatureOk, s"invalid signature")
                    _ <- rs.isPublicKeyValid(request.update.accessPublic)
                    _ <- Result(store.saveMemberInfo(request.update))
                } yield ()

            case None =>
                Result.Fail(s"Member does not exists [${request.update.id}]")
        }

    override def getMember(id: String): Result[MemberInformation] =
        store.getMemberInfo(id).toRight(s"No member with id $id")

    override def listMembers: Result[Array[MemberInformation]] = Result {
        store.listMemberInfo
    }

    override def getMemberRegistrationBlock(memberId: String): Result[Long] =
        store.getMemberRegistrationBlock(memberId).toRight(s"No block number of $memberId registration")

    override def approveTransaction(request: SignedTXRegulationRequest): Result[OperationEffect] =
        for {
            owner <- getMember(request.request.regulatorId)
            signatureOk <- cryptography.verifySignature(owner.signingPublic, request.request.toBytes, request.signature)
            _ <- Util.expect(signatureOk, "Invalid signature for approve transaction")
            // store.getTokenLinkedOperation(leg.tokenId) // TODO checkLinkedOperation
            operation <-
                store.getOperation(request.request.transactionId)
                    .flatMap(_.data.history.lastOption)
                    .toRight(s"Operation does not exist: ${request.request.transactionId}")

            result <- operation.state match {
                case OperationStatus.DealPendingRegulation =>
                    val pendingDeal = PendingDeal.parseFrom(operation.data)
                    val updated = pendingDeal.copy(
                        approvals = pendingDeal.approvals.map { approve =>
                            if (approve.regulatorId == request.request.regulatorId) {
                                approve.copy(
                                    approve = true,
                                    signature = request.signature
                                )
                            } else approve
                        }
                    )
                    if (updated.approvals.forall(_.approve == true)) {
                        val dealRequest = pendingDeal.deal
                        processSCTransfers(dealRequest) match {
                            case Right(scResult) =>
                                for {
                                    _ <- Result {
                                        pendingDeal.deal.deal.legs.foreach { leg =>
                                            store.removeTokenLinkedOperation(leg.tokenId)
                                        }
                                    }
                                    ownersOverride <- applyOwnerChanges(dealRequest)
                                    effects <- scResult.mapR(r => applySmartContractResult(r._1, r._2, ownersOverride))
                                } yield {
                                    val effect = collectEffects(effects)
                                    val transfers = effect.transferred
                                    val pending = effect.pendingDeals
                                    effect
                                        .withPendingDeals(pending :+ updated)
                                        .clearTransferred
                                        .withTransferred(dealRequest +: transfers)
                                }

                            case Left(msg) =>
                                logger.warn(s"Failed to apply deal ${pendingDeal.deal.deal.operationId}: $msg")
                                for {
                                    _ <- logger.warnR(s"Failed to apply deal ${pendingDeal.deal.deal.operationId}: $msg")
                                    _ <- Result {
                                        pendingDeal.deal.deal.legs.foreach { leg =>
                                            store.removeTokenLinkedOperation(leg.tokenId)
                                        }
                                    }
                                    _ <- saveOperation(
                                        dealRequest.deal.operationId,
                                        OperationStatus.DealRejectedBySmartContract,
                                        DealRejectedBySmartContract(
                                            dealRequest,
                                            msg
                                        ),
                                        dealRequest.deal.legs.flatMap(leg => Seq(leg.newOwner, leg.previousOwner)).distinct,
                                        pendingDeal.approvals.map(_.regulatorId),
                                        Collection.empty[String] // empty, issuer does not track
                                    )
                                } yield
                                    OperationEffect.defaultInstance.withPendingDeals(Collection(updated))
                        }
                    } else {
                        for {
                            issuers <- Result(Collection.empty[String])
                            //updated.deal.deal.legs
                            // .map(_.tokenId)
                            // .mapR(tokenId => TokenId.from(tokenId))
                            // .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                            // .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))

                            operation <-
                                saveOperation(
                                    updated.deal.deal.operationId,
                                    OperationStatus.DealPendingRegulation,
                                    updated,
                                    updated.deal.deal.legs.flatMap(leg => Seq(leg.newOwner, leg.previousOwner)).distinct,
                                    updated.approvals.map(_.regulatorId),
                                    issuers // empty, issuer does not track
                                )
                        } yield operation

                        Result(
                            OperationEffect
                                .defaultInstance
                                .withPendingDeals(Collection(updated))
                        )
                    }

                case OperationStatus.BurnPendingRegulation =>
                    val pendingBurn = PendingBurn.parseFrom(operation.data)
                    val updated = pendingBurn.copy(
                        approvals = pendingBurn.approvals.map { approve =>
                            if (approve.regulatorId == request.request.regulatorId) {
                                approve.copy(
                                    approve = true,
                                    signature = request.signature
                                )
                            } else approve
                        }
                    )
                    if (updated.approvals.forall(_.approve == true)) {
                        pendingBurn.burnRequest.request.tokens.foreach { tokenId =>
                            store.removeTokenLinkedOperation(tokenId)
                        }
                        _burnToken(pendingBurn.burnRequest)
                            .map(effect => effect.copy(pendingBurns = effect.pendingBurns :+ updated))

                    } else
                        for {
                            owners <- pendingBurn.burnRequest.request.tokens.mapR(tokenId => getTokenOwner(tokenId))
                            issuers <-
                                updated.burnRequest.request.tokens
                                    .mapR(tokenId => TokenId.from(tokenId))
                                    .flatMap(_.mapR(tokenId => getTokenType(tokenId.typeId)))
                                    .map(_.map(_.issuerId))

                            _ <-
                                saveOperation(
                                    updated.burnRequest.request.operationId,
                                    OperationStatus.BurnPendingRegulation,
                                    updated,
                                    owners.distinct,
                                    pendingBurn.approvals.map(_.regulatorId),
                                    issuers
                                )
                        } yield
                            OperationEffect
                                .defaultInstance
                                .withPendingBurns(Collection(updated))


                case OperationStatus.IssuePendingRegulation =>
                    val pendingIssue = PendingIssue.parseFrom(operation.data)
                    val updated = pendingIssue.copy(
                        approvals = pendingIssue.approvals.map { approve =>
                            if (approve.regulatorId == request.request.regulatorId) {
                                approve.copy(
                                    approve = true,
                                    signature = request.signature
                                )
                            } else approve
                        }
                    )
                    if (updated.approvals.forall(_.approve == true)) {
                        pendingIssue.request.issue.tokens.foreach { issue =>
                            store.removeTokenLinkedOperation(issue.tokenId)
                        }
                        _issueToken(pendingIssue.request)
                            .map(_.withPendingIssues(Collection(updated)))
                    } else {
                        for {
                            issuers <-
                                updated.request.issue.tokens
                                    .map(_.tokenId).mapR(tokenId => TokenId.from(tokenId))
                                    .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                                    .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))
                            _ <-
                                saveOperation(
                                    updated.request.issue.operationId,
                                    OperationStatus.IssuePendingRegulation,
                                    updated,
                                    pendingIssue.request.issue.tokens.map(_.owner).distinct,
                                    updated.approvals.map(_.regulatorId),
                                    issuers
                                )
                        } yield
                            OperationEffect
                                .defaultInstance
                                .withPendingIssues(Collection(updated))

                    }

                case state => Result.Fail(s"Operation does not require regulation: $state ")
            }
        } yield result

    override def addNotice(request: SignedTXRegulationNotification): Result[OperationEffect] =
        for {
            owner <- getMember(request.request.regulatorId)
            signatureOk <- cryptography.verifySignature(owner.signingPublic, request.request.toBytes, request.signature)
            _ <- Util.expect(signatureOk, "Invalid signature for approve transaction")
            operation <-
                store.getOperation(request.request.transactionId)
                    .flatMap(_.data.history.lastOption)
                    .toRight(s"Operation does not exist: ${request.request.transactionId}")

            result <- operation.state match {
                case OperationStatus.DealPendingRegulation =>
                    val pendingDeal = PendingDeal.parseFrom(operation.data)
                    val updated = pendingDeal.copy(
                        approvals = pendingDeal.approvals.map { approve =>
                            if (approve.regulatorId == request.request.regulatorId) {
                                approve.copy(
                                    notice = request.request.notice,
                                    signature = request.signature
                                )
                            } else approve
                        }
                    )
                    for {
                        issuers <- Result(Collection.empty[String])
                        _ <-
                            saveOperation(
                                updated.deal.deal.operationId,
                                OperationStatus.DealPendingRegulation,
                                updated,
                                updated.deal.deal.legs.flatMap(leg => Seq(leg.newOwner, leg.previousOwner)).distinct,
                                updated.approvals.map(_.regulatorId),
                                issuers // issuer does not track
                            )
                    } yield
                        OperationEffect
                            .defaultInstance
                            .withPendingDeals(Collection(updated))


                case OperationStatus.BurnPendingRegulation =>
                    val pendingBurn = PendingBurn.parseFrom(operation.data)
                    val updated = pendingBurn.copy(
                        approvals = pendingBurn.approvals.map { approve =>
                            if (approve.regulatorId == request.request.regulatorId) {
                                approve.copy(
                                    notice = request.request.notice,
                                    signature = request.signature
                                )
                            } else approve
                        }
                    )

                    for {
                        owners <- updated.burnRequest.request.tokens.mapR(tokenId => getTokenOwner(tokenId))
                        issuers <-
                            updated.burnRequest.request.tokens
                                .mapR(tokenId => TokenId.from(tokenId))
                                .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                                .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))
                        _ <-
                            saveOperation(
                                updated.burnRequest.request.operationId,
                                OperationStatus.BurnPendingRegulation,
                                updated,
                                owners.distinct,
                                updated.approvals.map(_.regulatorId),
                                issuers
                            )
                    } yield
                        OperationEffect
                            .defaultInstance
                            .withPendingBurns(Collection(updated))

                case OperationStatus.IssuePendingRegulation =>
                    val pendingIssue = PendingIssue.parseFrom(operation.data)
                    val updated = pendingIssue.copy(
                        approvals = pendingIssue.approvals.map { approve =>
                            if (approve.regulatorId == request.request.regulatorId) {
                                approve.copy(
                                    notice = request.request.notice,
                                    signature = request.signature
                                )
                            } else approve
                        }
                    )

                    for {
                        owners <- Result(updated.request.issue.tokens.map(token => token.owner))
                        issuers <-
                            updated.request.issue.tokens
                                .map(_.tokenId).mapR(tokenId => TokenId.from(tokenId))
                                .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                                .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))
                        _ <-
                            saveOperation(
                                updated.request.issue.operationId,
                                OperationStatus.IssuePendingRegulation,
                                updated,
                                owners.distinct,
                                updated.approvals.map(_.regulatorId),
                                issuers
                            )
                    } yield
                        OperationEffect
                            .defaultInstance
                            .withPendingIssues(Collection(updated))
            }
        } yield result

    override def rejectTransaction(request: SignedTXRegulationRequest): Result[OperationEffect] =
        for {
            owner <- getMember(request.request.regulatorId)
            signatureOk <- cryptography.verifySignature(owner.signingPublic, request.request.toBytes, request.signature)
            _ <- Util.expect(signatureOk, "Invalid signature for reject transaction")
            operation <-
                store.getOperation(request.request.transactionId)
                    .flatMap(_.data.history.lastOption)
                    .toRight(s"Operation does not exist: ${request.request.transactionId}")
            //getPendingOperation(request.request.transactionId).toRight(s"No pending deal for: ${request.request.transactionId}")
            result <-
                operation.state match {
                    case OperationStatus.DealPendingRegulation =>
                        val pendingDeal = PendingDeal.parseFrom(operation.data)
                        val updated = pendingDeal.copy(
                            approvals = pendingDeal.approvals.map { approve =>
                                if (approve.regulatorId == request.request.regulatorId) {
                                    approve.copy(
                                        reason = request.request.reason,
                                        signature = request.signature
                                    )
                                } else approve
                            }
                        )
                        for {
                            _ <- Result {
                                updated.deal.deal.legs.foreach { dealLeg =>
                                    store.removeTokenLinkedOperation(dealLeg.tokenId)
                                }
                            }
                            issuers <- Result(Collection.empty[String])
                            //updated.deal.deal.legs
                            // .map(_.tokenId).mapR(tokenId => TokenId.from(tokenId))
                            // .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                            // .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))
                            _ <-
                                saveOperation(
                                    updated.deal.deal.operationId,
                                    OperationStatus.DealRejectedByRegulation,
                                    updated,
                                    updated.deal.deal.legs.flatMap(leg => Seq(leg.newOwner, leg.previousOwner)).distinct,
                                    updated.approvals.map(_.regulatorId),
                                    issuers // issuer does not track
                                )
                        } yield
                            OperationEffect
                                .defaultInstance
                                .withPendingDeals(Collection(updated))

                    case OperationStatus.BurnPendingRegulation =>
                        val pendingBurn = PendingBurn.parseFrom(operation.data)
                        val updated = pendingBurn.copy(
                            approvals = pendingBurn.approvals.map { approve =>
                                if (approve.regulatorId == request.request.regulatorId) {
                                    approve.copy(
                                        reason = request.request.reason,
                                        signature = request.signature
                                    )
                                } else approve
                            }
                        )

                        for {
                            _ <- Result {
                                pendingBurn.burnRequest.request.tokens.foreach { tokenId =>
                                    store.removeTokenLinkedOperation(tokenId)
                                }
                            }
                            owners <- updated.burnRequest.request.tokens.mapR(tokenId => getTokenOwner(tokenId))
                            issuers <-
                                updated.burnRequest.request.tokens
                                    .mapR(tokenId => TokenId.from(tokenId))
                                    .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                                    .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))
                            _ <-
                                saveOperation(
                                    updated.burnRequest.request.operationId,
                                    OperationStatus.BurnRejectedByRegulation,
                                    updated,
                                    owners.distinct,
                                    updated.approvals.map(_.regulatorId),
                                    issuers
                                )
                        } yield
                            OperationEffect
                                .defaultInstance
                                .withPendingBurns(Collection(updated))

                    case OperationStatus.IssuePendingRegulation =>
                        val pendingIssue = PendingIssue.parseFrom(operation.data)
                        val updated = pendingIssue.copy(
                            approvals = pendingIssue.approvals.map { approve =>
                                if (approve.regulatorId == request.request.regulatorId) {
                                    approve.copy(
                                        reason = request.request.reason,
                                        signature = request.signature
                                    )
                                } else approve
                            }
                        )

                        for {
                            _ <- Result {
                                pendingIssue.request.issue.tokens.foreach { issueToken =>
                                    store.removeTokenLinkedOperation(issueToken.tokenId)
                                }
                            }
                            owners <- Result(updated.request.issue.tokens.map(token => token.owner))
                            issuers <-
                                updated.request.issue.tokens
                                    .map(_.tokenId).mapR(tokenId => TokenId.from(tokenId))
                                    .flatMap(tokenIds => tokenIds.mapR(tokenId => getTokenType(tokenId.typeId)))
                                    .map(tokenTypes => tokenTypes.map(typ => typ.issuerId))
                            _ <-
                                saveOperation(
                                    updated.request.issue.operationId,
                                    OperationStatus.IssueRejectedByRegulation,
                                    updated,
                                    owners.distinct,
                                    updated.approvals.map(_.regulatorId),
                                    issuers
                                )
                        } yield
                            OperationEffect
                                .defaultInstance
                                .withPendingIssues(Collection(updated))

                }
        } yield result

    override def endorseMember(request: SignedEndorsement): Result[Unit] = {
        val endorsement = request.endorsement
        for {
            regulatorId <- getMember(endorsement.regulatorId)
            signatureOk <- cryptography.verifySignature(regulatorId.signingPublic, endorsement.toBytes, request.signature)
            _ <- Util.expect(signatureOk, "Invalid signature")
        } yield
            store.saveEndorsement(endorsement.memberId, request)
    }

    override def endorseMemberPublic(request: SignedPublicEndorsement): Result[Unit] = {
        val endorsement = request.endorsement
        for {
            endorser <- getMember(endorsement.endorserId)
            signatureOk <- cryptography.verifySignature(endorser.signingPublic, endorsement.toBytes, request.signature)
            _ <- Util.expect(signatureOk, "Invalid signature")
        } yield
            store.savePublicEndorsement(endorsement.memberId, request)
    }

    override def revokePublicEndorsement(request: SignedPublicEndorsement): Result[Unit] = {
        val endorsement = request.endorsement
        for {
            endorser <- getMember(endorsement.endorserId)
            signatureOk <- cryptography.verifySignature(endorser.signingPublic, endorsement.toBytes, request.signature)
            _ <- Util.expect(signatureOk, "Invalid signature")
        } yield
            store.revokePublicEndorsement(endorsement.memberId, endorsement.endorserId, endorsement.kindId)
    }

    override def listEndorsements(memberId: String): Result[Array[SignedEndorsement]] = Result {
        store.getEndorsements(memberId)
    }

    override def listPublicEndorsements(memberId: String): Result[Array[SignedPublicEndorsement]] = Result {
        store.getPublicEndorsements(memberId)
    }

    // =====================================================================================================================
    // Public profiles
    // =====================================================================================================================

    override def createProfile(profile: Profile): Result[Profile] = {
        store.getProfile(profile.id) match {
            case Some(_) => Left(s"Public profile address already exists")
            case None =>
                store.saveProfile(profile)
                Right(profile)
        }
    }

    override def updateProfile(profile: Profile): Result[Profile] = {
        store.getProfile(profile.id) match {
            case None => Left(s"Public profile does not exist")
            case Some(storedProfile) =>
                val updatedProfile = storedProfile.copy(
                    description = profile.description,
                    avatar = profile.avatar,
                    background = profile.background
                )
                store.saveProfile(updatedProfile)
                Right(updatedProfile)
        }
    }

    override def getProfile(id: String): Result[Profile] = {
        store.getProfile(id).toRight(s"No such public profile")
    }

    override def listProfiles: Result[Array[Profile]] = Result {
        logger.debug(s"Querying for public profiles")
        store.listProfiles
    }

    override def linkTokensToProfile(profileTokens: ProfileTokens): Result[Unit] = {

        // TODO: check profile & token belong to the wallet
        for {
            profile <- store.getProfile(profileTokens.profileId).toRight("Public profile does not exist")
            prevTokensIds = store.getLinkedToProfileTokenIds(profile.id)
            idsToLink = profileTokens.tokenIds
            _ <- Result.expect(idsToLink.length == idsToLink.distinct.length, "Duplicate tokenIds to link")
        } yield {

            for {
                tokenId <- idsToLink
                storedProfileInfos = store.getTokenProfiles(tokenId)
                storedProfileIds = storedProfileInfos.map(_.profileId)
                if !storedProfileIds.contains(profileTokens.profileId)
            } yield {

                val tokenProfileInfo = TokenProfileInfo(
                    profile.id,
                    profile.name
                )

                store.saveTokenProfiles(
                    TokenProfiles(
                        tokenId,
                        collectionFromArray(tokenProfileInfo +: storedProfileInfos)
                    )
                )
            }

            val res = idsToLink.filter(!prevTokensIds.contains(_))
            val pt = ProfileTokens(
                profileTokens.profileId,
                collectionFromArray(res ++ prevTokensIds)
            )
            store.saveProfileTokens(pt)
        }
    }

    override def getLinkedToProfileTokenIds(profileId: String): Result[Collection[String]] = {
        Result(store.getLinkedToProfileTokenIds(profileId))
    }

    override def unlinkTokensFromProfile(profileTokens: ProfileTokens): Result[Unit] = {
        logger.debug(s"Unlinking tokens from public profile")

        for {
            profile <- store.getProfile(profileTokens.profileId).toRight("Public profile does not exist")
            idsToUnlink = profileTokens.tokenIds
            _ <- Result.expect(idsToUnlink.length == idsToUnlink.distinct.length, "Duplicate tokenIds to unlink")
            linkedTokenIds = store.getLinkedToProfileTokenIds(profile.id)
            idsToSave = linkedTokenIds.filter(!idsToUnlink.contains(_))
        } yield {
            // saving TokenProfiles
            idsToUnlink.foreach { id =>

                val profileInfos = store.getTokenProfiles(id)
                    .filter(_.profileId != profile.id)

                store.saveTokenProfiles(
                    TokenProfiles(
                        id,
                        collectionFromArray(profileInfos)
                    )
                )
            }

            // saving ProfileTokens
            if (idsToSave.nonEmpty) {
                store.saveProfileTokens(
                    ProfileTokens(
                        profile.id,
                        collectionFromArray(idsToSave)
                    )
                )
            } else {
                store.removeProfileTokens(profile.id)
            }
        }
    }

    override def getTokenProfiles(tokenId: String): Result[Collection[TokenProfileInfo]] = {
        Result(store.getTokenProfiles(tokenId))
    }


    // =====================================================================================================================
    // Smart contracts
    // =====================================================================================================================

    override def createSmartContract(signedContract: SignedSmartContract): Result[Unit] = {
        val SignedSmartContract(contract, signature) = signedContract
        for {
            owner <- getMember(contract.issuerId)
            signatureOk <- cryptography.verifySignature(owner.signingPublic, contract.toBytes, signature)
            _ <- Result.expect(signatureOk, s"wrong smart contract signature")
            _ <- getSmartContract(contract.id) match {
                case Left(_) =>
                    SmartContractRegistry.get(contract.templateId) match {
                        case Some(smartContractImpl) =>
                            // TODO perform checks:
                            //  1) DataFeeds and DataFeedTypes
                            //  2) Attributes and MetaInfo
                            val context =
                                SmartContractOperationContextImpl(
                                    contract,
                                    SmartContractState(
                                        contract.id,
                                        new Array(smartContractImpl.templateInformation.stateModel.length),
                                        alive = false
                                    ),
                                    store, txContext
                                )

                            smartContractImpl
                                .initialize(context)
                                .flatMap { initialState =>
                                    Result {
                                        val state = initialState.copy(alive = contract.regulators.isEmpty)

                                        store.saveSmartContract(contract)
                                        store.saveSmartContractState(state)
                                        store.saveSmartContractRegulation(
                                            SmartContractRegulation(
                                                id = contract.id,
                                                approves = contract.regulators.map { regulator =>
                                                    Approve(regulator.regulatorId, approved = false, "")
                                                }
                                            )
                                        )

                                        if (state.alive) {
                                            contract.feeds.foreach(feed =>
                                                store.addDataFeedSubscriber(feed, contract.id)
                                            )
                                        }
                                    }
                                }

                        case None =>
                            Left("No such smart contract template")
                    }

                case Right(_) =>
                    Left("Smart contract address already used")
            }
        } yield ()
    }

    override def getSmartContract(id: String): Result[SmartContract] = {
        logger.debug(s"Querying for smart contract")
        store.getSmartContract(id).toRight(s"No such smart contract id")
    }

    override def listSmartContracts: Result[Array[SmartContract]] = Result {
        logger.debug(s"Querying for smart contracts")
        store.listSmartContracts
    }

    override def listSmartContractAcceptedDeals(id: String): Result[Array[AcceptedDeal]] = Result {
        logger.debug(s"Querying for smart contracts")
        store.getSmartContractAcceptedDeals(id)
    }

    override def getSmartContractState(id: String): Result[SmartContractState] =
        store.getSmartContractState(id).toRight("No such smart contract state")

    override def getSmartContractRegulation(id: String): Result[SmartContractRegulation] =
        store.getSmartContractRegulation(id).toRight("No such smart contract regulation")

    override def approveSmartContract(signedSCRegulationRequest: SignedSCRegulationRequest): Result[SmartContractRegulation] = {
        val scRegulationRequest = signedSCRegulationRequest.request
        for {
            owner <- getMember(scRegulationRequest.regulatorId)
            signatureOk <- cryptography.verifySignature(owner.signingPublic, scRegulationRequest.toBytes, signedSCRegulationRequest.signature)
            _ <- Util.expect(signatureOk, "Invalid signature for approve smart contract")
            smartContractRegulation <- getSmartContractRegulation(scRegulationRequest.contractId)
            smartContract <- getSmartContract(scRegulationRequest.contractId)
            currentContractState <- store.getSmartContractState(scRegulationRequest.contractId).toRight("Current contract state undefined")
        } yield {
            val updated = smartContractRegulation.copy(
                approves = smartContractRegulation.approves.map { approve =>
                    if (approve.regulatorId == scRegulationRequest.regulatorId)
                        approve.copy(
                            approved = true
                        )
                    else approve
                }
            )
            if (updated.approves.forall(_.approved == true)) {
                val updatedContractState = currentContractState.copy(alive = true)
                store.saveSmartContractState(updatedContractState)
                smartContract.feeds.foreach(feed =>
                    store.addDataFeedSubscriber(feed, scRegulationRequest.contractId)
                )
            }
            store.saveSmartContractRegulation(updated)
            updated
        }
    }

    override def rejectSmartContract(signedSCRegulationRequest: SignedSCRegulationRequest): Result[String] = {
        val scRegulationRequest = signedSCRegulationRequest.request
        for {
            owner <- getMember(scRegulationRequest.regulatorId)
            signatureOk <- cryptography.verifySignature(owner.signingPublic, scRegulationRequest.toBytes, signedSCRegulationRequest.signature)
            _ <- Util.expect(signatureOk, "Invalid signature")
            smartContractRegulation <- getSmartContractRegulation(scRegulationRequest.contractId)
        } yield {
            val updated = smartContractRegulation.copy(
                approves = smartContractRegulation.approves.map { approve =>
                    if (approve.regulatorId == scRegulationRequest.regulatorId)
                        approve.copy(
                            reason = scRegulationRequest.reason
                        )
                    else approve
                }
            )
            store.saveSmartContractRegulation(updated)
            scRegulationRequest.contractId
        }
    }

    //    override def registerSmartContractTemplate(request: SmartContractTemplate): Result[SmartContractTemplate] =
    //        store.getSmartContractTemplate(request.address) match {
    //            case Some(_) => Left(s"Smart contract type address already exists")
    //            case None =>
    //                store.saveSmartContractTemplate(request)
    //                Right(request)
    //        }
    //
    override def getSmartContractTemplate(id: String): Result[SmartContractTemplate] =
        SmartContractRegistry
            .get(id)
            .map(_.templateInformation)
            .toRight(s"No such smart contract template")


    override def listSmartContractTemplates: Result[Array[SmartContractTemplate]] = Result {
        logger.debug(s"Querying for smart contract types")
        SmartContractRegistry.values.map(_.templateInformation).toArray
    }

    // =====================================================================================================================
    // Data feeds
    // =====================================================================================================================

    override def registerDataFeed(request: SignedDataFeed): Result[DataFeed] =
        for {
            owner <- getMember(request.dataFeed.owner)
            signatureOk <- cryptography.verifySignature(owner.signingPublic, request.dataFeed.toBytes, request.signature)
            _ <- Result.expect(signatureOk, s"wrong data feed signature")
            _ <- Result {
                store.saveDataFeed(request.dataFeed)
            }
        } yield request.dataFeed


    override def getDataFeed(dataFeedId: String): Result[DataFeed] = {
        logger.debug(s"Querying for data feed")
        store.getDataFeed(dataFeedId).toRight(s"No such data feed address")
    }

    override def listDataFeeds: Result[Array[DataFeed]] = Result {
        logger.debug(s"Querying for data feed list")
        store.listDataFeeds
    }

    override def submitDataFeedValue(request: FeedValueRequest): Result[OperationEffect] =
        for {
            feedIds <- Result(request.feedValues.map(_.id))
            ownerIds <- feedIds.mapR { id =>
                ResultOps.fromOption(store.getDataFeed(id), s"data feed $id not exist").map(_.owner)
            }
            _ <- Result.expect(ownerIds.toSet.size == 1, s"data feed values shall be for feeds belong to same owner")
            owner <- getMember(ownerIds(0))
            signatureOk <- cryptography.verifySignature(
                owner.signingPublic,
                ArrayEncoder[DataFeedValue].encode(collectionToArray(request.feedValues)),
                request.signature
            )
            _ <- Result.expect(signatureOk, s"wrong data feed values signature")
            _ <- Result(logger.debug(s"submitting data feed value: ${request.feedValues.flatMap(_.content).mkString("[", ", ", "]")}"))

            effect <- request.feedValues.mapR { dataFeedValue =>
                store.saveDataFeedValue(dataFeedValue)
                store.listDataFeedSubscribers(dataFeedValue.id).map {
                    _.flatMap { address =>
                        logger.debug(s"DTF subscriber address: $address")
                        val result =
                            for {
                                contract <- getSmartContract(address)
                                smartContractImpl <-
                                    SmartContractRegistry.get(contract.templateId)
                                        .toRight(s"Smart contract $address has invalid template id ${contract.templateId}")

                                contractState <- getSmartContractState(address)
                                context =
                                    SmartContractOperationContextImpl(
                                        contract, contractState, store, txContext
                                    )
                                operationEffects <- smartContractImpl.processDataFeed(context, dataFeedValue)
                                effects <-
                                    applySmartContractResult(
                                        contract, operationEffects.copy(stateUpdate = context.stateUpdate),
                                        Map.empty
                                    )
                            } yield effects

                        result match {
                            case Right(value) => Some(value)
                            case Left(msg) =>
                                logger.warn(s"DTF execution for $address skipped: $msg")
                                None
                        }
                    }
                }
            }.map(_.flatten).map { scEffects =>
                OperationEffect
                    .defaultInstance
                    .withIssued(scEffects.flatMap(_.issued))
                    .withPendingIssues(scEffects.flatMap(_.pendingIssues))
                    .withChange(scEffects.flatMap(_.change))
                    .withPendingDeals(scEffects.flatMap(_.pendingDeals))
                    .withTransferred(scEffects.flatMap(_.transferred))
                    .withBurned(scEffects.flatMap(_.burned))
                    .withPendingBurns(scEffects.flatMap(_.pendingBurns))
                    .withSmartContractUpdates(scEffects.flatMap(_.smartContractUpdates))
            }
        } yield effect


    override def getDataFeedValue(dataFeedId: String): Result[DataFeedValue] = {
        logger.debug(s"Querying  data feed value")
        store.getDataFeedValue(dataFeedId).toRight(s"No such data feed address")
    }

    //    override def getPendingOperation(operationId: String): Result[PendingOperation] = {
    //        logger.debug(s"Querying pending operation")
    //        store.getPendingOperation(operationId).toRight(s"No pending operation with id $operationId")
    //    }

    //    override def getPendingAccept(operationId: String): Result[PendingAccept] = {
    //        logger.debug(s"Querying pending accept")
    //        store.getPendingAccept(operationId).toRight(s"No pending accept with id $operationId")
    //    }
    //
    //
    //    override def listPendingAccepts: Result[Array[PendingAccept]] = {
    //        logger.debug(s"Querying pending accept list")
    //        Result(store.listPendingAccepts)
    //    }

    override def getOperation(operationId: String): Result[OperationHistory] = {
        logger.debug(s"Querying operation data")
        ResultOps.fromOption(store.getOperation(operationId), s"No operation data for id $operationId")
    }

    override def listOperations: Result[Collection[OperationHistory]] = {
        logger.debug(s"Querying operations data list")
        Result(store.listOperations)
    }

    // =====================================================================================================================
    // Utility functions
    // =====================================================================================================================

    private def getRequiredRegulation(tokens: Collection[String], capabilities: Collection[String]): Result[Seq[String]] = {
        // for every type of specified tokenId ...

        tokens.mapR { tokenId =>
            TokenId.from(tokenId)
                .flatMap { id => getTokenType(id.typeId) }
        }
            .map(
                _.flatMap { theType =>
                    val regulation = theType.regulation
                    // ... filter list of regulation by specified capabilities
                    if (
                        regulation.exists { regulator =>
                            capabilities.exists(require => regulator.capabilities.contains(require))
                        }
                    ) regulation.map(_.regulatorId)
                    else Collection.empty[String]
                }
            )
    }.map(_.toSet.toSeq)

    private def findTokenFieldIndex(theType: TokenType, name: String): Result[Int] =
        theType.meta.fields
            .zipWithIndex
            .find(_._1.id == name)
            .map(_._2)
            .toRight(s"Mandatory field $name missing in type ${theType.typeId}")

    private def verifySignatures(
        content: Bytes, address: Signatures, signatures: Collection[Bytes]
    ): Boolean = {
        val keys = address.keys
        val require = address.require
        //        logger.debug(s"VerifySignatures for:\n${address.toProtoString}\n")
        var found = 0
        keys.takeWhile { k =>
            if (
                signatures.exists { s =>
                    cryptography
                        .verifySignature(k, content, s)
                        .contains(true)
                }
            ) found += 1
            found >= require
        }
        //        logger.debug(s"VerifySignatures: found $found from $require")
        found >= require
    }

    private def updateRestrictions(request: TokenChangeRequest, added: Array[TokenAdded]): Result[Unit] = {
        for {
            tokenRestrictions <- store.getTokenRestrictions(request.tokenId)
            _ <- added.mapR { t => store.addTokenRestrictions(t.tokenId, tokenRestrictions) }
            _ <- store.clearRestrictions(request.tokenId)
        } yield ()
    }

    private def deleteToken(tokenId: String, operations: Collection[String], status: Int): Result[Unit] =
        for {
            lastOwner <- store.getTokenOwner(tokenId)
            _ <- store.deleteToken(tokenId)
            _ <- Result(
                store.saveBurntToken(
                    BurntToken(
                        tokenId = tokenId,
                        status = status,
                        lastOwner = lastOwner,
                        operations = operations
                    )
                )
            )
        } yield ()

    override def getCurrentVersionInfo: Result[PlatformVersion] = Result {
        store.getPlatformVersionInfo
    }

    override def getEngineVersion: Result[String] = Result {
        CurrentPlatformVersion
    }
}
