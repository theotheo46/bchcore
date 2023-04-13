package ru.sberbank.blockchain.cnft.chaincode.store

import org.enterprisedlt.fabric.contract.OperationContext
import org.enterprisedlt.spec.{Key, KeyValue}
import ru.sberbank.blockchain.cnft.CurrentPlatformVersion
import ru.sberbank.blockchain.cnft.common.types.{Collection, Result, collectionFromIterable}
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.engine.CNFTStore
import ru.sberbank.blockchain.cnft.model._

object ChaincodeCNFTStore extends CNFTStore with LoggingSupport {

    override def saveTokenLinkedOperation(tokenId: String, transactionId: String): Unit = {
        logger.debug(s"Store TokenLinkedOperation for $tokenId: $transactionId")
        OperationContext.store.put[TokenLinkedOperation](tokenId, TokenLinkedOperation(transactionId))
    }

    override def getTokenLinkedOperation(tokenId: String): Option[String] = {
        logger.debug(s"Get TokenLinkedOperation for $tokenId")
        OperationContext.store.get[TokenLinkedOperation](tokenId).map(_.id)
    }

    override def removeTokenLinkedOperation(tokenId: String): Unit = {
        logger.debug(s"Remove TokenLinkedOperation for $tokenId")
        OperationContext.store.del[TokenLinkedOperation](tokenId)
    }

    // Endorsement
    override def saveEndorsement(memberId: String, endorsement: SignedEndorsement): Unit = {
        logger.debug(s"Store endorsement")
        val updated = OperationContext.store
            .get[Array[SignedEndorsement]](memberId)
            .map { e => e :+ endorsement }
            .getOrElse(Collection(endorsement))

        OperationContext.store.put[Array[SignedEndorsement]](memberId, updated)
    }

    override def savePublicEndorsement(memberId: String, endorsement: SignedPublicEndorsement): Unit = {
        logger.debug(s"Store public endorsement")
        val updated = OperationContext.store
            .get[Array[SignedPublicEndorsement]](memberId)
            .map { e =>
                e.filterNot(a => a.endorsement.endorserId == endorsement.endorsement.endorserId && a.endorsement.kindId == endorsement.endorsement.kindId) :+ endorsement
            }
            .getOrElse(Collection(endorsement))

        OperationContext.store.put[Array[SignedPublicEndorsement]](memberId, updated)
    }

    override def revokePublicEndorsement(memberId: String, endorserId: String, kindId: String): Unit = {
        logger.debug(s"Revoke public endorsement")
        val updated = OperationContext.store
            .get[Array[SignedPublicEndorsement]](memberId)
            .map { e =>
                e.filterNot(a => a.endorsement.kindId == kindId && a.endorsement.endorserId == endorserId)
            }
            .getOrElse(Collection.empty)

        OperationContext.store.put[Array[SignedPublicEndorsement]](memberId, updated)
    }

    override def getEndorsements(memberId: String): Array[SignedEndorsement] = {
        logger.debug(s"Get Endorsement")
        OperationContext.store.get[Array[SignedEndorsement]](memberId)
            .getOrElse(Collection.empty)
    }

    override def getPublicEndorsements(memberId: String): Array[SignedPublicEndorsement] = {
        logger.debug(s"Get Public Endorsement")
        OperationContext.store.get[Array[SignedPublicEndorsement]](memberId)
            .getOrElse(Collection.empty)
    }

    // Profiles
    override def getProfile(id: String): Option[Profile] = {
        logger.debug(s"Get public profile by: '$id'")
        OperationContext.store.get[Profile](id)
    }

    override def listProfiles: Array[Profile] =
        OperationContext.store.list[Profile].map { x =>
            logger.debug(s"\t-\tPublic profile id: '${x.key}'")
            x.value
        }.toArray

    override def saveProfile(profile: Profile): Unit = {
        logger.debug(s"Store public profile: '$profile'")
        OperationContext.store.put[Profile](profile.id, profile)
    }

    override def saveProfileTokens(profileTokens: ProfileTokens): Unit = {
        logger.debug(s"Saving tokens linked to profile address: '${profileTokens.profileId}'")
        OperationContext.store.put[ProfileTokens](profileTokens.profileId, profileTokens)
    }

