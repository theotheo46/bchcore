package ru.sberbank.blockchain.cnft.wallet.test

import ru.sberbank.blockchain.cnft.common.types.{Collection, Result}
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.model.{Approve, BurnRequest, BurnResponse, DataFeed, DealRejectedBySmartContract, DealRequest, DescriptionField, FieldMeta, IssueTokenRequest, OperationData, PendingBurn, PendingDeal, PendingIssue, Profile, RegulatorApproval, RegulatorTransferRequest, Restriction, SignedEndorsement, SignedPublicEndorsement, SmartContract, SmartContractRegulation, SmartContractTemplate, TokenRequest, TokenType, TransferProposal, WalletToken}
import ru.sberbank.blockchain.cnft.wallet.spec.{CNFTWalletSpec, WalletEvents}
import ru.sberbank.blockchain.cnft.wallet.walletmodel.AddressInfo

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.util.{Failure, Success, Try}

/**
 * @author Maxim Fedin
 */
object Util extends LoggingSupport {

    import ru.sberbank.blockchain.cnft.wallet.dsl._

    trait EventsWaiter[X] {
        def get: X
    }

    def awaitWalletEvents[X](wallet: CNFTWalletSpec[Result])(f: (CNFTWalletSpec[Result], WalletEvents) => Option[X]): EventsWaiter[X] = {
        def height() = wallet.chain.getLatestBlockNumber.orFail("Unable to get block number")

        var lastSeenBlock = height()
        new EventsWaiter[X] {
            override def get: X = {
                var result: Option[X] = None
                logger.info(s"Start polling blocks from ($lastSeenBlock) ...")
                while (true) {
                    while (lastSeenBlock <= height()) {
                        val events = wallet.events(BigInteger.valueOf(lastSeenBlock), skipSignaturesCheck = false).orFail(s"Failed to parse block $lastSeenBlock")
                        result = f(wallet, events)
                        logger.info(s"Processed block ($lastSeenBlock) [event found: ${result.nonEmpty}]")
                        if (result.nonEmpty) return result.get
                        lastSeenBlock += 1
                    }
                    logger.info(s"Awaiting next block ...")
                    Thread.sleep(1000L)
                }
                result.get
            }
        }
    }

    def expectOne[T](values: Collection[T], msg: String): Either[String, T] =
        values match {
            case Array(t) => Right(t)
            case _ => Left(msg)
        }


    def mkEndorsement(endorsements: Collection[SignedEndorsement]): String = endorsements.zipWithIndex.map { case (endorsement, index) =>
        s"""
           |\t(${index + 1})Endorsement:
           |\t\tRegulatorId: ${endorsement.endorsement.regulatorId}
           |\t\tCertificate: ${new String(endorsement.endorsement.data, StandardCharsets.UTF_8)}
           |""".stripMargin
    }.mkString("\n")

    def mkPublicEndorsement(endorsements: Collection[SignedPublicEndorsement]): String = endorsements.zipWithIndex.map { case (endorsement, index) =>
        s"""
           |\t(${index + 1})Endorsement:
           |\t\tEndorser id: ${endorsement.endorsement.endorserId}
           |\t\tData: ${new String(endorsement.endorsement.data, StandardCharsets.UTF_8)}
           |""".stripMargin
    }.mkString("\n")

    def mkRegulationString(regulation: SmartContractRegulation): String =
        s"""
           |Address: ${regulation.id}
           |Approves: ${mkApproves(regulation.approves)}
           |""".stripMargin

    def mkRestrictionsString(restriction: Collection[Restriction], p: Int = 1): String =
        if (restriction.isEmpty) "[]"
        else restriction.map(r =>
            s"${pad(p + 1)}${r.regulatorId} : ${r.restrictionId}"
        ).mkString("[\n", "\n", s"\n${pad(p + 1)}]")


    def mkApproves(approves: Collection[Approve]): String = approves.map { approve =>
        s"${approve.regulatorId}: ${approve.approved} - " + s"${if (approve.reason.nonEmpty) approve.reason else "No reason"}"
    }.mkString("[", ", ", "]")


    def mkAddress(addresses: Collection[AddressInfo]): String = addresses.zipWithIndex.map { case (address, index) =>
        s"""| $index) AddressInfo:
            | Status: ${address.status}
            | Sent to: ${address.sentTo}
            | Sent from: ${address.sentFrom}""".stripMargin
    }.mkString("\n")

