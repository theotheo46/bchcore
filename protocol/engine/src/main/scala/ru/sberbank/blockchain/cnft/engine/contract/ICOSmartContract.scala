package ru.sberbank.blockchain.cnft.engine.contract

import ru.sberbank.blockchain.cnft.common.types._
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.engine.contract
import ru.sberbank.blockchain.cnft.errors.{ErrorEncoder, ICOError}
import ru.sberbank.blockchain.cnft.model.{AcceptedToken, DataFeedValue, Deal, DealLeg, DealRequest, FeedType, FieldMeta, FieldType, IssueToken, IssueTokenRequest, IssueTokens, OwnerType, RelatedDealReference, RequestActor, RequestActorType, RequiredDealFields, TokenContent, TokenId, TokenOwner}

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time.{Instant, OffsetDateTime}
import java.util.Base64

/**
 * @author Alexey Polubelov
 */
object ICOSmartContract extends SmartContractDSL with ISmartContract with LoggingSupport {
    override protected val templateId: String = "ICO"

    // Attributes:
    private val investmentTokenType = attribute.Text("investmentTokenType")
    private val issuerAddress = attribute.Text("issuerAddress")
    private val hardcapSum = attribute.Numeric("hardcapSum")
    private val softcapSum = attribute.Numeric("softcapSum")
    private val investmentCoefficient = attribute.Numeric("investmentCoefficient")
    private val subscriptionStartDate = attribute.Date("subscriptionStartDate")
    private val subscriptionEndDate = attribute.Date("subscriptionEndDate")
    private val dfaAllocationDate = attribute.Date("dfaAllocationDate")

    // ICO State constants:
    private val AwaitingInvestmentDate = "AwaitingInvestmentDate"
    private val InvestmentPhase = "InvestmentPhase"
    private val AwaitingReleaseDate = "AwaitingReleaseDate"
    private val TokensReleased = "TokensReleased"
    private val InvestmentsReturned = "InvestmentsReturned"

    // State
    private val icoPhase = field.Text("icoPhase", AwaitingInvestmentDate)
    private val totalPurchased = field.Numeric("totalPurchased", 0L)

    override protected val feedsRequire: Collection[FeedType] =
        Collection(
            FeedType(
                Collection(
                    FieldMeta("timestamp", FieldType.Text)
                )
            )
        )

    override protected def requiredDealFields: Collection[RequiredDealFields] =
        Collection(
            RequiredDealFields(
                "investmentTokenType",
                Collection.empty[FieldMeta]
            )
        )

    //
    override def init(implicit context: SmartContractOperationContext): Result[Unit] =
        for {

            _ <- Result.expect(investmentCoefficient.get > 0L, s"Unsupported investmentCoefficient (${investmentCoefficient.get}) - must be greater then 0")

            _ <- Result.expect(context.contract.feeds.length == 1, s"Only 1 feed expected - the timestamp feed")

            _ <- Result.expect((softcapSum: BigInteger) >= "0".toNumeric, s"softcapSum should be positive")

            _ <- Result.expect((hardcapSum: BigInteger) >= softcapSum, s"hardcapSum should be no less than softcapSum")

            _ <- Result.expect((subscriptionStartDate: Instant) <= subscriptionEndDate, s"subscription start date shall be before subscription end date")

            addressIssuer <- Result {
                Base64.getDecoder.decode(issuerAddress)
            }
            ownerAddressIssuer <- Result {
                TokenOwner.parseFrom(addressIssuer)
            }

            _ <- Result {
                logger.debug(s"tokenOwner type: ${ownerAddressIssuer.ownerType} address: ${ownerAddressIssuer.address.mkString(" ")}")
            }

            address = context.contract.id
            now = getCurrentTimestamp

            _ <- Result {
                if (now >= subscriptionEndDate) {
                    logger.debug(s"SC init [$address] : Subscription end date passed")
                    icoPhase := InvestmentsReturned
                } else if (now >= subscriptionStartDate) {
                    logger.debug(s"SC init [$address] : Subscription start date passed")
                    icoPhase := InvestmentPhase
                }
            }
        } yield ()


