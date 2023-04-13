package ru.sberbank.blockchain.cnft.wallet


import ru.sberbank.blockchain.cnft.common.types.{BigIntOps, Bytes, BytesOps, Collection, CollectionR_Ops, Optional, asBytes}
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{Base58, BigInt, LogAware, Logger, ROps, asByteArray, collectionFromIterable, collectionToArray, isEmptyBytes, isEqualBytes, optionalFromOption}
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.gate.service.{ChainServiceSpec, ChainTxServiceSpec}
import ru.sberbank.blockchain.cnft.model.{MessageRequest, _}
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec._
import ru.sberbank.blockchain.cnft.wallet.walletmodel._
import ru.sberbank.blockchain.cnft.{CurrentPlatformVersion, RelatedDealReferenceEmpty, SupportedMessagesIndex}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Random
import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag
import scala.scalajs.js.annotation.JSExportAll

/**
 * @author Alexey Polubelov
 */
@JSExportAll
class CNFTWalletInternal[R[+_]](
    val id: WalletIdentity,
    val crypto: WalletCrypto[R],
    val chain: ChainServiceSpec[R],
    val chainTx: ChainTxServiceSpec[R],
    val minBlockHeight: Long
)(implicit
    val R: ROps[R],
    val logger: Logger
) extends CNFTWalletSpec[R]
    with WalletCommonOps[R]
    with LogAware {

    private val processing: BlocksProcessing[R] = new BlocksProcessing[R](id, crypto, chain)

    @inline protected implicit def i2c[T: ClassTag](v: scala.collection.Iterable[T]): Collection[T] = collectionFromIterable(v)

    @inline protected implicit def c2a[T: ClassTag](v: Collection[T]): scala.Array[T] = collectionToArray(v)

    // Token types

    override def registerTokenType(
        tokenTypeId: String,
        meta: TokenTypeMeta,
        dna: DNA,
        regulation: Collection[RegulatorCapabilities],
        burnExtraData: Collection[FieldMeta]
    ): R[TxResult[Unit]] = {

        for {
            issuer <- myWalletIdentity
            request =
                TokenTypeRegistrationRequest(
                    operationId = generateTokenInstanceId,
                    timestamp = generateTimestamp,
                    tokenType = Collection(
                        TokenType(tokenTypeId, issuer.id, meta, dna, regulation, burnExtraData)
                    )
                )
            signature <- crypto.identityOperations.createSignature(
                issuer.signingKey,
                asBytes(request.toByteArray)
            )
            signedTokenRegistration = SignedTokenTypeRegistration(
                request,
                signature
            )
            result <- chainTx.registerTokenType(signedTokenRegistration)
                .logDebug(_ => s"[registerTokenType] $tokenTypeId")
        } yield result
    }

    override def listOwnedTokenTypes: R[Collection[TokenType]] =
        for {
            issuerId <- myWalletIdentity.map(_.id)
            tokenTypes <- chain.listTokenTypes
        } yield tokenTypes.filter(_.issuerId == issuerId)


    //
    // Tokens
    //

    override def createAddress: R[Bytes] = createSingleOwnerAddress.map(_.toBytes)

    override def createSingleOwnerAddress: R[TokenOwner] =
        for {
            keyIdentifier <- crypto.addressOperations.requestNewKey()
                .logDebug(key => s"[createAddress] Address created for ${id.id}: $key")

            tokenOwner <- _createSingleOwnerAddress(keyIdentifier)
        } yield tokenOwner

    override def createId: R[String] = R(generateId)

    override def issue(requests: Collection[WalletIssueTokenRequest]): R[TxResult[String]] = {

        for {

            issue <- requests.mapR { request =>
                for {
                    tokenTypeId <- TokenId.from(request.tokenId).map(_.typeId)
                    regulatorsMembers <-
                        chain.getTokenType(tokenTypeId).map { theType =>
                            theType.regulation.map(_.regulatorId) :+ theType.issuerId
                        }

                    extraMembers = Collection(request.to, id.id)
                    membersToEncryptFor <- R {
                        regulatorsMembers ++ extraMembers
                    }

                    issueExtraData = IssueExtraData(request.to).toBytes
                    issueExtraDataEncrypted <- encryptFor(issueExtraData, membersToEncryptFor.toSet)
                } yield IssueToken(
                    tokenId = request.tokenId,
                    owner = request.owner,
                    body = request.body,
                    relatedDealRef = RelatedDealReferenceEmpty,
                    extra = issueExtraDataEncrypted
                )
            }

            issueTokens =
                IssueTokens(
                    operationId = generateId,
                    timestamp = generateTimestamp,
                    issue
                )

            signature <- crypto.identityOperations.createSignature(id.signingKey, issueTokens.toBytes)

            request =
                IssueTokenRequest(
                    issueTokens,
                    Collection(
                        RequestActor(
                            RequestActorType.Member,
                            MemberSignature(id.id, signature).toBytes
                        )
                    )
                )
            result <- chainTx.issueToken(request)
        } yield result.copy(value = issueTokens.operationId)
    }

    override def changeToken(tokenId: String, amounts: Collection[String]): R[TxResult[TokenChangeResponse]] = {
        val request = TokenChangeRequest(
            operationId = generateId,
            timestamp = generateTimestamp,
            tokenId = tokenId,
            amounts = amounts
        )
        for {
            maybeKey <- chain.getTokenOwner(request.tokenId)
                .flatMap { owner =>
                    owner.ownerType match {
                        case OwnerType.Signatures =>
                            for {
                                signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
                                publicKeys <- R(signatures.keys)
                                privateKeys <- crypto.addressOperations.findKeysByPublic(publicKeys)
                            } yield privateKeys.headOption
                        case OwnerType.SmartContractId =>
                            R(None)
                    }
                }
            keyIdentifier <- R.fromOption(maybeKey, s"No key for tokenId ${request.tokenId}")
            signedTokenChangeRequest <- crypto.addressOperations.createSignature(keyIdentifier, request.toBytes)
            result <- chainTx.changeToken(
                SignedTokenChangeRequest(
                    tokenChangeRequest = request,
                    signature = signedTokenChangeRequest
                ))
        } yield result
    }

    override def mergeTokens(tokens: Collection[String]): R[TxResult[TokenMergeResponse]] = {
        val mergeTokensRequest =
            TokenMergeRequest(
                operationId = generateId,
                timestamp = generateTimestamp,
                tokens = tokens
            )
        for {
            signatures <- tokens.mapR { tokenId =>
                for {
                    maybeKey <- chain.getTokenOwner(tokenId)
                        .flatMap { owner =>
                            owner.ownerType match {
                                case OwnerType.Signatures =>
                                    for {
                                        signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
                                        publicKeys <- R(signatures.keys)
                                        privateKeys <- crypto.addressOperations.findKeysByPublic(publicKeys)
                                    } yield privateKeys.headOption
                                case OwnerType.SmartContractId =>
                                    R(None)
                            }
                        }
                    keyIdentifier <- R.fromOption(maybeKey, s"No key for tokenId $tokenId")
                    signature <- crypto.addressOperations.createSignature(keyIdentifier, asBytes(mergeTokensRequest.toByteArray))
                } yield signature
            }
            result <- chainTx.mergeTokens(
                SignedTokenMergeRequest(
                    mergeTokensRequest,
                    signatures
                )
            )
        } yield result
    }


    override def listTokens: R[Collection[WalletToken]] =
        for {
            allTokens <- chain.listTokens
            myTokens <- allTokens.toSeq.filterR(walletToken => amIAnOwner(walletToken.tokenOwner))
        } yield myTokens

    override def listBurntIssuedTokens: R[Collection[WalletToken]] =
        for {
            burnt <- chain.listBurntTokens.map(_.filter(_.status == WalletTokenStatus.Burnt))
            burntIssuedTokens <- burnt.toSeq.filterR { token =>
                for {
                    tokenId <- TokenId.from(token.id)
                    tokenType <- chain.getTokenType(tokenId.typeId)
                } yield tokenType.issuerId == id.id
            }
        } yield burntIssuedTokens

    override def listTokensFiltered(tokenTypeFilter: TokenTypeFilter): R[Collection[WalletToken]] =
        for {
            tokens <- listTokens
            TokenTypeFilter(changeGeneId, negation) = tokenTypeFilter
            filtered <-
                tokens.toSeq
                    .filterR { token =>
                        for {
                            tokenId <- TokenId.from(token.id)
                            tokenType <- chain.getTokenType(tokenId.typeId)
                            geneExists = tokenType.dna.change.exists(_.id == changeGeneId)
                        } yield if (negation) !geneExists else geneExists
                    }

        } yield collectionFromIterable(filtered)

    override def getTokensByTypeId(typeId: String): R[Collection[WalletToken]] =
        for {
            allTokensByTypeId <- chain.getTokensByTypeId(typeId)
            myTokens <- allTokensByTypeId.toSeq.filterR(walletToken => amIAnOwner(walletToken.tokenOwner))
        } yield myTokens

    //
    // Send and receive tokens
    //

    override def acceptToken(transactionId: String): R[TxResult[Unit]] =
        for {
            operation <- chain.getOperation(transactionId)
            dealRequest <-
                if (operation.data.history.last.state == OperationStatus.AcceptPending) {
                    R(DealRequest.parseFrom(asByteArray(operation.data.history.last.data)))
                } else
                    R.Fail(s"Current state of operation $transactionId is not a pending accept")

            deal = dealRequest.deal
            recipientDealSignatures <-
                deal.legs.toSeq.mapR { leg =>
                    val owner = leg.newOwner
                    owner.ownerType match {
                        case OwnerType.Signatures =>
                            for {
                                signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
                                newTokenOwnerKeys = signatures.keys
                                result <- crypto.addressOperations.findKeysByPublic(newTokenOwnerKeys)
                                    .flatMap { keys =>
                                        keys.headOption.map { key =>
                                            for {
                                                signature <- crypto.addressOperations.createSignature(key, deal.toBytes)
                                            } yield
                                                Option(OwnerSignature(leg.tokenId, signature))
                                        }.getOrElse(R(None))
                                    }
                            } yield result

                        case _ => R(None)
                    }
                }.map(_.flatten).map(collectionFromIterable)

            extraMembers <-
                extractDealExtraData(dealRequest.deal)
                    .map(_.members.map(_.id))
                    .map(_.toSet)

            regulatorsMembers <-
                deal.legs.toSeq.mapR { leg =>
                    TokenId.from(leg.tokenId).flatMap { id =>
                        chain.getTokenType(id.typeId).map {
                            theType => theType.regulation.map(_.regulatorId)
                        }
                    }
                }.map(_.flatten.toSet)

            membersToEncryptFor <- R(regulatorsMembers ++ extraMembers)

            memberSignature <- singByMyId(deal.toBytes)

            memberSignatureEncrypted <- encryptFor(memberSignature, membersToEncryptFor)

            result <-
                chainTx.acceptToken(
                    AcceptTokenRequest(
                        transactionId = transactionId,
                        dealSignatures = recipientDealSignatures,
                        memberSignature = memberSignatureEncrypted
                    )
                )
        } yield result

    override def sendTokenToMember(
        memberId: String, address: Bytes, dealId: String,
        tokenIds: Collection[String],
        cptyEndorsements: Collection[SignedEndorsement],
        extraData: Bytes
    ): R[TxResult[String]] =
        for {
            tokenOwner <- R(TokenOwner.parseFrom(asByteArray(address)))
            _ <- R.expect(tokenOwner.ownerType == OwnerType.Signatures, s"Not a valid address: ${address.toB64}")
            operationId = generateId
            tx <- sendToken(
                operationId, generateTimestamp,
                memberId, cptyEndorsements,
                tokenOwner, dealId, tokenIds, extraData
            )
        } yield
            TxResult(
                blockNumber = tx.blockNumber,
                txId = tx.txId,
                operationId
            )

    override def sendTokenToSmartContract(
        id: String, dealId: String,
        tokenIds: Collection[String], extraData: Bytes, requiredDealExtra: Collection[String]
    ): R[TxResult[String]] =
        for {
            contract <- chain.getSmartContract(id) // ensure SC exist
            operationId = generateId
            tx <- sendTokenExtra(
                operationId, generateTimestamp,
                contract.issuerId, Collection.empty, // TODO: shell we copy from smart contract?
                TokenOwner(
                    OwnerType.SmartContractId,
                    asBytes(id.getBytes(StandardCharsets.UTF_8))
                ), dealId, tokenIds, extraData, requiredDealExtra
            )
        } yield
            TxResult(
                blockNumber = tx.blockNumber,
                txId = tx.txId,
                operationId
            )


    private[wallet] def sendToken(
        operationId: String, timestamp: String,
        toMemberId: String, cptyEndorsements: Collection[SignedEndorsement],
        tokenOwner: TokenOwner, dealId: String,
        tokens: Collection[String],
        extraData: Bytes
    ): R[TxResult[Unit]] =
        sendTokenExtra(operationId, timestamp, toMemberId, cptyEndorsements, tokenOwner, dealId, tokens, extraData, Collection.empty)


    private[wallet] def sendTokenExtra(
        operationId: String, timestamp: String,
        toMemberId: String, cptyEndorsements: Collection[SignedEndorsement],
        tokenOwner: TokenOwner, dealId: String,
        tokens: Collection[String],
        extraData: Bytes,
        requiredDealExtra: Collection[String]
    ): R[TxResult[Unit]] =
        for {
            legsAndKeys <-
                tokens.mapR { tokenId =>
                    for {
                        currentOwner <- chain.getTokenOwner(tokenId)
                        maybeKey <-
                            currentOwner.ownerType match {
                                case OwnerType.Signatures =>
                                    for {
                                        signatures <- R(Signatures.parseFrom(asByteArray(currentOwner.address)))
                                        publicKeys <- R(signatures.keys)
                                        privateKeys <- crypto.addressOperations.findKeysByPublic(publicKeys)
                                    } yield privateKeys.headOption

                                case _ => R(None)
                            }

                        keyIdentifier <- R.fromOption(maybeKey, s"No key for tokenId $tokenId")
                    } yield
                        (
                            DealLeg(
                                tokenId = tokenId,
                                newOwner = tokenOwner,
                                previousOwner = currentOwner,
                                relatedDealRef = RelatedDealReferenceEmpty,
                                requiredDealExtra
                            ),
                            tokenId -> keyIdentifier,
                        )
                }
            (legs, keys) = legsAndKeys.unzip

            myId <- myWalletIdentity
            endorsements <- listEndorsements

            regulatorsMembers <-
                tokens.mapR { tokenId =>
                    TokenId.from(tokenId).flatMap { id =>
                        chain.getTokenType(id.typeId).map { theType =>
                            theType.regulation.map(_.regulatorId) // :+ theType.issuerId
                        }
                    }
                }.map(_.flatten.toSet)

            extraMembers = Collection(myId.id, toMemberId)
            membersToEncryptFor <- R(regulatorsMembers ++ extraMembers)

            dealExtra =
                DealExtraData(
                    members = Collection(
                        DealMember(myId.id, endorsements),
                        DealMember(toMemberId, cptyEndorsements) //TODO get counterparty endorsement certificates
                    ),
                    legs = tokens.indices map { _ => LegInfo(0, 1) },
                    extraData
                ).toBytes

            dealExtraEncrypted <- encryptFor(dealExtra, membersToEncryptFor)

            deal =
                Deal(
                    operationId,
                    timestamp,
                    dealId = dealId,
                    legs = legs,
                    extra = dealExtraEncrypted
                )

            signatures <-
                keys.mapR { case (tokenId, keyId) =>
                    crypto.addressOperations.createSignature(keyId, deal.toBytes).map { signature =>
                        OwnerSignature(tokenId, signature)
                    }
                }

            memberSignature <- singByMyId(deal.toBytes)
            memberSignatureEncrypted <- encryptFor(memberSignature, membersToEncryptFor)

            dealRequest =
                DealRequest(
                    deal = deal,
                    ownerSignatures = signatures,
                    recipientSignatures = Collection.empty,
                    actors =
                        Collection(
                            RequestActor(
                                RequestActorType.Member,
                                memberSignatureEncrypted
                            )
                        )
                )
            result <- chainTx.makeDeal(dealRequest)
        } yield TxResult(blockNumber = result.blockNumber, txId = result.txId, value = ())

    //
    // Burn token
    //
    override def burnTokens(tokens: Collection[String], extra: Bytes, extraFields: Collection[String]): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            endorsements <- listEndorsements

            regulatorsMembers <-
                tokens.mapR { tokenId =>
                    TokenId.from(tokenId).flatMap { id =>
                        chain.getTokenType(id.typeId).map { theType =>
                            theType.regulation.map(_.regulatorId) :+ theType.issuerId
                        }
                    }
                }.map(_.flatten.toSet)

            membersToEncryptFor <- R(regulatorsMembers ++ Set(myId.id))

            burnExtraData = BurnExtraData(endorsements, extra, extraFields, myId.id).toBytes
            burnExtraDataEncrypted <- encryptFor(burnExtraData, membersToEncryptFor)

            burnRequest =
                BurnRequest(
                    operationId = generateId,
                    timestamp = generateTimestamp,
                    tokens = tokens,
                    extra = burnExtraDataEncrypted
                )
            requestBytes = burnRequest.toBytes
            signatures <-
                tokens.mapR { tokenId =>
                    for {
                        maybeKey <- chain.getTokenOwner(tokenId)
                            .flatMap { owner =>
                                owner.ownerType match {
                                    case OwnerType.Signatures =>
                                        for {
                                            signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
                                            publicKeys <- R(signatures.keys)
                                            privateKeys <- crypto.addressOperations.findKeysByPublic(publicKeys)
                                        } yield privateKeys.headOption

                                    case _ =>
                                        R(None)
                                }
                            }
                        keyIdentifier <- R.fromOption(maybeKey, s"No key for tokenId $tokenId")
                        signature <-
                            crypto.addressOperations.createSignature(keyIdentifier, requestBytes)

                    } yield OwnerSignature(tokenId, signature)
                }

            memberSignature <-
                crypto.identityOperations.createSignature(myId.signingKey, requestBytes)

            mSignature =
                MemberSignature(
                    myId.id,
                    memberSignature
                ).toBytes
            mSignatureEncrypted <- encryptFor(mSignature, membersToEncryptFor)

            response <-
                chainTx.burnTokens(
                    SignedBurnRequest(
                        burnRequest,
                        generateTimestamp, // note: the system will override this one
                        signatures,
                        mSignatureEncrypted
                    )
                )

        } yield response.copy(value = ())

    // Regulation

    override def addNotice(transactionId: String, notice: String, membersIds: Collection[String]): R[TxResult[Unit]] =
        for {
            request <- getSignedTXRegulationNotification(transactionId, notice, membersIds)
            result <- chainTx.addNotice(request)
        } yield result

    override def approveTransaction(transactionId: String): R[TxResult[Unit]] =
        for {
            request <- getSignedTXRegulationRequest(transactionId, "", Collection.empty[String])
            result <- chainTx.approveTransaction(request)
        } yield result

    override def rejectTransaction(transactionId: String, reason: String, membersIds: Collection[String]): R[TxResult[Unit]] =
        for {
            request <- getSignedTXRegulationRequest(transactionId, reason, membersIds)
            result <- chainTx.rejectTransaction(request)
        } yield result


    def extractMembers(pending: OperationState): R[Collection[String]] = {
        pending.state match {
            case OperationStatus.DealPendingRegulation =>
                for {
                    pendingDeal <- R(PendingDeal.parseFrom(asByteArray(pending.data)))
                    memberIds <-
                        if (isEmptyBytes(pendingDeal.deal.deal.extra)) {
                            if (pendingDeal.deal.actors.length == 1 &&
                                pendingDeal.deal.actors(0).theType == RequestActorType.SmartContract) {
                                chain.getSmartContract(pendingDeal.deal.actors(0).value.toUTF8)
                                    .map(v => Collection(v.issuerId))
                            } else R(Collection.empty[String])
                        }
                        else
                            for {
                                extra <-
                                    crypto.encryptionOperations
                                        .decrypt(pendingDeal.deal.deal.extra, id.encryptionKey)

                                extraData <- R(DealExtraData.parseFrom(asByteArray(extra)))
                            } yield extraData.members.map(_.id)
                } yield memberIds

            case OperationStatus.IssuePendingRegulation =>
                for {
                    pendingIssue <- R(PendingIssue.parseFrom(asByteArray(pending.data)))
                    issue = pendingIssue.request.issue
                    ids <-
                        issue.tokens.toSeq.mapR { token =>
                            TokenId.from(token.tokenId).map(_.typeId).flatMap { tokenTypeId =>
                                chain.getTokenType(tokenTypeId).flatMap { tokenType =>
                                    val to =
                                        if (isEmptyBytes(token.extra)) {
                                            if (pendingIssue.request.actors.length == 1 &&
                                                pendingIssue.request.actors(0).theType == RequestActorType.SmartContract &&
                                                token.relatedDealRef != RelatedDealReferenceEmpty) {
                                                for {
                                                    deals <- chain.listSmartContractAcceptedDeals(pendingIssue.request.actors(0).value.toUTF8)
                                                    deal <- R(deals(token.relatedDealRef.dealIndex))
                                                    extra <- extractDealExtraData(deal.deal.deal)
                                                    from <- R(extra.legs(token.relatedDealRef.tokenLegIndex).from)
                                                } yield Option(extra.members(from).id)
                                            }
                                            else R(None)
                                        }
                                        else
                                            for {
                                                extraData <- crypto.encryptionOperations
                                                    .decrypt(token.extra, id.encryptionKey)
                                                issueExtra <- R(IssueExtraData.parseFrom(asByteArray(extraData)))
                                            } yield Option(issueExtra.memberID)
                                    to.map(_ ++ Option(tokenType.issuerId))
                                }
                            }
                        }.map(_.flatten)
                } yield collectionFromIterable(ids) //   Collection(theType.issuerId) ++ issueToIDs
            case OperationStatus.BurnPendingRegulation =>
                for {
                    pendingBurn <- R(PendingBurn.parseFrom(asByteArray(pending.data)))
                    ids <- if (asByteArray(pendingBurn.burnRequest.memberSignature).nonEmpty) {
                        crypto.encryptionOperations.decrypt(pendingBurn.burnRequest.memberSignature, id.encryptionKey)
                            .map { signature =>
                                Collection(
                                    MemberSignature.parseFrom(asByteArray(signature)).memberId
                                )
                            }
                    } else R(Collection.empty[String])
                } yield ids

            case _ =>
                R.Fail("unsupported operation type.")
        }
    }

    def extractedMembersKeys(membersIds: Collection[String]): R[Collection[Bytes]] = {
        membersIds.mapR { memberId =>
            chain.getMember(memberId).map(_.encryptionPublic)
        }
    }

    private def getSignedTXRegulationRequest(transactionId: String, reason: String, membersIds: Collection[String]): R[SignedTXRegulationRequest] =
        for {
            myId <- myWalletIdentity
            operation <- chain.getOperation(transactionId)
            state <- R.fromOption(operation.data.history.lastOption.map(_.state), s"Invalid operation: $transactionId")
            pending <-
                if (
                    state == OperationStatus.DealPendingRegulation ||
                        state == OperationStatus.BurnPendingRegulation ||
                        state == OperationStatus.IssuePendingRegulation
                )
                    R(operation.data.history.last)
                else
                    R.Fail(s"Operation with id $transactionId is not in pending status")
            extraMembersToEncryptFor <- extractMembers(pending)
            reasonEncrypted <-
                if (reason.isEmpty) R(reason)
                else encryptText(reason, membersIds ++ extraMembersToEncryptFor :+ myId.id)
            request = TXRegulationRequest(
                operationId = generateId,
                timestamp = generateTimestamp,
                transactionId = transactionId,
                regulatorId = myId.id,
                reason = reasonEncrypted
            )
            signature <- crypto.identityOperations.createSignature(myId.signingKey, request.toBytes)
        } yield SignedTXRegulationRequest(
            request = request,
            signature = signature
        )

    private def getSignedTXRegulationNotification(transactionId: String, notice: String, membersIds: Collection[String]) =
        for {
            myId <- myWalletIdentity
            operation <- chain.getOperation(transactionId)
            state <- R.fromOption(operation.data.history.lastOption.map(_.state), s"Invalid operation: $transactionId")
            pending <-
                if (
                    state == OperationStatus.DealPendingRegulation ||
                        state == OperationStatus.BurnPendingRegulation ||
                        state == OperationStatus.IssuePendingRegulation
                )
                    R(operation.data.history.last)
                else
                    R.Fail(s"Operation with id $transactionId is not in pending status")
            extraMembersToEncryptFor <- extractMembers(pending)
            messageEncrypted <-
                if (notice.isEmpty) R(notice)
                else encryptText(notice, membersIds ++ extraMembersToEncryptFor :+ myId.id)
            request = TXRegulationNotification(
                operationId = generateId,
                timestamp = generateTimestamp,
                transactionId = transactionId,
                regulatorId = myId.id,
                notice = messageEncrypted
            )
            signature <- crypto.identityOperations.createSignature(myId.signingKey, request.toBytes)
        } yield SignedTXRegulationNotification(
            request = request,
            signature = signature
        )


    private[wallet] def publishPlatformMessage[T <: scalapb.GeneratedMessage](to: String, message: T): R[TxResult[Unit]] =
        for {
            messageType <- R.fromOption(SupportedMessagesIndex.get(message.companion), s"unsupported message type")
            tx <- publishGenericMessage(to,
                GenericMessage(
                    systemId = 0,
                    messageType = messageType,
                    data = message.toBytes
                )
            )
        } yield tx


    private[wallet] def publishGenericMessage(to: String, genericMessage: GenericMessage): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            recipient <- chain.getMember(to)
            from <- chain.getMember(myId.id)
            messageBytes = genericMessage.toBytes
            messageSignature <- crypto.identityOperations.createSignature(myId.signingKey, messageBytes)
            signedPayload = SignedPayload(
                messageBytes,
                messageSignature
            )
            encryptedMessage <- crypto.encryptionOperations.encrypt(signedPayload.toBytes, Collection(recipient.encryptionPublic, from.encryptionPublic))
            tx <- chainTx.publishMessages(
                Collection(
                    Message(
                        from = from.id,
                        to = recipient.id,
                        payload = encryptedMessage,
                    )
                )
            )
        } yield tx

    override def getIdentity: R[String] = R(id.id)

    override def getWalletInformation: R[WalletIdentity] = R(id)

    //
    //
    //

    // Endorsement Info

    //    override def getEndorsementInfo(blockNumber: Long): R[Array[EndorserInfo]] = {
    //        chain.getEndorsementInfo(blockNumber)
    //    }


    //    override def getProof(tokenId: String): R[Collection[BlockInfo]] = chain.getProof(tokenId)

    //    override def getTransaction(blockNumber: Long, txId: String): R[BlockEvents] = chain.getTransaction(blockNumber, txId)

    //
    // Utility methods
    //

    private[wallet] def _createSingleOwnerAddress(keyIdentifier: KeyIdentifier): R[TokenOwner] =
        for {
            publicAsBytes <- crypto.addressOperations.publicKey(keyIdentifier)
            owner = TokenOwner(
                ownerType = OwnerType.Signatures,
                address = Signatures(
                    require = 1,
                    keys = Collection(publicAsBytes)

                ).toBytes
            )
        } yield owner

    //
    //    private def tokenOwner(tokenId: String): R[TokenOwner] = for {
    //        possibleCurrentTokenOwner <- store.getTokenOwner(tokenId)
    //        currentTokenOwner <- R.fromOption(possibleCurrentTokenOwner, "couldn't find owner for token id")
    //    } yield currentTokenOwner


    private[wallet] def findMember(from: Bytes): R[Optional[MemberInformation]] =
        chain.listMembers.map { members =>
            optionalFromOption(
                members.find { member =>
                    isEqualBytes(member.encryptionPublic, from)
                }
            )
        }

    private[wallet] def expectOne[T](values: Collection[T], msg: String): R[T] =
        R.fromOption(values.headOption, msg)

    private[wallet] def expectOne[T](values: TxResult[Collection[T]], msg: String): R[TxResult[T]] =
        R.fromOption(values.value.headOption.map(TxResult(values.blockNumber, values.txId, _)), msg)

    private[wallet] def collectionToString[T](v: Collection[T]): String = v.mkString("[", ", ", "]")

    private[wallet] def myWalletIdentity: R[WalletIdentity] = R(id)

    //    //
    //    // Offers
    //    //
    //    private val offers = new Offers[R](this)
    //
    //    override def putOffer(supply: TokenDescription, demand: TokenDescription): R[TxResult[Offer]] =
    //        offers.putOffer(supply, demand)
    //
    //    override def listTokenSupplyCandidates(offerId: String): R[Collection[String]] =
    //        offers.listTokenSupplyCandidates(offerId)
    //
    //    override def listTokenDemandCandidates(offerId: String): R[Collection[String]] =
    //        offers.listTokenDemandCandidates(offerId)
    //
    //    override def applyForOffer(offerId: String, dealId: String, signedToken: String): R[Unit] =
    //        offers.applyForOffer(offerId, dealId, signedToken)
    //
    //    override def approveOffer(offerId: String, dealId: String, signedToken: String): R[Unit] =
    //        offers.approveOffer(offerId, dealId, signedToken)
    //
    //    override def finalizeOffer(offerId: String, dealId: String): R[Unit] =
    //        offers.finalizeOffer(offerId, dealId)
    //
    //    override def closeOffer(offerId: String): R[TxResult[Unit]] =
    //        offers.closeOffer(offerId)
    //
    //    override def listOffers: R[Collection[WalletOffer]] =
    //        offers.listOffers
    //
    //    override def getOffer(offerId: String): R[Optional[WalletOffer]] =
    //        offers.getOffer(offerId)

    //
    // Messages
    //

    private val messaging = new Messages[R](this)

    override def proposeToken(to: String, tokenType: String, tokenContent: Collection[String], extraData: Bytes): R[String] =
        messaging.proposeToken(to, tokenType, tokenContent, extraData)

    override def acceptTransferProposal(operationId: String, extraData: Bytes): R[Bytes] =
        messaging.acceptTransferProposal(operationId, extraData)

    override def requestToken(from: String, address: Bytes, tokenType: String, tokenContent: Collection[String], extraData: Bytes): R[Bytes] =
        messaging.requestToken(from, address, tokenType, tokenContent, extraData)

    override def requestToken(from: String, tokenType: String, tokenContent: Collection[String], extraData: Bytes): R[Bytes] =
        messaging.requestToken(from, tokenType, tokenContent, extraData)

    //    override def requestIssue(tokenType: Bytes, content: Collection[String], extraData: Bytes): R[String] =
    //        messaging.requestIssue(tokenType, content, extraData)

    override def acceptTokenRequest(operationId: String, tokenIds: Collection[String], extraData: Bytes): R[TxResult[Unit]] =
        messaging.acceptTokenRequest(operationId, tokenIds, extraData)

    override def sendGenericMessage(to: String, systemId: Int, messageType: Int, messageData: Bytes): R[TxResult[Unit]] =
        messaging.sendGenericMessage(to, systemId, messageType, messageData)

    override def listMessages: R[Collection[MessageRequest]] = messaging.listMessages


    //
    // Data feeds
    //

    private val dataFeeds = new DataFeeds[R](this)

    override def registerDataFeed(description: Collection[DescriptionField], fields: Collection[FieldMeta]): R[TxResult[DataFeed]] =
        dataFeeds.registerDataFeed(description, fields)

    override def submitDataFeedValue(values: Collection[DataFeedValue]): R[TxResult[Unit]] =
        dataFeeds.submitDataFeedValue(values)

    //
    // Members
    //

    private val membersRegistry = new MemberRegistry[R](this)

    override def registerMember(member: MemberInformation): R[TxResult[String]] =
        membersRegistry.registerMember(member)

    override def updateMember(update: MemberInformation): R[TxResult[Unit]] =
        membersRegistry.updateMemberInfo(update)

    override def listEndorsements: R[Collection[SignedEndorsement]] =
        membersRegistry.listEndorsements

    override def requestEndorsement(regulatorId: String, data: Bytes): R[TxResult[Unit]] =
        membersRegistry.requestEndorsement(regulatorId, data)

    override def endorseMember(memberId: String, certificate: Bytes): R[TxResult[Unit]] =
        membersRegistry.endorseMember(memberId, certificate)

    override def endorseMemberPublic(memberId: String, kindId: String, data: Bytes): R[TxResult[Unit]] =
        membersRegistry.endorseMemberPublic(memberId, kindId, data)

    override def revokePublicEndorsement(memberId: KeyIdentifier, kindId: KeyIdentifier): R[TxResult[Unit]] =
        membersRegistry.revokePublicEndorsement(memberId, kindId)

    override def rejectEndorsement(memberId: String, reason: String): R[TxResult[Unit]] =
        membersRegistry.rejectEndorsement(memberId, reason)

    //
    // Regulator operation
    //

    private val regulation = new RegulationOps[R](this)

    override def freezeToken(requests: Collection[FreezeInfo]): R[TxResult[Unit]] =
        regulation.freezeToken(requests)

    override def unfreezeToken(requests: Collection[FreezeInfo]): R[TxResult[Unit]] =
        regulation.unfreezeToken(requests)

    override def regulatoryBurnToken(request: RegulatoryBurnTokenRequest): R[TxResult[Unit]] =
        regulation.regulatoryBurnToken(request)

    override def regulatoryTransfer(memberId: String, dealId: String, tokenIds: Collection[String], to: Bytes): R[TxResult[Unit]] =
        regulation.regulatoryTransfer(memberId, dealId, tokenIds, to)

    override def regulatoryChangeToken(tokenId: String, amounts: Collection[String]): R[TxResult[TokenChangeResponse]] =
        regulation.regulatoryChangeToken(tokenId: String, amounts: Collection[String])

    // Profiles

    private val profiles = new Profiles(this)

    override def createProfile(profile: CreateProfileInfo): R[TxResult[Profile]] = profiles.createProfile(profile)

    override def updateProfile(profile: Profile): R[TxResult[Profile]] = profiles.updateProfile(profile)

    override def listProfiles: R[Collection[Profile]] = profiles.listProfiles

    override def linkTokensToProfile(profileId: String, tokenIds: Collection[String]): R[TxResult[Unit]] = {
        profiles.linkTokensToProfile(ProfileTokens(profileId, tokenIds))
    }

    override def unlinkTokensFromProfile(profileId: String, tokenIds: Collection[String]): R[TxResult[Unit]] = {
        profiles.unlinkTokensFromProfile(ProfileTokens(profileId, tokenIds))
    }

    // TokenId

    def createTokenId(tokenTypeId: String): R[String] = TokenId.encode(tokenTypeId, generateTokenInstanceId)

    //
    // Smart contracts
    //

    private val smartContracts = new SmartContracts(this)

    //    override def registerSmartContractTemplate(
    //        feeds: Collection[FeedType], description: Collection[DescriptionField],
    //        attributes: Collection[FieldMeta], stateModel: Collection[FieldMeta],
    //        classImplementation: String): R[TxResult[SmartContractTemplate]] =
    //        smartContracts.registerSmartContractTemplate(feeds, description, attributes, stateModel, classImplementation)

    override def createSmartContract(
        id: String, templateId: String, attributes: Collection[String],
        dataFeeds: Collection[String], regulators: Collection[RegulatorCapabilities]
    ): R[TxResult[SmartContract]] =
        smartContracts.createSmartContract(id, templateId, attributes, dataFeeds, regulators)

    override def getSmartContractRegulation(id: String): R[SmartContractRegulation] =
        smartContracts.getSmartContractRegulation(id)

    override def approveSmartContract(id: String): R[TxResult[Unit]] =
        smartContracts.approveSmartContract(id)

    override def rejectSmartContract(id: String, reason: String): R[TxResult[Unit]] =
        smartContracts.rejectSmartContract(id, reason)

    override def extractMemberSignature(signature: Bytes): R[MemberSignature] =
        for {
            decrypted <- crypto.encryptionOperations.decrypt(signature, id.encryptionKey)
            memberSignature <- R {
                MemberSignature.parseFrom(asByteArray(decrypted))
            }
        } yield memberSignature

    override def extractDealExtraData(deal: Deal): R[DealExtraData] =
        for {
            extraBytes <- crypto.encryptionOperations.decrypt(deal.extra, id.encryptionKey)
            data <- R(DealExtraData.parseFrom(asByteArray(extraBytes)))
        } yield data

    override def extractBurnExtraData(burn: BurnRequest): R[BurnExtraData] =
        for {
            extraBytes <- crypto.encryptionOperations.decrypt(burn.extra, id.encryptionKey)
            data <- R(BurnExtraData.parseFrom(asByteArray(extraBytes)))
        } yield data

    override def extractIssueTokenExtraData(issue: IssueToken): R[IssueExtraData] =
        for {
            extraBytes <- crypto.encryptionOperations.decrypt(issue.extra, id.encryptionKey)
            data <- R(IssueExtraData.parseFrom(asByteArray(extraBytes)))
        } yield data

    override def extractGenericMessage(request: MessageRequest): R[GenericMessage] =
        for {
            decryptedPayload <- crypto.encryptionOperations.decrypt(request.message.payload, id.encryptionKey)
            signedPayload <- R {
                SignedPayload.parseFrom(asByteArray(decryptedPayload))
            }
            genericMessage <- R {
                GenericMessage.parseFrom(asByteArray(signedPayload.data))
            }
        } yield genericMessage

    private[wallet] val operations = new Operations(this)


    override def listOperations: R[Collection[Operation]] = operations.listOperations

    override def getOperation(operationId: String): R[Operation] = operations.getOperation(operationId)
        .flatMap(R.fromOption(_,s"can not get operation $operationId"))

    override def getOperationDetails(state: OperationState): R[OperationData] = operations.getOperationDetails(state)

    // TODO use random from crypto
    private val rnd = new Random()

    private[wallet] def generateId: String = {
        val bytes = new Array[Byte](32)
        rnd.nextBytes(bytes)
        Base58.encode(bytes)
    }

    private[wallet] def generateTokenInstanceId: String = {
        val bytes = new Array[Byte](36)
        rnd.nextBytes(bytes)
        Base58.encode(bytes)
    }

    private[wallet] def generateTimestamp: String = Instant.now().toString

    private[wallet] def encryptFor(data: Bytes, members: Set[String]): R[Bytes] = {
        for {
            encryptionKeys <- members.mapR { memberId =>
                chain.getMember(memberId).map(_.encryptionPublic)
            }
            res <- crypto.encryptionOperations.encrypt(data, collectionFromIterable(encryptionKeys))
        } yield res
    }

    private[wallet] def singByMyId(data: Bytes): R[Bytes] =
        for {
            wallet <- myWalletIdentity
            signature <- crypto.identityOperations.createSignature(wallet.signingKey, data)
        } yield MemberSignature(wallet.id, signature).toBytes

    override def isRegistered: R[Boolean] =
        chain.getMember(id.id)
            .map(_ => true)
            .recover(_ => R(false))

    override def events(block: BigInt, skipSignaturesCheck: Boolean): R[WalletEvents] = {
        if (block.toLong <= minBlockHeight) {
            logger.warn(s"EVENTS PROCESSING SKIPPED: the requested block $block is earlier than current version minimal height  ${minBlockHeight}")
            WalletEvents.empty
        }
        val blockNumber = block.toLong

        chain.getTransactions(blockNumber).flatMap { b =>
            processing.extractEvents(blockNumber, b, skipSignaturesCheck)
        }
    }

    override def listIssuedTokens: R[Collection[WalletToken]] =
        for {
            allTypes <- chain.listTokenTypes
            issuedByMeTypes <- R(allTypes.filter(t => t.issuerId == id.id))
            tokens <- issuedByMeTypes.mapR(t => getTokensByTypeId(t.typeId)).map(_.flatten)
        } yield tokens

    override def listBurntTokens: R[Collection[WalletToken]] =
        for {
            allTokens <- chain.listBurntTokens
            burntTokens = allTokens.filter(walletToken => walletToken.status == WalletTokenStatus.Burnt)
            myBurntTokens <- burntTokens.toSeq.filterR(bt => amIAnOwner(bt.tokenOwner))
        } yield myBurntTokens

    override def walletVersion: R[KeyIdentifier] = R {
        CurrentPlatformVersion
    }


}