    def mkTokenTypesString(tokenTypes: Collection[TokenType]): String =
        (for {
            (tokenType, i) <- tokenTypes.zipWithIndex
        } yield
            s"""
               | ${i + 1}) TokenType:
               | - typeId: $tokenType
               | - issuerId: ${tokenType.issuerId}
               | - dna:
               |   - change: ${tokenType.dna.change.mkString}
               | - meta:
               |   - description: ${tokenType.meta.description.mkString}
               |""".stripMargin).mkString("\n")

    def mkTokensString(walletTokens: Collection[WalletToken], p: Int = 0): String =
        walletTokens.zipWithIndex.map { case (wt, index) =>
            s"""| ${pad(p)}(${index + 1}) Token:
                | ${pad(p + 1)}- Id: ${wt.id}
                | ${pad(p + 1)}- Content: ${wt.content.mkString("{ ", ", ", " }")}
                | ${pad(p + 1)}- Restrictions:  ${mkRestrictionsString(wt.restrictions, p + 1)}
                | ${pad(p + 1)}- Operations:  ${wt.operations.mkString("[ ", ", ", " ]")}
                |""".stripMargin
        }.mkString

    def pad(i: Int): String = "\t".repeat(i)

    def mkProfilesString(profiles: Collection[Profile]): String =
        profiles
            .zipWithIndex
            .map { case (Profile(id, name, description, _, _, _), index) =>
                s"""
                   | ${index + 1}) Profile:
                   | - id: $id
                   | - name: $name
                   | - description: $description
                   |""".stripMargin
            }
            .mkString("\n")

    def mkDataFeedString(dataFeeds: Collection[DataFeed]): String = dataFeeds.zipWithIndex.map { case (dataFeed, index) =>
        s"""
           | ${index + 1}) Data Feed:
           | FeedId: ${dataFeed.id}
           | Descriptions:\n" ${mkDescriptions(dataFeed.description)}
           | Fields:\n" ++ ${mkFields(dataFeed.fields)}
           |""".stripMargin
    }.mkString("\n")


    def mkSmartContractTemplateString(scTypes: Collection[SmartContractTemplate]): String = scTypes.zipWithIndex.map { case (smartContractTemplate, index) =>
        s"""
           | ${index + 1}) Smart contract type Id: ${smartContractTemplate.id}
           | TODO
            """.stripMargin
    }.mkString("\n")

    def mkSmartContractString(smartContractsList: Collection[SmartContract]): String = smartContractsList.zipWithIndex.map { case (smartContract, index) =>
        s"""
           | ${index + 1}) Smart contract Id: ${smartContract.id}
            """.stripMargin
    }.mkString("\n")


    private def mkDescriptions(descriptions: Collection[DescriptionField]): String = descriptions.map(desc => s" - ${desc.name}(${desc.typeId}): ${desc.value}").mkString("\n")

    private def mkFields(fields: Collection[FieldMeta]): String = fields.map(field => s" - ${field.id}(${field.typeId})").mkString("\n")

    def toB64(data: Array[Byte]): String = new String(Base64.getEncoder.encode(data), StandardCharsets.UTF_8)

    def try2EitherWithLogging[T](obj: => T): Either[String, T] = {
        Try(obj) match {
            case Success(something) => Right(something)
            case Failure(err) =>
                val msg = err.getMessage
                logger.error(msg, err)
                Left(msg)
        }
    }

    private def extractRegulatorApproval(approve: RegulatorApproval, wallet: CNFTWalletSpec[Result]): RegulatorApproval = {
        for {
            reasonDecrypted <- if (approve.reason.nonEmpty)
                wallet.decryptText(approve.reason)
            else Result(approve.reason)
            noticeDecrypted <- if (approve.notice.nonEmpty)
                wallet.decryptText(approve.notice)
            else Result(approve.notice)
        } yield
            approve.copy(
                reason = reasonDecrypted,
                notice = noticeDecrypted)
    }.getOrElse(approve)


