//package ru.sberbank.blockchain.cnft.gate.service
//
//import com.google.protobuf.ByteString
//import org.enterprisedlt.fabric.client.FabricChannel
//import org.hyperledger.fabric.protos.common.Common._
//import org.hyperledger.fabric.protos.common.Configtx.Config
//import org.hyperledger.fabric.protos.common.Policies.{ImplicitMetaPolicy, SignaturePolicyEnvelope}
//import org.hyperledger.fabric.protos.common.{Common, Configtx}
//import org.hyperledger.fabric.protos.ledger.rwset.Rwset.TxReadWriteSet
//import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset.KVRWSet
//import org.hyperledger.fabric.protos.msp
//import org.hyperledger.fabric.protos.msp.Identities.SerializedIdentity
//import org.hyperledger.fabric.protos.peer.Chaincode.{ChaincodeInput, ChaincodeInvocationSpec}
//import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage.ChaincodeEvent
//import org.hyperledger.fabric.protos.peer.ProposalPackage.{ChaincodeAction, ChaincodeHeaderExtension, ChaincodeProposalPayload}
//import org.hyperledger.fabric.protos.peer.ProposalResponsePackage.ProposalResponsePayload
//import org.hyperledger.fabric.protos.peer.TransactionPackage.{ChaincodeActionPayload, Transaction}
//import org.hyperledger.fabric.protos.peer.lifecycle.Lifecycle.QueryChaincodeDefinitionResult
//import org.hyperledger.fabric.protos.peer.{Policy, TransactionPackage}
//import ru.sberbank.blockchain.cnft.commons.{Lazy, _}
//import ru.sberbank.blockchain.cnft.gate.CNFTOperation._
//import ru.sberbank.blockchain.cnft.gate.model._
//import ru.sberbank.blockchain.cnft.gate.{CNFTOperation, model}
//import ru.sberbank.blockchain.cnft.model.{DealRequest, IssueTokenRequest, IssueTokenResponse, MemberInformation, Message, Offer, PutOfferRequest, RegisterTokenType, RegulatorBurnRequest, ReserveTokenIDsRequest, ReservedId, SignedBurnRequest, TokenFreezeRequest, TokenType}
//import ujson.Value
//
//import java.nio.charset.StandardCharsets
//import java.util.{Base64, Date}
//import scala.annotation.tailrec
//import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
//
//trait CNFTProofService extends CNFTProofSpec[Result] with LoggingSupport with CNFTBlocksService {
//    def channel: FabricChannel
//
//    private def getBlockInfo(number: Long): Result[BlockInfo] =
//        for {
//            headersCNFT <- getAllHeadersCNFTChannel(number)
//            policy <- getChaincodePolicy
//            block <- getParseBlockNumber(number).toRight(s"Unable to fetch block: $number")
//        } yield
//            BlockInfo(
//                blocksOrderer =
//                    BlocksOrderer(
//                        block
//                    ),
//                headersCNFT = headersCNFT,
//                policy = policy
//            )
//
//    @tailrec
//    private def findConfigGroup(referenceList: Array[String], mapGroup: Configtx.ConfigGroup): Configtx.ConfigGroup = {
//        if (referenceList.isEmpty || mapGroup.getGroupsMap.isEmpty) {
//            mapGroup
//        }
//        else if (mapGroup.getGroupsMap.containsKey(referenceList.head) && referenceList.length > 1) {
//            findConfigGroup(referenceList.tail, mapGroup.getGroupsMap.get(referenceList.head))
//        } else
//            findConfigGroup(referenceList.tail, mapGroup)
//    }
//
//    private def getPolicyFromConfig(configPolicyReference: String): Result[ImplicitMetaPolicyAndSignature] = {
//        val reference = configPolicyReference.split("/")
//        val channelGroup = Config.parseFrom(channel.getChannelConfigurationBytes).getChannelGroup
//        val configGroup = findConfigGroup(reference, channelGroup)
//        val configPolicy = configGroup.getPoliciesMap.get(reference.last).getPolicy
//        configPolicy.getType match {
//            case 1 =>
//                val policy = SignaturePolicyEnvelope.parseFrom(configPolicy.getValue)
//                val rules = Rules(
//                    policy.getRule.getNOutOf.getRulesList
//                        .map { rule =>
//                            Rule(rule.getSignedBy)
//                        }.toArray
//                )
//                val policyJson = PolicyJson(
//                    rules = rules,
//                    n = policy.getRule.getNOutOf.getN
//                )
//                Result(
//                    ImplicitMetaPolicyAndSignature(
//                        implicitMetaPolicy = null,
//                        signaturePolicy = policyJson
//                    )
//                )
//            case 3 =>
//                val implicitMetaPolicy = ImplicitMetaPolicy.parseFrom(configPolicy.getValue)
//                val meta = MetaPolicy(
//                    implicitMetaPolicy.getSubPolicy,
//                    implicitMetaPolicy.getRule.toString
//                )
//                Result(
//                    ImplicitMetaPolicyAndSignature(
//                        implicitMetaPolicy = meta,
//                        signaturePolicy = null
//                    )
//                )
//        }
//    }
//
//    private def getChaincodePolicy: Result[ImplicitMetaPolicyAndSignature] = {
//        val cnftCC = channel.getChainCode("cnft")
//        for {
//            res <- cnftCC.rawQueryDefinitions()
//            endorserInfo = QueryChaincodeDefinitionResult.parseFrom(res)
//            policy = Policy.ApplicationPolicy.parseFrom(endorserInfo.getValidationParameter)
//            implicitMetaPolicyAndSignature <-
//                if (!policy.hasSignaturePolicy) {
//                    getPolicyFromConfig(policy.getChannelConfigPolicyReference)
//                } else {
//                    Result.Ok(
//                        ImplicitMetaPolicyAndSignature(
//                            implicitMetaPolicy = null,
//                            signaturePolicy = PolicyJson(
//                                rules = Rules(
//                                    policy.getSignaturePolicy.getRule.getNOutOf.getRulesList
//                                        .map { rule =>
//                                            Rule(rule.getSignedBy)
//                                        }.toArray
//                                ),
//                                n = policy.getSignaturePolicy.getRule.getNOutOf.getN
//                            )
//                        )
//                    )
//                }
//        } yield implicitMetaPolicyAndSignature
//    }
//
//
//    private def getAllHeadersCNFTChannel(numberBlock: Long): Result[HeadersCNFT] = {
//        ResultOps.foldFailFast(
//            0L.to(numberBlock).map { numberBlock =>
//                Lazy[Result, BlockHeaderJson] {
//                    channel.getBlockByNumber(numberBlock).map { blockInfo =>
//                        readBlockHeaderToJson(blockInfo.getBlock.getHeader)
//                    }
//                }
//            }
//        ).map(f =>
//            HeadersCNFT(f.toArray)
//        )
//    }
//
//    private def getParseBlockNumber(blockNumber: Long): Option[BlockJson] = {
//        channel.getBlockByNumber(blockNumber) match {
//            case Left(_) => None
//            case Right(value) =>
//                Option(
//                    BlockJson(
//                        readBlockHeaderToJson(value.getBlock.getHeader),
//                        readBlockMetadataToJson(value.getBlock),
//                        parseBlockData(value.getBlock),
//                        getData(value.getBlock)
//                    )
//                )
//        }
//    }
//
//    private def parseInput(input: ChaincodeInput, method: String): Value = {
//        val args = input.getArgsList
//        method match {
//            case CNFTOperation.RegisterTokenType =>
//                val registerTokenType = codec.
//                    decode[Array[RegisterTokenType]](
//                        args.get(2).toByteArray,
//                        classOf[Array[RegisterTokenType]]
//                    )
//                val res = upickle.default.writeJs[Array[RegisterTokenType]](registerTokenType)
//                ujson.Obj(
//                    "gateId" -> args.get(1).toStringUtf8,
//                    method -> res
//                )
//            case ReserveTokenIDs =>
//                val reserveTokenIDsRequest = ReserveTokenIDsRequest.parseFrom(args.get(1).toByteArray)
//                val reserveJson = upickle.default.writeJs[ReserveTokenIDsRequest](reserveTokenIDsRequest)
//                ujson.Obj(method -> reserveJson)
//            case MakeDeal =>
//                val dealRequest = DealRequest.parseFrom(args.get(1).toByteArray)
//                ujson.Obj(
//                    method -> upickle.default.writeJs[DealRequest](dealRequest)
//                )
//            case BurnToken =>
//                val signedBurnRequest = SignedBurnRequest.parseFrom(args.get(1).toByteArray)
//                ujson.Obj(
//                    method -> upickle.default.writeJs[SignedBurnRequest](signedBurnRequest)
//                )
//            case PutOffers =>
//                val putOffers = codec.
//                    decode[Array[PutOfferRequest]](
//                        args.get(1).toByteArray,
//                        classOf[Array[PutOfferRequest]]
//                    )
//                ujson.Obj(
//                    method -> upickle.default.writeJs[Array[PutOfferRequest]](putOffers)
//                )
//            case IssueTokens =>
//                val issueTokens = codec.
//                    decode[Array[IssueTokenRequest]](
//                        args.get(1).toByteArray,
//                        classOf[Array[IssueTokenRequest]]
//                    )
//                ujson.Obj(
//                    method -> upickle.default.writeJs[Array[IssueTokenRequest]](issueTokens)
//                )
//            case CloseOffers =>
//                val closeOffers = codec.
//                    decode[Array[String]](
//                        args.get(1).toByteArray,
//                        classOf[Array[String]]
//                    )
//                ujson.Obj(
//                    method -> upickle.default.writeJs[Array[String]](closeOffers)
//                )
//            case PublishMessages =>
//                val publishMessages = codec.
//                    decode[Array[Message]](
//                        args.get(1).toByteArray,
//                        classOf[Array[Message]]
//                    )
//                ujson.Obj(
//                    method -> upickle.default.writeJs[Array[Message]](publishMessages)
//                )
//            case RegisterMember =>
//                val registerMember = codec.
//                    decode[MemberInformation](
//                        args.get(1).toByteArray,
//                        classOf[MemberInformation]
//                    )
//                ujson.Obj(
//                    method -> upickle.default.writeJs[MemberInformation](registerMember)
//                )
//
//            case FreezeToken =>
//                val freezeToken = TokenFreezeRequest.parseFrom(args.get(1).toByteArray)
//                upickle.default.writeJs[TokenFreezeRequest](freezeToken)
//            case RegulatoryBurnToken =>
//                val regulatoryBurnRequest = RegulatorBurnRequest.parseFrom(args.get(1).toByteArray)
//                upickle.default.writeJs[RegulatorBurnRequest](regulatoryBurnRequest)
//            case _ => "not Found ARGS"
//        }
//    }
//
//    private def parseResponseTransaction(responsePayload: ByteString, transaction: String): Value = {
//        transaction match {
//            case CNFTOperation.RegisterTokenType =>
//                val tokenType = codec.
//                    decode[Array[TokenType]](
//                        responsePayload.toByteArray,
//                        classOf[Array[TokenType]]
//                    )
//                upickle.default.writeJs[Array[TokenType]](tokenType)
//
//            case ReserveTokenIDs =>
//                val reservedId = codec
//                    .decode[Array[ReservedId]](
//                        responsePayload.toByteArray,
//                        classOf[Array[ReservedId]]
//                    )
//                upickle.default.writeJs[Array[ReservedId]](reservedId)
//
//            case IssueTokens =>
//                val responses = codec
//                    .decode[Array[IssueTokenResponse]](
//                        responsePayload.toByteArray,
//                        classOf[Array[IssueTokenResponse]]
//                    )
//                upickle.default.writeJs[Array[IssueTokenResponse]](responses)
//
//            case MakeDeal =>
//                upickle.default.writeJs[String](responsePayload.toStringUtf8)
//
//            case BurnToken =>
//                upickle.default.writeJs[String](responsePayload.toStringUtf8)
//
//            case PutOffers =>
//                val offer = codec.
//                    decode[Array[Offer]](
//                        responsePayload.toByteArray,
//                        classOf[Array[Offer]]
//                    )
//                upickle.default.writeJs[Array[Offer]](offer)
//
//            case CloseOffers =>
//                val closeOffers = codec.
//                    decode[Array[String]](
//                        responsePayload.toByteArray,
//                        classOf[Array[String]]
//                    )
//                upickle.default.writeJs[Array[String]](closeOffers)
//
//            case PublishMessages =>
//                val message = codec.
//                    decode[Array[Message]](
//                        responsePayload.toByteArray,
//                        classOf[Array[Message]]
//                    )
//                upickle.default.writeJs[Array[Message]](message)
//
//            case FreezeToken =>
//                upickle.default.writeJs[String](responsePayload.toStringUtf8)
//
//            case RegulatoryBurnToken =>
//                upickle.default.writeJs[String](responsePayload.toStringUtf8)
//
//            case _ => upickle.default.writeJs(responsePayload.toStringUtf8)
//        }
//    }
//
//    private def getEndorsementJson(chaincodeEndorsedAction: TransactionPackage.ChaincodeEndorsedAction): Value = {
//        val endorsement = chaincodeEndorsedAction.getEndorsementsList.map { endorsement =>
//            val creator = SerializedIdentity.parseFrom(endorsement.getEndorser)
//            model.Endorsement(
//                endorser = model.Creator(
//                    creator.getMspid,
//                    creator.getIdBytes.toStringUtf8
//                ),
//                signature = new String(Base64.getEncoder.encode(endorsement.getSignature.toByteArray), StandardCharsets.UTF_8),
//            )
//        }.toArray
//        upickle.default.writeJs(endorsement)
//    }
//
//    private def getRWSetJson(results: TxReadWriteSet): Value = {
//        val nsRWSet = results.getNsRwsetList.map { nsRWSet =>
//            val rwSet = KVRWSet.parseFrom(nsRWSet.getRwset)
//            NsRwset(
//                nameSpace = nsRWSet.getNamespace,
//                rwSet = RwSet(
//                    rwSet.getReadsList.map { reads =>
//                        Reads(
//                            reads.getKey,
//                            reads.getVersion.toString
//                        )
//                    }.toArray
//                )
//            )
//        }.toArray
//        ujson.read(
//            upickle.default.write[Array[NsRwset]](nsRWSet)
//        )
//    }
//
//    private def getData(block: Block): Array[String] = {
//        for {
//            data <- block.getData.getDataList
//            envelope = Common.Envelope.parseFrom(data)
//            payload = Common.Payload.parseFrom(envelope.getPayload)
//            transaction = Transaction.parseFrom(payload.getData)
//            action = transaction.getActionsList.map { action =>
//                val chaincodeActionPayload = ChaincodeActionPayload.parseFrom(action.getPayload)
//                val chaincodeProposalPayload = ChaincodeProposalPayload.parseFrom(chaincodeActionPayload.getChaincodeProposalPayload)
//                val ccSpec = ChaincodeInvocationSpec.parseFrom(chaincodeProposalPayload.getInput).getChaincodeSpec
//                val ccIdSpec = ccSpec.getChaincodeId
//                val chaincodeEndorsedAction = chaincodeActionPayload.getAction
//                val proposalResponsePayload = ProposalResponsePayload.parseFrom(chaincodeEndorsedAction.getProposalResponsePayload)
//                val chaincodeAction = ChaincodeAction.parseFrom(proposalResponsePayload.getExtension)
//                val ccIdAction = chaincodeAction.getChaincodeId
//                val response = chaincodeAction.getResponse
//                val ccEvents = ChaincodeEvent.parseFrom(chaincodeAction.getEvents)
//                val results = TxReadWriteSet.parseFrom(chaincodeAction.getResults)
//                val method = ccSpec.getInput.getArgsList.get(0).toStringUtf8
//                ujson.read(
//                    ujson.Obj("TransactionAction" ->
//                        ujson.Obj("ChaincodeActionPayload" ->
//                            ujson.Obj(
//                                "ChaincodeProposalPayload" -> ujson.Obj(
//                                    "ChaincodeInvocationSpec" -> ujson.Obj(
//                                        "ChaincodeSpec" -> ujson.Obj(
//                                            "ChaincodeId" -> ujson.Obj(
//                                                "path" -> ccIdSpec.getPath,
//                                                "name" -> ccIdSpec.getName,
//                                                "version" -> ccIdSpec.getVersion
//                                            ),
//                                            "proposal" -> parseInput(ccSpec.getInput, method)
//                                        )
//                                    )
//                                ),
//                                "ChaincodeEndorsedAction" ->
//                                    ujson.Obj("ProposalResponsePayload" ->
//                                        ujson.Obj(
//                                            "proposalHash" -> new String(
//                                                Base64.getEncoder.encode(proposalResponsePayload.getProposalHash.toByteArray),
//                                                StandardCharsets.UTF_8
//                                            ),
//                                            "ChaincodeAction" ->
//                                                ujson.Obj(
//                                                    "ChaincodeAction" -> ujson.Obj(
//                                                        "ChaincodeId" -> ujson.Obj(
//                                                            "path" -> ccIdAction.getPath,
//                                                            "name" -> ccIdAction.getName,
//                                                            "version" -> ccIdAction.getVersion
//                                                        ),
//                                                        "Response" -> ujson.Obj(
//                                                            "status" -> response.getStatus,
//                                                            "message" -> response.getMessage,
//                                                            "payload" -> parseResponseTransaction(
//                                                                chaincodeAction.getResponse.getPayload,
//                                                                method
//                                                            )
//                                                        ),
//                                                        "Events" -> ujson.Obj(
//                                                            "chaincode_id" -> ccEvents.getChaincodeId,
//                                                            "txId" -> ccEvents.getTxId,
//                                                            "event_name" -> ccEvents.getEventName,
//                                                            "payload" -> ccEvents.getPayload.toStringUtf8
//                                                        ),
//                                                        "Results" -> ujson.Obj(
//                                                            "dataModel" -> results.getDataModel.getNumber,
//                                                            "ns_rwset" -> getRWSetJson(results)
//                                                        )
//                                                    )
//                                                )
//                                        ),
//                                        "Endorsement" -> getEndorsementJson(chaincodeEndorsedAction)
//                                    )
//                            )
//                        )
//                    )
//                )
//            }.toArray
//        } yield {
//            ujson.write(
//                ujson.Obj(
//                    "Envelope" -> ujson.Obj(
//                        "Payload" -> ujson.Obj(
//                            "channelHeader" -> getChannelHeader(payload.getHeader.getChannelHeader),
//                            "signatureHeader" -> getSignatureHeader(payload.getHeader.getSignatureHeader)
//                        ),
//                        "Actions" -> upickle.default.writeJs(
//                            action.map(f => f)
//                        )
//                    ),
//                    "Signature" -> new String(Base64.getEncoder.encode(envelope.getSignature.toByteArray), StandardCharsets.UTF_8)
//                )
//            )
//        }
//    }.toArray
//
//    private def getSignatureHeader(signatureHeader: ByteString): Value = {
//        val sh = SignatureHeader.parseFrom(signatureHeader)
//        val si = SerializedIdentity.parseFrom(sh.getCreator)
//        ujson.Obj("SignatureHeader" ->
//            ujson.Obj("Creator" ->
//                ujson.Obj(
//                    "mspId" -> si.getMspid,
//                    "idBytes" -> si.getIdBytes.toStringUtf8
//                )
//            ),
//            "nonce" -> new String(Base64.getEncoder.encode(sh.getNonce.toByteArray), StandardCharsets.UTF_8)
//        )
//    }
//
//    private def getChannelHeader(channelHeader: ByteString): Value = {
//        val ch = ChannelHeader.parseFrom(channelHeader)
//        val extension = ChaincodeHeaderExtension.parseFrom(ch.getExtension)
//        ujson.Obj(
//            "ChannelHeader" -> ujson.Obj(
//                "type" -> ch.getType,
//                "version" -> ch.getVersion,
//                "timestamp" -> ujson.Obj(
//                    "nanos" -> ch.getTimestamp.getNanos,
//                    "seconds" -> ch.getTimestamp.getSeconds,
//                    "date" -> new Date(ch.getTimestamp.getSeconds * 1000).toString
//                ),
//                "channelId" -> ch.getChannelId,
//                "txId" -> ch.getTxId,
//                "epoch" -> ch.getEpoch,
//                "extension" -> ujson.Obj(
//                    "path" -> extension.getChaincodeId.getPath,
//                    "name" -> extension.getChaincodeId.getName,
//                    "version" -> extension.getChaincodeId.getVersion
//                ),
//                "tlsCert" -> ch.getTlsCertHash.toStringUtf8
//            )
//        )
//    }
//
//    private def parseBlockData(block: Block): BlockDataJson = {
//        val envelope = Common.Envelope.parseFrom(block.getData.getData(0))
//        val payload = Common.Payload.parseFrom(envelope.getPayload)
//        val transaction = Transaction.parseFrom(payload.getData)
//        val chaincodeActionPayload = ChaincodeActionPayload.parseFrom(transaction.getActions(0).getPayload)
//        val chaincodeProposalPayload = ChaincodeProposalPayload.parseFrom(chaincodeActionPayload.getChaincodeProposalPayload)
//        val chaincodeInvocationSpec = ChaincodeInvocationSpec.parseFrom(chaincodeProposalPayload.getInput)
//        val arrayArgcString = chaincodeInvocationSpec.getChaincodeSpec.getInput.getArgsList.map(arg =>
//            arg.toStringUtf8
//        ).toArray
//
//        val transactionJson = TransactionJson(
//            args = arrayArgcString
//        )
//        BlockDataJson(
//            data = new String(Base64.getEncoder.encode(block.getData.toByteArray), StandardCharsets.UTF_8),
//            transactionsJson = transactionJson
//        )
//    }
//
//    private def readBlockHeaderToJson(blockHeader: Common.BlockHeader): BlockHeaderJson = {
//        BlockHeaderJson(
//            number = blockHeader.getNumber,
//            dataHashB64 = new String(Base64.getEncoder.encode(blockHeader.getDataHash.toByteArray), StandardCharsets.UTF_8),
//            previousHashB64 = new String(Base64.getEncoder.encode(blockHeader.getPreviousHash.toByteArray), StandardCharsets.UTF_8),
//            bytesB64 = new String(Base64.getEncoder.encode(blockHeader.toByteArray), StandardCharsets.UTF_8)
//        )
//    }
//
//    private def readBlockMetadataToJson(block: Common.Block): BlockMetaDataJson = {
//        val metadataProtoc = Metadata.parseFrom(block.getMetadata.getMetadata(Common.BlockMetadataIndex.SIGNATURES_VALUE).toByteArray)
//        val testSignEncodeToString = new String(
//            Base64.getEncoder.encode(metadataProtoc.getSignaturesList.head.getSignature.toByteArray),
//            StandardCharsets.UTF_8
//        )
//        val signHeader = SignatureHeader.parseFrom(metadataProtoc.getSignaturesList.head.getSignatureHeader)
//        val creator = msp.Identities.SerializedIdentity.parseFrom(signHeader.getCreator)
//        val creatorJson = CreatorJson(
//            mspId = creator.getMspid,
//            certHash = creator.getIdBytes.toStringUtf8
//        )
//        BlockMetaDataJson(
//            value = new String(Base64.getEncoder.encode(metadataProtoc.getValue.toByteArray), StandardCharsets.UTF_8),
//            signature = testSignEncodeToString,
//            signatureHeader = SignatureHeaderJson(creator = creatorJson),
//            signatureHeaderB64 = new String(Base64.getEncoder.encode(signHeader.toByteArray), StandardCharsets.UTF_8)
//        )
//    }
//
//    override def getProof(tokenId: String): Result[Collection[BlockInfo]] = {
//        getLatestBlockNumber.flatMap { lastBlock =>
//            ResultOps
//                .foldFailFast(
//                    (0L to lastBlock).map { blockNumber =>
//                        Lazy[Result, (Long, BlockEvents)] {
//                            getTransactions(blockNumber).map(e => (blockNumber, e))
//                        }
//                    }
//                )
//                .flatMap { events =>
//                    ResultOps.foldFailFast(
//                        events
//                            .filter { case (_, blockEvents) =>
//                                blockEvents.tokensIssued.exists(tokensIssued => tokensIssued.event.id.tokenId == tokenId) ||
//                                    blockEvents.tokensBurned.exists(tokensBurned => tokensBurned.event.request.tokenId == tokenId) ||
//                                    blockEvents.idsReserved.exists(idsReserved => idsReserved.event.tokenId == tokenId) ||
//                                    blockEvents.regulatorBurned.exists(regulatory => regulatory.event.request.tokenId == tokenId) ||
//                                    blockEvents.tokenFrozen.exists(tokenFrozen => tokenFrozen.event.tokenIds.contains(tokenId)) ||
//                                    blockEvents.tokensExchanged.exists(deal => deal.event.dealRequest.deal.changes
//                                        .exists {
//                                            case (tokenIdDeal, _) =>
//                                                tokenIdDeal == tokenId
//                                        }
//                                    )
//                            }
//                            .map { case (blockNumber, _) =>
//                                Lazy {
//                                    getBlockInfo(blockNumber)
//                                }
//                            }
//                    )
//                }
//        }.flatMap { proofJsons =>
//            if (proofJsons.nonEmpty)
//                Result.Ok(collectionFromSequence(proofJsons))
//            else
//                Result.Fail("Not found token id")
//        }
//    }
//
//}