    override def removeProfileTokens(profileId: String): Unit = {
        logger.debug(s"Removing tokens linked to profile address: '$profileId'")
        OperationContext.store.del[ProfileTokens](profileId)
    }

    override def getLinkedToProfileTokenIds(profileId: String): Array[String] = {
        logger.debug(s"Getting profile tokens linked to profile: '$profileId'")
        OperationContext.store.get[ProfileTokens](profileId)
            .map(_.tokenIds)
            .getOrElse(Array.empty)
    }

    override def saveTokenProfiles(tokenProfiles: TokenProfiles): Unit = {
        logger.debug(s"Saving token profiles linked to token: ${tokenProfiles.tokenId}")
        OperationContext.store.put[TokenProfiles](tokenProfiles.tokenId, tokenProfiles)
    }

    override def getTokenProfiles(tokenId: String): Collection[TokenProfileInfo] = {
        logger.debug(s"Getting token profiles for tokenId: $tokenId")
        OperationContext.store.get[TokenProfiles](tokenId)
            .map(_.tokenProfileInfos)
            .getOrElse(Array.empty)
    }

    // Smart Contracts
    override def getSmartContract(id: String): Option[SmartContract] = {
        logger.debug(s"Get smart contract")
        OperationContext.store.get[SmartContract](id)
    }

    override def saveSmartContract(smartContract: SmartContract): Unit = {
        logger.debug(s"Store smart contract")
        OperationContext.store.put[SmartContract](smartContract.id, smartContract)
    }

    override def listSmartContracts: Array[SmartContract] = OperationContext.store.list[SmartContract].map { x =>
        logger.debug(s"\t-\tSmart contract id: '${x.key}'")
        x.value
    }.toArray

    override def deleteSmartContract(id: String): Unit = {
        logger.debug(s"Deleting smart contract")
        OperationContext.store.del[SmartContract](id)
    }


    override def saveSmartContractState(smartContractState: SmartContractState): Unit = {
        logger.debug(s"Store smart contract state for [${smartContractState.id}]")
        OperationContext.store.put[SmartContractState](smartContractState.id, smartContractState)
    }

    override def getSmartContractState(id: String): Option[SmartContractState] = {
        logger.debug(s"Get smart contract state")
        OperationContext.store.get[SmartContractState](id)
    }

    override def deleteSmartContractState(id: String): Unit = {
        logger.debug(s"Deleting smart contract state")
        OperationContext.store.del[SmartContractState](id)
    }

    override def getSmartContractAcceptedDeals(id: String): Collection[AcceptedDeal] = {
        logger.debug(s"Get tokens for smart contract [${id}]")
        OperationContext.store.get[SmartContractAcceptedDeals](id) match {
            case Some(value) => value.deal
            case None => Collection.empty
        }
    }

    override def appendSmartContractAcceptedDeal(id: String, acceptedDeal: AcceptedDeal): Result[Unit] = {
        Result {
            logger.debug(s"Updating smart contract accepted token for [${id}]")
            val toSave = OperationContext.store.get[SmartContractAcceptedDeals](id) match {
                case Some(value) =>
                    value.copy(
                        deal = value.deal :+ acceptedDeal
                    )

                case None =>
                    SmartContractAcceptedDeals(
                        id, Collection(acceptedDeal)
                    )
            }
            OperationContext.store.put[SmartContractAcceptedDeals](id, toSave)
        }
    }

    override def saveSmartContractRegulation(smartContractRegulation: SmartContractRegulation): Unit = {
        logger.debug(s"Store smart contract regulation")
        OperationContext.store.put[SmartContractRegulation](smartContractRegulation.id, smartContractRegulation)
    }

    override def getSmartContractRegulation(id: String): Option[SmartContractRegulation] = {
        logger.debug(s"Get smart contract regulation")
        OperationContext.store.get[SmartContractRegulation](id)
    }

    //    override def deleteSmartContractRegulation(smartContractAddress: Array[Byte]): Unit = {
    //        logger.debug(s"Deleting smart contract regulation")
    //        OperationContext.store.del[SmartContractRegulation](smartContractAddress)
    //    }