    override def acceptTransfer(
        context: SmartContractOperationContext,
        tokens: Collection[AcceptedToken]
    ): Result[SmartContractResult] = Result {
        implicit val cx: SmartContractOperationContext = context
        val currentPhase: String = icoPhase
        logger.info(s"SC Accept transfer [${context.contract.id}] phase ($currentPhase)")

        if (InvestmentPhase != currentPhase)
            FailErr(ICOError.InvestmentPhase, currentPhase)

        val depositAmount =
            tokens.map { token =>
                val tokenId = token.id

                TokenId
                    .from(tokenId)
                    .filterOrElse(
                        _.typeId == (investmentTokenType: String),
                        s"Invalid invested token type $tokenId"
                    )
                    .getOrElse(FailErr(ICOError.WrongType))

                val value = context
                    .getTokenContent(tokenId)
                    .getOrElse(FailErr(ICOError.NoToken, tokenId))
                    .fields.head.toNumeric // TODO make it safe

                if (value % investmentCoefficient !== 0L)
                    FailErr(ICOError.Coefficient, investmentCoefficient.get.toString)
                value

            }.foldLeft(0L: BigInteger)(_ + _)

        val newTotal = depositAmount + totalPurchased

        if (newTotal > hardcapSum) {
            val msg = "Hard capacity is over exceeded  - impossible to invest"
            logger.debug(msg)
            FailErr(ICOError.HardCap)

        } else if (newTotal < hardcapSum) {
            logger.debug(s"Under hardcap value - investing $depositAmount tokens")
            totalPurchased := newTotal

            // no effect, only state is updated
            NoEffect

        } else {
            logger.debug(s"Hard capacity is reached")

            totalPurchased := newTotal

            if (getCurrentTimestamp < dfaAllocationDate) {
                logger.debug("Release date not reached yet, will wait")
                icoPhase := AwaitingReleaseDate
                NoEffect

            } else {
                logger.debug("Release date already came, releasing tokens")
                releaseTokens()
            }
        }
    }

    private def getCurrentTimestamp(implicit context: SmartContractOperationContext): Instant = {
        val feed = context.contract.feeds.head
        val value = context.getFeedValue(feed)
            .getOrElse(FailErr(ICOError.DFValue))
        OffsetDateTime.parse(value.content.head).toInstant
    }


    override def processDataFeed(
        context: SmartContractOperationContext,
        dataFeedValue: DataFeedValue
    ): Result[SmartContractResult] = Result {
        implicit val tx: SmartContractOperationContext = context
        val currentPhase: String = icoPhase
        logger.info(s"SC process data feed [${context.contract.id}] phase ($currentPhase)")
        val feedDate: Instant = OffsetDateTime.parse(dataFeedValue.content.head).toInstant

        if (AwaitingInvestmentDate == currentPhase) {
            if (feedDate >= subscriptionStartDate) { // switch to investment phase
                logger.debug(s"SC subscription started [${context.contract.id}]")
                icoPhase := InvestmentPhase
            }
            NoEffect

        } else if (InvestmentPhase == currentPhase) {
            if (feedDate < subscriptionEndDate) {
                logger.debug(s"SC awaiting subscription end date [${context.contract.id}]")
                // do nothing, we still in investment phase ...
                NoEffect

            } else { // investment phase done, time to decide:
                if (totalPurchased.get < softcapSum) { // didn't got soft cap, return investments:
                    logger.debug(s"SC transferring tokens back [${context.contract.id}] - soft capacity not reached")
                    icoPhase := InvestmentsReturned
                    returnTokens()

                } else { // we have reached soft cap:
                    // shell we wait for release date?
                    if (feedDate < dfaAllocationDate) { // yes
                        logger.debug(s"SC got capacity [${context.contract.id}] - will wait for allocation date")
                        icoPhase := AwaitingReleaseDate
                        NoEffect

                    } else { // we are ready to release right now
                        logger.debug(s"SC releasing tokens [${context.contract.id}]")
                        releaseTokens()

                    }
                }
            }
        } else if (AwaitingReleaseDate == currentPhase) {
            if (feedDate < dfaAllocationDate) { // still waiting for release date ...
                NoEffect

            } else { // we are ready to release tokens
                releaseTokens()

            }

        } else { // do nothing in case we in some other phase
            NoEffect
        }
    }