    def reprocessOperationsDetails(details: OperationData, wallet: CNFTWalletSpec[Result]): String = {
        details match {
            case TransferProposal(operationId, timestamp, from, tokenType, content, extraData, endorsements) =>
                s"Transfer Proposal:\n operationID: $operationId | timestamp: $timestamp\n from: $from\n tokenType: $tokenType | content: $content\n extraData: $extraData\n endorsments: $endorsements"
            case TokenRequest(operationId, timestamp, from, tokenType, content, address, extraData, endorsements) =>
                s"Token Request:\n operationID: $operationId | timestamp: $timestamp\n from: $from\n tokenType: $tokenType content: | $content\n address: $address\n extraData: $extraData\n endorsments: $endorsements "
            case IssueTokenRequest(issue, actors) =>
                val tokens = issue.tokens.map { token => s"tokenId: ${token.tokenId} | owner: ${token.owner} | content: ${token.body}" }.mkString("\n")
                s"Issue Token Request:\n " +
                    s"issue: operationId: ${issue.operationId} timestamp: ${issue.timestamp}\n" +
                    tokens +
                    s"\n| actors: $actors"
            case PendingIssue(issueRequest, approvals, timestamp) =>
                s"Pending Issue:\n timestamp: $timestamp \n issueRequest: $issueRequest\n" +
                    s"approvals: ${
                        approvals.map { apEncrypted =>
                            val ap = extractRegulatorApproval(apEncrypted, wallet)
                            s"regulator: ${ap.regulatorId} + reason: ${ap.reason} + notice: ${ap.notice}"
                        }.mkString("\n")
                    }"
            case PendingDeal(deal, approvals) =>
                s"Pending Deal:\n deal: $deal\n" +
                    s"approvals: ${
                        approvals.map { apEncrypted =>
                            val ap = extractRegulatorApproval(apEncrypted, wallet)
                            s"regulator: ${ap.regulatorId} + reason: ${ap.reason} + notice: ${ap.notice}"
                        }.mkString("\n")
                    }"
            case BurnResponse(request, tokens) =>
                s"Burn Response:\n request: $request\n tokens: $tokens"
            case PendingBurn(burnRequest, owners, approvals) =>
                s"Pending Burn:\n burnRequest: $burnRequest\n" +
                    s"approvals: ${
                        approvals.map { apEncrypted =>
                            val ap = extractRegulatorApproval(apEncrypted, wallet)
                            s"regulator: ${ap.regulatorId} + reason: ${ap.reason} + notice: ${ap.notice}"
                        }.mkString("\n")
                    }" +
                    s"owners: ${owners.mkString(" ")}"
            case DealRequest(deal, ownerSignatures, recipientSignatures, actors) =>
                s"Deal:\n operationId: $deal.operationId | timestamp: $deal.timestamp\n dealId: $deal.dealId\n legs: $deal.legs\n extra: $deal.extra"
            case RegulatorTransferRequest(deal, signature) =>
                s"Deal:\n operationId: $deal.operationId | timestamp: $deal.timestamp\n dealId: $deal.dealId\n legs: $deal.legs\n extra: $deal.extra"
            case BurnRequest(operationId, timestamp, tokens, extra) =>
                s"Burn Request: $operationId: $operationId | timestamp: $timestamp\n tokens: $tokens\n extra: $extra"
            case DealRejectedBySmartContract(dealRequest, reason) =>
                s"reason: $reason\nDeal:\n operationId: ${dealRequest.deal.operationId} | timestamp: ${dealRequest.deal.timestamp}\n dealId: ${dealRequest.deal.dealId}\n legs: ${dealRequest.deal.legs}\n extra: ${dealRequest.deal.extra}"
        }
    }

    def printOperations(wallet: CNFTWalletSpec[Result]): Unit = {
        val walletIdentity = wallet.getIdentity.orFail(s"Failed to get wallet identity")
        logger.info("")
        logger.info(s"Operations for $walletIdentity")

        logger.info(s"_______")
        wallet.listOperations.orFail(s"Failed to get listOperations")
            .foreach { op =>
                logger.info(s"id: ${op.operationId}")
                logger.info(s"history:")
                op.history.foreach(h =>
                    logger.info(s"opstate: ${h.state}, timestamp: ${h.timestamp}, txId: ${h.txId}")
                )
            }


        wallet.listOperations.orFail(s"Failed to get listOperations")
            .foreach { operation =>
                logger.info(s"\n\t OpID: ${operation.operationId}\n" +
                    operation.history.map { operationDetail =>
                        val details = wallet.getOperationDetails(operationDetail).orFail(s"Failed to getOperationDetails")
                        s"\n\t\t ${operationDetail.state}\n\t\t ${reprocessOperationsDetails(details, wallet)}\n"
                    }.mkString("\n") +
                    "========================================================================="
                )
            }
        ()
    }

}