    //
    //    // Smart Contract Templates
    //    override def getSmartContractTemplate(templateId: Array[Byte]): Option[SmartContractTemplate] = {
    //        logger.debug(s"Get smart contract template")
    //        OperationContext.store.get[SmartContractTemplate](smartContractTemplateAddress)
    //    }
    //
    //    override def saveSmartContractTemplate(smartContractTemplate: SmartContractTemplate): Unit = {
    //        logger.debug(s"Store smart contract template")
    //        OperationContext.store.put[SmartContractTemplate](smartContractTemplate.address, smartContractTemplate)
    //    }
    //
    //    override def listSmartContractTemplates: Array[SmartContractTemplate] = OperationContext.store.list[SmartContractTemplate].map { x =>
    //        logger.debug(s"\t-\tSmart contract template id: '${x.key}'")
    //        x.value
    //    }.toArray


    // Data feeds
    override def getDataFeed(dataFeedId: String): Option[DataFeed] = {
        logger.debug(s"Get data feed")
        OperationContext.store.get[DataFeed](dataFeedId)
    }

    override def saveDataFeed(dataFeed: DataFeed): Unit = {
        logger.debug(s"Store data feed")
        OperationContext.store.put[DataFeed](dataFeed.id, dataFeed)
    }

    override def saveDataFeedValue(dataFeedValue: DataFeedValue): Unit = {
        logger.debug(s"Store data feed value")
        OperationContext.store.put[DataFeedValue](dataFeedValue.id, dataFeedValue)
    }

    override def getDataFeedValue(dataFeedId: String): Option[DataFeedValue] = {
        logger.debug(s"Get data feed for value")
        OperationContext.store.get[DataFeedValue](dataFeedId)
    }

    override def listDataFeeds: Array[DataFeed] = OperationContext.store.list[DataFeed].map { x =>
        x.value
    }.toArray


    override def addDataFeedSubscriber(dataFeedId: String, subscriberAddress: String): Unit = {
        logger.debug(s"DTF save data feed ${dataFeedId} subscriber ${subscriberAddress}")
        OperationContext.store.put[Array[Byte]](makeKeyDataFeed(dataFeedId, subscriberAddress), Array[Byte](0))
    }

    override def deleteDataFeedSubscriber(dataFeedId: String, subscriberAddress: String): Unit = {
        logger.debug(s"DTF del subscriber ${subscriberAddress} for data feed ${dataFeedId}")
        OperationContext.store.del[Array[Byte]](makeKeyDataFeed(dataFeedId, subscriberAddress))
    }

    override def listDataFeedSubscribers(dataFeedId: String): Result[Collection[String]] = {
        logger.debug(s"DTF list subscribers for data feed ${dataFeedId}")
        Result {
            collectionFromIterable(
                OperationContext.store.listKeys(classOf[Array[Byte]], makeKeyDataFeed(dataFeedId))
                    .map { encodedKey => encodedKey.parts(2) }
            )
        }
    }

    def makeKeyDataFeed(arg: String*): Key = Key("DataFeedSubscribers" +: arg: _*)

    //
    // Token types
    //

    override def getTokenType(typeId: String): Option[TokenType] = {
        logger.debug(s"Get token type for: '${typeId}")
        OperationContext.store.get[TokenType](typeId)
    }

    override def saveTokenType(tokenType: TokenType): Unit = {
        logger.debug(s"Store token type for: '${tokenType.typeId}'")
        OperationContext.store.put[TokenType](tokenType.typeId, tokenType)
    }

    override def listTokenTypes: Array[TokenType] =
        OperationContext.store.list[TokenType].map { x =>
            logger.debug(s"\t-\tToken type id: '${x.key}'")
            x.value
        }.toArray

    // TODO full-featured filtering
    override def listTokenTypesWithChangeGene(changeGeneId: String, negation: Boolean): Array[TokenType] =
        OperationContext.store.list[TokenType]
            .withFilter { case KeyValue(_, tokenType) =>
                val geneExists = tokenType.dna.change.exists(_.id == changeGeneId)
                if (negation) !geneExists else geneExists
            }
            .map(_.value)
            .toArray