    private def returnTokens()(implicit context: SmartContractOperationContext) = {
        logger.info(s"SC return tokens [${context.contract.id}]")
        val deals = context.dealsAccepted
        val contractIdBytes = context.contract.id.getBytes(StandardCharsets.UTF_8)
        val ContractAddress =
            TokenOwner(
                ownerType = OwnerType.SmartContractId,
                address = contractIdBytes
            )

        val result =
            contract.SmartContractResult(
                emission = Collection.empty,
                transfer = deals.zipWithIndex.map { case (acceptedDeal, dealIndex) =>
                    DealRequest(
                        Deal(
                            operationId = context.nextUniqueId,
                            timestamp = context.timestamp,
                            dealId = acceptedDeal.deal.deal.dealId,
                            legs = acceptedDeal.tokens.map { token =>
                                DealLeg(
                                    token.id,
                                    token.from,
                                    ContractAddress,
                                    RelatedDealReference(dealIndex, token.leg),
                                    Collection.empty
                                )
                            },
                            extra = Bytes.empty
                        ),
                        // TODO: think about - do we need something here? :
                        ownerSignatures = Collection.empty,
                        recipientSignatures = Collection.empty,
                        actors =
                            Collection(
                                RequestActor(
                                    RequestActorType.SmartContract,
                                    context.contract.id.getBytes(StandardCharsets.UTF_8)
                                )
                            )
                    )
                },
                change = Collection.empty,
                burn = Collection.empty
            )
        context.completeContract()
        result
    }

    private def releaseTokens()(implicit context: SmartContractOperationContext): SmartContractResult = {
        logger.info(s"SC release tokens [${context.contract.id}]")
        val typeId = context.contract.id
        val contractIdBytes = context.contract.id.getBytes(StandardCharsets.UTF_8)
        val ContractAddress =
            TokenOwner(
                ownerType = OwnerType.SmartContractId,
                address = contractIdBytes
            )

        val deals = context.dealsAccepted ++ context.currentDeal

        icoPhase := TokensReleased
        val result =
            contract.SmartContractResult(
                emission =
                    deals.zipWithIndex.map { case (deal, dealIndex) =>
                        IssueTokenRequest(
                            IssueTokens(
                                operationId = context.nextUniqueId,
                                timestamp = context.timestamp,
                                tokens = deal.tokens.map { token =>
                                    val depositAmount =
                                        context
                                            .getTokenContent(token.id)
                                            .getOrElse(FailErr(ICOError.NoToken,token.id))
                                            .fields.head.toNumeric / investmentCoefficient

                                    TokenId.encode(typeId, context.nextUniqueId).map { tokenId =>
                                        IssueToken(
                                            tokenId = tokenId,
                                            owner = token.from,
                                            body = TokenContent(Collection(depositAmount.toString)),
                                            relatedDealRef = RelatedDealReference(dealIndex, token.leg),
                                            extra = Bytes.empty
                                        )

                                    }.getOrElse(FailErr(ICOError.CreateId))
                                }
                            ),
                            Collection(
                                RequestActor(
                                    RequestActorType.SmartContract,
                                    contractIdBytes
                                )
                            )
                        )
                    },
                transfer =
                    deals.zipWithIndex.map { case (deal, dealIndex) =>
                        DealRequest(
                            Deal(
                                operationId = context.nextUniqueId,
                                timestamp = context.timestamp,
                                dealId = context.nextUniqueId,
                                legs = deal.tokens.map { token =>
                                    DealLeg(
                                        token.id,
                                        TokenOwner.parseFrom(Base64.getDecoder.decode(issuerAddress)),
                                        ContractAddress,
                                        RelatedDealReference(dealIndex, token.leg),
                                        Collection.empty
                                    )
                                },
                                extra = Bytes.empty
                            ),
                            // TODO: think about - do we need something here? :
                            ownerSignatures = Collection.empty,
                            recipientSignatures = Collection.empty,
                            actors =
                                Collection(
                                    RequestActor(
                                        RequestActorType.SmartContract,
                                        contractIdBytes
                                    )
                                )
                        )
                    },
                change = Collection.empty,
                burn = Collection.empty
            )
        context.completeContract()
        result
    }

    private def NoEffect =
        contract.SmartContractResult(
            Collection.empty,
            Collection.empty,
            Collection.empty,
            Collection.empty,
        )

    private def FailErr(err: String, data: String*) = {
        throw new Exception(ErrorEncoder.encode(err, data))
    }

}