    //
    // Tokens
    //

    override def createToken(tokenId: String, owner: TokenOwner, body: TokenContent): Result[Unit] = Result {
        logger.debug(s"Creating new token with ID: ${tokenId}")
        OperationContext.store.put(tokenId, owner)
        OperationContext.store.put(tokenId, body)
        OperationContext.store.put(tokenId, TokenRestrictions(Collection.empty))
    }

    override def tokenExist(tokenId: String): Result[Boolean] = Result {
        logger.debug(s"Querying owner for token: ${tokenId}")
        OperationContext.store.get[TokenOwner](tokenId).isDefined
    }

    override def getTokenOwner(tokenId: String): Result[TokenOwner] = Result {
        logger.debug(s"Querying owner for token: ${tokenId}")
        OperationContext.store.get[TokenOwner](tokenId)
    }.flatMap(_.toRight(s"Token Owner does not exist ${tokenId}"))

    override def getTokenBody(tokenId: String): Option[TokenContent] = {
        logger.debug(s"Querying body for token: ${tokenId}")
        OperationContext.store.get[TokenContent](tokenId)
    }

    override def updateTokenOwner(tokenId: String, owner: TokenOwner): Result[Unit] = Result {
        logger.debug(s"Updating owner for token with ID: ${tokenId}")
        OperationContext.store.put(tokenId, owner)
    }

    override def deleteToken(tokenId: String): Result[Unit] = {
        logger.debug(s"Removing token with ID: ${tokenId}")
        Result(OperationContext.store.del[TokenOwner](tokenId))
    }

    // Restrictions

    override def getTokenRestrictions(tokenId: String): Result[TokenRestrictions] = Result {
        logger.debug(s"Querying restrictions for token: ${tokenId}")
        OperationContext.store.get[TokenRestrictions](tokenId)
    }.map(_.getOrElse(TokenRestrictions(Collection.empty)))


    override def addTokenRestriction(tokenId: String, restriction: Restriction): Result[Unit] = {
        logger.debug(s"Adding restriction for token: ${tokenId}")
        for {
            maybeCurrent <- Result(OperationContext.store.get[TokenRestrictions](tokenId))
            restrictions = maybeCurrent
                .map { current =>
                    current.copy(
                        current.restrictions :+ restriction
                    )
                }
                .getOrElse(TokenRestrictions(Collection(restriction)))
            _ <- Result {
                OperationContext.store.put[TokenRestrictions](tokenId, restrictions)
            }
        } yield restrictions
    }.map { updatedRestrictions =>
        logger.debug(s"Updated restrictions are:\n ${updatedRestrictions.toProtoString}")
    }


    override def addTokenRestrictions(tokenId: String, newRestrictions: TokenRestrictions): Result[Unit] = {
        logger.debug(s"Adding restriction for token: ${tokenId}")
        for {
            maybeCurrent <- Result(OperationContext.store.get[TokenRestrictions](tokenId))
            restrictions: TokenRestrictions = maybeCurrent
                .map { current =>
                    TokenRestrictions(current.restrictions ++ newRestrictions.restrictions)
                }
                .getOrElse(newRestrictions)
            _ <- Result {
                OperationContext.store.put[TokenRestrictions](tokenId, restrictions)
            }
        } yield restrictions
    }.map { updated =>
        logger.debug(s"Updated restrictions are:\n ${updated.restrictions.map(_.toProtoString).mkString(",")}")
    }


    override def clearRestrictions(tokenId: String): Result[Unit] = {

        Result(OperationContext.store.del[TokenRestrictions](tokenId))

    }

    // TODO: can the regulator have multiple restrictions?
    override def removeTokenRestriction(tokenId: String, regulatorId: String): Result[Unit] = {
        logger.debug(s"Remove Token restriction for token: ${tokenId} by ${regulatorId}")
        for {
            maybeCurrent <- Result(OperationContext.store.get[TokenRestrictions](tokenId))
            current <- maybeCurrent.toRight(s"Token does not exist ${tokenId}")
            // TODO: shell we also check the restriction exist?
            restrictions = current.copy(
                current.restrictions.filter(_.regulatorId != regulatorId)
            )
            _ <- Result {
                OperationContext.store.put[TokenRestrictions](tokenId, restrictions)
            }
        } yield restrictions
    }.map { updatedRestrictions =>
        logger.debug(s"Updated restrictions are:\n ${updatedRestrictions.toProtoString}")
    }

    //
    // Offers
    //

    override def saveOffer(offerId: String, offer: Offer): Unit = {
        logger.debug(s"Storing Offer: $offerId")
        OperationContext.store.put[Offer](offerId, offer)
    }

    override def deleteOffer(offerId: String): Unit = {
        logger.debug(s"Deleting Offer: $offerId")
        OperationContext.store.del[Offer](offerId)
    }

    //
    // Memberships info
    //

    override def getMemberInfo(id: String): Option[MemberInformation] =
        OperationContext.store.get[MemberInformation](id)


    override def listMemberInfo: Array[MemberInformation] =
        OperationContext.store.list[MemberInformation].map { x =>
            x.value
        }.toArray

    override def saveMemberInfo(memberInformation: MemberInformation): Unit = {
        logger.debug(s"Store data feed value")
        OperationContext.store.put[MemberInformation](memberInformation.id, memberInformation)
    }

    override def saveMemberRegistrationBlock(memberId: String, blockNumber: Long): Unit = {
        OperationContext.store.put[Long](memberId, blockNumber)
    }

    override def getMemberRegistrationBlock(memberId: String): Option[Long] = {
        OperationContext.store.get[Long](memberId)
    }

    override def listTokens: Array[String] = {
        OperationContext.store.list[TokenOwner].map(kv => kv.key).toArray
    }

    override def listBurntTokens: Array[BurntToken] =
        OperationContext.store.list[BurntToken].map(kv => kv.value).toArray

    override def getBurntToken(tokenId: String): Option[BurntToken] =
        OperationContext.store.get[BurntToken](tokenId)

    override def saveBurntToken(token: BurntToken): Unit =
        OperationContext.store.put[BurntToken](token.tokenId, token)

    // Messages

    override def saveMessages(id: String, msg: MessageRequest): Unit = {
        logger.debug(s"MSG saving message with id ${id}")
        OperationContext.store.put[MessageRequest](id, msg)
        OperationContext.store.put[MemberMessages](id, MemberMessages(msg.message.to, msg.message.from, id))
    }

    override def listMessages(to: String): Array[MessageRequest] = {
        logger.debug(s"MSG Querying list of messages to $to")
        OperationContext.store
            .list[MemberMessages]
            .map(_.value)
            .filter(_.to == to)
            .map(_.id)
            .flatMap(id =>
                OperationContext.store.get[MessageRequest](id)
            )
            .toArray
    }

    override def listMessagesFrom(from: String): Array[MessageRequest] = {
        logger.debug(s"MSG Querying list of messages from $from")
        OperationContext.store
            .list[MemberMessages]
            .map(_.value)
            .filter(_.from == from)
            .map(_.id)
            .flatMap(id =>
                OperationContext.store.get[MessageRequest](id)
            )
            .toArray
    }

    // Operations

    override def saveOperation(operationId: String, operationData: OperationHistory): Unit = {
        logger.debug(s"Saving operation: $operationId")
        OperationContext.store.put[OperationHistory](operationId, operationData)
    }

    override def getOperation(operationId: String): Option[OperationHistory] = {
        logger.debug(s"Querying operation: $operationId")
        OperationContext.store.get[OperationHistory](operationId)
    }

    override def listOperations: Collection[OperationHistory] =
        OperationContext.store.list[OperationHistory].map(kv => kv.value).toArray

    override def saveCurrentPlatformVersionInfo(v: PlatformVersion): Unit =
        OperationContext.store.put[PlatformVersion]("version", v)

    override def getPlatformVersionInfo: PlatformVersion =
        OperationContext.store.get[PlatformVersion]("version")
            .getOrElse(PlatformVersion(CurrentPlatformVersion, 0L))
}
