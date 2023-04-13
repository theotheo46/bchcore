package ru.sberbank.blockchain.cnft.engine.contract

import ru.sberbank.blockchain.cnft.RelatedDealReferenceEmpty
import ru.sberbank.blockchain.cnft.common.types.{Bytes, Collection, Result, ResultOps, collectionFromIterable}
import ru.sberbank.blockchain.cnft.commons.ROps.IterableR_Ops
import ru.sberbank.blockchain.cnft.commons.{LoggingSupport, collectionFromSequence}
import ru.sberbank.blockchain.cnft.engine.contract
import ru.sberbank.blockchain.cnft.errors.{ErrorEncoder, IndexTradeRedeemError}
import ru.sberbank.blockchain.cnft.model.{AcceptedToken, BurnRequest, DataFeedValue, Deal, DealLeg, DealRequest, FeedType, FieldMeta, FieldType, OwnerType, RelatedDealReference, RequestActor, RequestActorType, RequiredDealFields, TokenId, TokenOwner}
import upickle.default

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time._
import java.time.format.DateTimeFormatter
import java.util.Base64
import scala.math.BigInt.javaBigInteger2bigInt

/**
 * @author Alexey Polubelov
 */
object IndexTradeRedeemSmartContract extends SmartContractDSL with ISmartContract with LoggingSupport {
    override protected val templateId: String = "IndexTradeRedeem"

    // Attributes:
    private val issuerAddress = attribute.Text("issuerAddress")
    private val redeemedTokenType = attribute.Text("redeemedTokenType")
    private val techTokenType = attribute.Text("techTokenType")
    private val subscriptionStartDate = attribute.Date("subscriptionStartDate")
    private val subscriptionEndDate = attribute.Date("subscriptionEndDate")
    private val quoteTTL = attribute.Numeric("quoteTTL")

    // ExRt State constants:
    private val Exchanging = "Open"
    private val OutOfService = "OutOfService"

    // State
    private val serviceStatus = field.Text("serviceStatus", "OutOfService")
    private val totalRedeemed = field.Numeric("totalRedeemed", "0".toNumeric)
    private val redeemBalance = field.Object("redeemBalance", Collection.empty[RedeemBalance])
    private val techTokensBalance = field.Object("techTokensBalance", Collection.empty[String])

    override protected val feedsRequire: Collection[FeedType] =
        Collection(
            FeedType(
                Collection(
                    FieldMeta("Bandprice", FieldType.Text)
                )
            )
        )

    val redeemedTokenFields: Collection[String] = Array[String]("amount", "price", "symbol", "tenorValue", "valueDate", "bandPrice", "tolerancePrice", "maxBandVolume")

    override def init(implicit context: SmartContractOperationContext): Result[Unit] =
        for {
            _ <- logger.debugR(s"initializing contract with issuer address ${issuerAddress.get}")

            addressTechToken <- Result {
                Base64.getDecoder.decode(issuerAddress)
            }

            _ <- Result {
                TokenOwner.parseFrom(addressTechToken)
            }

            _ <- Result.expect(context.contract.feeds.length == 1, s"Only 1 feeds expected - exchange and control")

            _ <- Result.expect((quoteTTL: BigInteger) >= "0".toNumeric, s"quote ttl should be positive")

            _ <- Result.expect((subscriptionEndDate: Instant) > subscriptionStartDate, s"end date shall be later than start date")

            tokenType <- ResultOps.fromOption(context.getTokenType(redeemedTokenType), s"can not get token type for type id ${context.contract.id}")

            _ <- Result.expect(tokenType.meta.fields.map(_.id) sameElements redeemedTokenFields, s"token type has not contain proper metafields")

        } yield ()


    override def acceptTransfer(
        context: SmartContractOperationContext,
        tokens: Collection[AcceptedToken]
    ): Result[SmartContractResult] = Result {
        implicit val cx: SmartContractOperationContext = context
        val currentStatus: String = serviceStatus

        logger.info(s"Accept transfer [${context.contract.id}] phase ($currentStatus)")

        if (!verifyTokensMetaInfo()) {
            FailErr(IndexTradeRedeemError.MetadataWrong)
        }

        if (verifyTechTokens(tokens)) {
            logger.debug(s"received tech tokens")
            techTokensBalance := (techTokensBalance: Collection[String]) ++ tokens.map(_.id)
            logger.debug(s"tech token balance: ${(techTokensBalance: Collection[String]).mkString("Array(", ", ", ")")}")
            processTechTokensTransfer()
        } else {
            currentStatus match {
                case Exchanging =>
                    if (verifyExchangeableTokens(tokens)) {
                        logger.debug(s"token is exchangeable, processing exchange...")
                        processTokensExchange()
                    } else {
                        logger.debug(s"tokens types can not be accepted for exchange, returning")
                        FailErr(IndexTradeRedeemError.TtNotExc)
                    }
                case OutOfService =>
                    logger.debug(s"contract out of service")
                    FailErr(IndexTradeRedeemError.OoS)
                case _ =>
                    NoEffect
            }
        }
    }

    private def isTokenOfType(tokens: Collection[AcceptedToken], checkType: RO[String])(implicit context: SmartContractOperationContext): Boolean = {
        val tokenTypeIds = tokens.toSeq.mapR(t => TokenId.from(t.id).map(_.typeId)).getOrElse(FailErr(IndexTradeRedeemError.TtGET))
        logger.debug(s"type : ${checkType: String}  tokens types is: ")
        tokenTypeIds.foreach { t =>
            logger.debug(s"type: $t")
        }
        val valid = tokens.toSeq.filterR(t => TokenId.from(t.id).map(_.typeId != (checkType: String))).map(_.isEmpty).getOrElse(false)
        logger.debug(s"type validation result: $valid")
        valid
    }

    private def verifyTokensMetaInfo()(implicit context: SmartContractOperationContext): Boolean = {

        val deal = context.currentDeal.getOrElse(FailErr(IndexTradeRedeemError.NoDeal))

        deal.deal.deal.legs.forall { leg =>
            val tokenTypeId = TokenId.from(leg.tokenId).map(_.typeId)
                .getOrElse(FailErr(IndexTradeRedeemError.TtDecode))
            if ((redeemedTokenType: String) == tokenTypeId) {
                leg.fields.length == requiredRedeemDealFields.length
            } else {
                true
            }
        }
    }

    private def verifyExchangeableTokens(tokens: Collection[AcceptedToken])(implicit context: SmartContractOperationContext): Boolean = {
        isTokenOfType(tokens, redeemedTokenType)
    }

    private def verifyTechTokens(tokens: Collection[AcceptedToken])(implicit context: SmartContractOperationContext): Boolean = {
        val addressTechToken = Base64.getDecoder.decode(issuerAddress)
        val ownerTechToken = TokenOwner.parseFrom(addressTechToken)
        val ownerOK = tokens.forall(token =>
            (token.from.ownerType == ownerTechToken.ownerType) && (token.from.address sameElements ownerTechToken.address)
        )
        ownerOK && isTokenOfType(tokens, techTokenType)
    }

    // Data feed processing section
    override def processDataFeed(
        context: SmartContractOperationContext,
        dataFeedValue: DataFeedValue
    ): Result[SmartContractResult] = Result {
        implicit val tx: SmartContractOperationContext = context

        val quoteData = processDataFeedValue(dataFeedValue)
            .getOrElse(FailErr(IndexTradeRedeemError.DfVl))

        setServiceStatus(quoteData.quoteCondition)

        NoEffect
    }

    private def setServiceStatus(quoteCondition: String)(implicit context: SmartContractOperationContext): Unit = {
        val currentStatus: String = serviceStatus

        quoteCondition
        match {
            case "A" =>
                serviceStatus := Exchanging
            case "B" =>
                serviceStatus := OutOfService
            case _ =>
                FailErr(IndexTradeRedeemError.FdCnd)
        }
        logger.debug(s"service status  set to ${serviceStatus: String}")
        logger.debug(s"process data feed [${context.contract.id}] phase ($currentStatus), serviceStatus: ${serviceStatus: String}")
    }

    private def processDataFeedValue(dataFeedValue: DataFeedValue)(implicit context: SmartContractOperationContext): Result[Quote] =
        for {
            feedFields <- context.getDataFeed(dataFeedValue.id).map(_.fields)
            feedData <- Result {
                feedFields.zipWithIndex.map { case (f, index) =>
                    (f.id.toLowerCase(), f.typeId, dataFeedValue.content(index))
                }
            }
            _ <- Result {
                logger.debug(s"received datafeed values:")
                feedData.foreach { d =>
                    logger.debug(s"field: ${d._1} \t\t (${d._2}): ${d._3}")
                }
            }
            quoteData <- processFeedValue(feedData)
            _ <- Result {
                logger.debug(s"quote data: ${quoteData.toString}")
                val ts = LocalDateTime.parse(quoteData.quotesTimeStamp, DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"))
                val vd = LocalDate.parse(quoteData.valueDate, DateTimeFormatter.ofPattern("yyyyMMdd"))
                logger.debug(s"parsed timestamp: $ts , value date ${vd}")
            }
        } yield quoteData


    private def processFeedValue(feedValue: Collection[(String, String, String)]): Result[Quote] =
        for {
            quoteEntryId <- ResultOps.fromOption(feedValue.find(_._1 == "quoteentryid"), s"can not get QuoteEntryID")
                .map(_._3)
            quotesTimeStamp <- ResultOps.fromOption(feedValue.find(_._1 == "quotestimestamp"), s"can not get Quotestimestamp")
                .map(_._3.filterNot(_.isWhitespace))
            symbol <- ResultOps.fromOption(feedValue.find(_._1 == "symbol"), s"can not get Symbol")
                .map(_._3)
            tenorValue <- ResultOps.fromOption(feedValue.find(_._1 == "tenorvalue"), s"can not get Tenorvalue")
                .map(_._3)
            valueDate <- ResultOps.fromOption(feedValue.find(_._1 == "valuedate"), s"can not get Valuedate")
                .map(_._3.filterNot(_.isWhitespace))
            bandPrice <- ResultOps.fromOption(feedValue.find(_._1 == "bandprice"), s"can not get Bandprice")
                .map(_._3)
            tolerancePrice <- ResultOps.fromOption(feedValue.find(_._1 == "toleranceprice"), s"can not get Toleranceprice")
                .map(_._3)
            maxBandVolume <- ResultOps.fromOption(feedValue.find(_._1 == "maxbandvolume"), s"can not get Maxandvolume")
                .map(_._3)
            quoteCondition <- ResultOps.fromOption(feedValue.find(_._1 == "quotecondition"), s"can not get QuoteCondition")
                .map(_._3)

            quote <- Result {
                Quote(
                    quoteEntryId = quoteEntryId,
                    quotesTimeStamp = quotesTimeStamp,
                    symbol = symbol,
                    tenorValue = tenorValue,
                    valueDate = valueDate,
                    bandPrice = bandPrice,
                    tolerancePrice = tolerancePrice,
                    maxBandVolume = maxBandVolume,
                    quoteCondition = quoteCondition
                )
            }

            bp <- Result(quote.bandPrice.toNumeric)
            _ <- Result.expect(bp > "0".toNumeric, s"band price shall be greater than 0")
            tp <- Result(quote.tolerancePrice.toNumeric)
            _ <- Result.expect(tp > "0".toNumeric, s"tolerance price shall be greater than 0")
            bv <- Result(quote.maxBandVolume.toNumeric)
            _ <- Result.expect(bv >= "0".toNumeric, s"band volume shall be greater than 0")

        } yield quote


    private def processLegRequiredFields(dealValue: Collection[(String, String, String)]): Result[DealFields] =
        for {
            priceStr <- ResultOps.fromOption(dealValue.find(_._1 == "price"), s"can not get price")
                .map(_._3)
            price <- Result(priceStr.toNumeric)
        } yield
            DealFields(
                price = price,
            )


    //@ToDo verify quote selector match to confluence specification
    def quoteSelectorRedeem(clientPrice: BigInteger, bandPrice: BigInteger, tolerancePrice: BigInteger): Result[BigInteger] = Result {
        logger.debug(s"quote selector client price $clientPrice , bandPrice: $bandPrice, tolerancePrice $tolerancePrice")

        clientPrice match {
            case price if price == bandPrice =>
                logger.debug(s"client price match to band price, selecting client price")
                price
            case price if tolerancePrice >= price && price > bandPrice =>
                logger.debug(s"tolerancePrice >= price && price > bandPrice, selecting client price")
                price
            case price if price > tolerancePrice && tolerancePrice > bandPrice =>
                logger.debug(s"price > tolerancePrice && tolerancePrice > bandPrice, rejecting transaction")
                FailErr(IndexTradeRedeemError.PriceNotValid)
            case price if price < bandPrice =>
                logger.debug(s"price < bandPrice , selecting client price")
                bandPrice
            case _ =>
                FailErr(IndexTradeRedeemError.PriceCnd)
        }
    }

    private def processTokensExchange()(implicit context: SmartContractOperationContext): SmartContractResult = {

        val typeId = context.contract.id
        logger.info(s"exchanging tokens [${typeId}]")

        val indexAcceptedDeal = context.dealsAccepted.length

        val feedValue = context.getFeedValue(context.contract.feeds.head)
            .getOrElse(FailErr(IndexTradeRedeemError.DfVl))

        val quote: Quote = processDataFeedValue(feedValue)
            .getOrElse(FailErr(IndexTradeRedeemError.DfVl))
        setServiceStatus(quote.quoteCondition)

        val quoteTimeStamp = LocalDateTime.parse(quote.quotesTimeStamp, DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"))
        val quoteValueDate = LocalDate.parse(quote.valueDate, DateTimeFormatter.ofPattern("yyyyMMdd"))
        logger.debug(s"parsed quote timestamp: $quoteTimeStamp , value date $quoteValueDate")
        val maxQuoteTTL: BigInteger = quoteTTL

        val deal = context.currentDeal
            .getOrElse(FailErr(IndexTradeRedeemError.NoDeal))

        val dealParams = {
            val dealTS = context.timestamp

            val dealTimeStamp = Instant.parse(dealTS)
            val dealTimeStampZone = dealTimeStamp.atZone(ZoneOffset.UTC)
            logger.debug(s"deal timestamp string: $dealTS, parsed: $dealTimeStampZone")
            val diffTimeStampQuote = Duration.between(quoteTimeStamp, dealTimeStampZone).toSeconds
            logger.debug(s"max quote ttl: $maxQuoteTTL , deal vs quote difference: $diffTimeStampQuote")
            if (diffTimeStampQuote > maxQuoteTTL.toLong) {
                logger.debug(s"quote ttl exceeded.")
                FailErr(IndexTradeRedeemError.TtL)
            }
            if (dealTimeStamp < subscriptionStartDate) {
                logger.debug(s"deal $dealTimeStamp can not be early than start date ${subscriptionStartDate: Instant}")
                FailErr(IndexTradeRedeemError.DateStart)
            }
            if (dealTimeStamp > subscriptionEndDate) {
                logger.debug(s"deal $dealTimeStamp can not be later than end date ${subscriptionEndDate: Instant}")
                FailErr(IndexTradeRedeemError.DateEnd)
            }

            val tokensParams =
                deal.deal.deal.legs.map { leg =>
                    val dealFieldsValue =
                        requiredRedeemDealFields.zipWithIndex.map { case (f, index) =>
                            (f.id.toLowerCase(), f.typeId, leg.fields(index))
                        }
                    logger.debug(s"received deal required field values:")
                    dealFieldsValue.foreach { d =>
                        logger.debug(s"field: ${d._1} \t\t (${d._2}): ${d._3}")
                    }
                    val dealFields = processLegRequiredFields(dealFieldsValue)
                        .getOrElse(FailErr(IndexTradeRedeemError.Fields))
                    logger.debug(s"deal fields: $dealFields")
                    (leg.tokenId,
                        context.getTokenValue(leg.tokenId)
                            .getOrElse(FailErr(IndexTradeRedeemError.TokenValue, leg.tokenId))
                        , dealFields)
                }

            if (tokensParams.forall(_._3.price != tokensParams.map(_._3).head.price)) {
                FailErr(IndexTradeRedeemError.DealPriceRequest)
            }

            DealParams(
                deal.deal.deal.operationId,
                tokensParams.map(_._1),
                tokensParams.map(_._2).foldLeft("0".toNumeric)(_.add(_)),
                tokensParams.map(_._3).head.price
            )
        }


        val tradeResultForDeal = {
            logger.debug(s"deal parameters for operation Id ${dealParams.operationId} with value ${dealParams.sum}")
            logger.debug(s"client price request: ${dealParams.priceRequest}")
            logger.debug(s"current quote band price ${quote.bandPrice} tolerance price ${quote.tolerancePrice}")
            val selectedQuote = quoteSelectorRedeem(dealParams.priceRequest, quote.bandPrice.toNumeric, quote.tolerancePrice.toNumeric)
                .getOrElse(FailErr(IndexTradeRedeemError.QuoteSelect))
            logger.debug(s"selected quote $selectedQuote")
            TradeResult(
                dealParams.operationId,
                dealParams.tokenIds,
                dealParams.sum,
                selectedQuote)
        }

        val totalTradeRequest = tradeResultForDeal.dealValue // tradeResultForToken.map(_.tokenValue).foldLeft("0".toNumeric)(_.add(_))
        logger.debug(s"total requested trade volume ${totalTradeRequest: BigInteger}")

        val newTotalRedeemed = (totalRedeemed: BigInteger) + totalTradeRequest

        totalRedeemed := newTotalRedeemed

        logger.debug(s"trade to execute:")
        logger.debug(s"trade: $tradeResultForDeal")


        //            RedeemResult(
        //                tradeResultForDeal.operationId,
        //                tradeResultForDeal.tokenIds,
        //                Collection(
        //                    tradeResultForDeal.dealValue.toString,
        //                    tradeResultForDeal.selectedQuote.toString,
        //                    quote.symbol,
        //                    quote.tenorValue,
        //                    quote.valueDate,
        //                    quote.bandPrice,
        //                    quote.tolerancePrice,
        //                    quote.maxBandVolume
        //                )
        //            ),
        val redeem =
        RedeemBalance(
            operationId = tradeResultForDeal.operationId,
            tokenIds = tradeResultForDeal.tokenIds,
            dealOwner = deal.deal.deal.legs.head.previousOwner,
            relatedDealReference =
                RelatedDealReference(
                    indexAcceptedDeal,
                    -1
                ),
            amount = tradeResultForDeal.dealValue.toString,
            quote = tradeResultForDeal.selectedQuote.toString,
            sum = (tradeResultForDeal.dealValue * tradeResultForDeal.selectedQuote).toString,
            quote.symbol,
            quote.tenorValue
        )

        redeemBalance := (redeemBalance: Collection[RedeemBalance]) :+ redeem

        val result =
            contract.SmartContractResult(
                emission = Collection.empty,
                transfer = Collection.empty,
                burn = Collection.empty,
                change = Collection.empty,
            )
        result
    }

    private def processTechTokensTransfer()(implicit context: SmartContractOperationContext): SmartContractResult = {

        val contractIdBytes = context.contract.id.getBytes(StandardCharsets.UTF_8)
        val currentTechTokenBalance = (techTokensBalance: Collection[String])
            .map { id =>
                TokenValue(id,
                    context.getTokenValue(id).getOrElse(FailErr(IndexTradeRedeemError.TokenValue))
                )
            }
        val availableBalance = currentTechTokenBalance.map(_.value).foldLeft("0".toNumeric)(_.add(_))
        logger.debug(s"tech balance: ${availableBalance.toString}")

        val toRedeem = (redeemBalance: Collection[RedeemBalance])

        val valuesToRedeem = toRedeem.map(_.sum.toNumeric) //
        logger.debug(s"values to redeem ${valuesToRedeem.mkString(",")}")
        val totalNeededForRedeem = valuesToRedeem.foldLeft("0".toNumeric)(_.add(_))
        logger.debug(s"total to redeem ${totalNeededForRedeem.toString}")

        val (payResult, leftTokens) = calcPayments(toRedeem.map(r => DealValue(r.operationId, r.sum.toNumeric)), currentTechTokenBalance)

        val techTokensChangeResult = (payResult.flatMap(_.tokens) ++ leftTokens).zipWithIndex.groupBy(_._1.id)
            .map { case (id, a) =>
                if (a.length == 1) {
                    (id,
                        None,
                        a.head._2
                    )
                } else {
                    (id,
                        Option(context.changeTokens(id, collectionFromSequence(a.map(_._1.value)))
                            .getOrElse(FailErr(IndexTradeRedeemError.NoChangeData)),
                        ),
                        a.head._2
                    )
                }
            }.toArray
            .sortBy(_._3)
            .map(v => (v._1, v._2))

        logger.debug(s"token change result:")
        techTokensChangeResult.foreach { r =>
            logger.debug(s"id: ${r._1} change ${r._2}")
            r._2 match {
                case Some(v) =>
                    logger.debug(s"to create: ${v.toCreate.mkString(" ")}")
                case None =>
                    logger.debug(s"original token ${r._1}")
            }
        }

        val techTokensIds = techTokensChangeResult.flatMap { case (id, change) =>
            change.map(_.toCreate.map(v => TokenValue(v.id, v.value.toNumeric)))
                .getOrElse(Collection(TokenValue(id, currentTechTokenBalance.find(_.id == id).getOrElse(FailErr(IndexTradeRedeemError.NoTechToken)).value)))
        }

        logger.debug(s"tech token ids:")
        techTokensIds.foreach { t =>
            logger.debug(s" id: ${t.id}, val: ${t.value}")
        }

        logger.debug(s"to transfer")
        val toTransfer = payResult.flatMap(r => r.tokens.map(a => (a, r.operationId))).zipWithIndex.map { case (v, index) =>
            val techToken = techTokensIds(index)
            logger.debug(s"tech token $techToken")
            if (v._1.value != techToken.value) {
                logger.debug(s"valued needed: ${v._1.value} , value of tech token ${techToken.value}")
                FailErr(IndexTradeRedeemError.Mapping)
            }
            val redeemToken = toRedeem.find(_.operationId == v._2).getOrElse(FailErr(IndexTradeRedeemError.MappingRedeemTech))
            ToTransfer(v._2, redeemToken.relatedDealReference, redeemToken.dealOwner, techToken.id)
        }

        val toChange = collectionFromIterable(
            techTokensChangeResult.flatMap(_._2)
        )

        logger.debug(s"change request:")
        toChange.foreach { c =>
            logger.debug(s"id to change: ${c.toChange}")
            logger.debug(s"ids to create:")
            c.toCreate.foreach { n =>
                logger.debug(s"new token id: ${n.id} with value ${n.value}")
            }
        }

        val leftTokensForBalance = techTokensIds.drop(toTransfer.length)
        logger.debug(s"token left on smart balance:")
        leftTokensForBalance.foreach { t =>
            logger.debug(s"id: ${t.id}, val ${t.value}")
        }


        val contractAddress =
            TokenOwner(
                ownerType = OwnerType.SmartContractId,
                address = contractIdBytes
            )

        val issuerTechTokens = TokenOwner.parseFrom(Base64.getDecoder.decode(issuerAddress))


        val leftTechTokens = leftTokensForBalance.map(_.id)
        val toBurn = (redeemBalance: Collection[RedeemBalance]).filter(r => toTransfer.map(_.operationId).contains(r.operationId))
        val leftOnBalance = (redeemBalance: Collection[RedeemBalance]).filterNot(r => toTransfer.map(_.operationId).contains(r.operationId))
        redeemBalance := leftOnBalance
        val techChangeTransfer =
            if (leftOnBalance.isEmpty && leftTechTokens.nonEmpty) { //
                techTokensBalance := Collection.empty
                Collection(
                    DealRequest(
                        Deal(
                            operationId = context.nextUniqueId,
                            timestamp = context.timestamp,
                            dealId = context.nextUniqueId,
                            legs =
                                collectionFromIterable(
                                    leftTechTokens.map { techToken =>
                                        DealLeg(
                                            techToken,
                                            issuerTechTokens,
                                            contractAddress,
                                            RelatedDealReferenceEmpty,
                                            Collection.empty
                                        )
                                    }
                                ),
                            extra = Bytes.empty
                        ),
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
                )
            } else {
                techTokensBalance := leftTechTokens
                Collection.empty[DealRequest]
            }

        // Extra check of outgoing sum
        if (assertOutgoingPayment(toTransfer, techTokensIds, toRedeem)) FailErr(IndexTradeRedeemError.SumTransferRedeem)

        val transferRedeemAndBurn =
            toTransfer
                .groupBy(_.operationId)
                .map { case (inOperationId, transfer) => //case (redeemId, owner, techId) =>
                    val opId = context.nextUniqueId
                    (
                        inOperationId,
                        (
                            DealRequest(
                                Deal(
                                    operationId = opId,
                                    timestamp = context.timestamp,
                                    dealId = context.nextUniqueId,
                                    legs =
                                        collectionFromIterable(
                                            transfer.map { t =>
                                                DealLeg(
                                                    t.techTokenId,
                                                    t.tokenOwner,
                                                    contractAddress,
                                                    t.relatedDealReference,
                                                    Collection.empty
                                                )
                                            }
                                        ),
                                    extra = Bytes.empty
                                ),
                                ownerSignatures = Collection.empty,
                                recipientSignatures = Collection.empty,
                                actors =
                                    Collection(
                                        RequestActor(
                                            RequestActorType.SmartContract,
                                            context.contract.id.getBytes(StandardCharsets.UTF_8)
                                        )
                                    )
                            ),
                            opId
                        )
                    )
                }

        contract.SmartContractResult(
            emission = Collection.empty,
            transfer =
                collectionFromIterable(
                    transferRedeemAndBurn.values.map(_._1)
                ) ++ techChangeTransfer,
            burn = collectionFromIterable(
                toBurn.map { redeem =>
                    val linkedTransferId = transferRedeemAndBurn
                        .getOrElse(redeem.operationId, FailErr(IndexTradeRedeemError.NoLinkedOpId))
                        ._2.getBytes(StandardCharsets.UTF_8)
                    BurnRequest(
                        operationId = context.nextUniqueId,
                        timestamp = context.timestamp,
                        tokens = redeem.tokenIds,
                        extra = linkedTransferId
                    )
                }
            ),
            change = toChange,
        )
    }


    private def assertOutgoingPayment(toTransfer: Collection[ToTransfer], techTokensIds: Collection[TokenValue], toRedeem: Collection[RedeemBalance]): Boolean = {
        val outgoingTransferSum =
            toTransfer
                .groupBy(_.operationId)
                .map { case (operationId, transfer) =>
                    operationId ->
                        transfer.map { t =>
                            techTokensIds
                                .find(_.id == t.techTokenId)
                                .getOrElse(FailErr(IndexTradeRedeemError.NoTechId))
                                .value
                        }.foldLeft("0".toNumeric)(_.add(_))
                }
        !outgoingTransferSum.forall { case (operationId, outSum) =>
            val redeemValue =
                toRedeem.find(r => r.operationId == operationId)
                    .getOrElse(FailErr(IndexTradeRedeemError.NoRedeemId))
                    .sum
                    .toNumeric
            redeemValue == outSum
        }
    }

    private def calcPayments(redeem: Collection[DealValue], techTokens: Collection[TokenValue]): (Collection[PayResult], Collection[TokenValue]) = {
        var toRedeem = redeem
        var availableTokens = techTokens
        var payResult: Collection[PayResult] = Collection.empty[PayResult]
        var leftTokens = Collection.empty[TokenValue]

        while (availableTokens.nonEmpty && toRedeem.nonEmpty) {
            var sumNeeded = toRedeem.head.value
            var tokenForSum: Collection[TokenValue] = Collection.empty[TokenValue]
            while (sumNeeded > "0".toNumeric && availableTokens.nonEmpty && toRedeem.nonEmpty) {
                val tt = availableTokens.head
                sumNeeded = sumNeeded - tt.value
                availableTokens = availableTokens.tail
                if (sumNeeded < "0".toNumeric) {
                    availableTokens = Collection(TokenValue(tt.id, sumNeeded.abs())) ++ availableTokens
                    tokenForSum = tokenForSum ++ Collection(TokenValue(tt.id, tt.value + sumNeeded))
                } else {
                    tokenForSum = tokenForSum ++ Collection(TokenValue(tt.id, tt.value))
                }
            }
            if (sumNeeded <= "0".toNumeric) {
                payResult = payResult :+ PayResult(toRedeem.head.operationId, tokenForSum)
                toRedeem = toRedeem.tail
            } else {
                leftTokens = tokenForSum
            }
        }

        (payResult, leftTokens ++ availableTokens)
    }

    private def NoEffect =
        contract.SmartContractResult(
            Collection.empty,
            Collection.empty,
            Collection.empty,
            Collection.empty,
        )

    override protected def requiredDealFields: Collection[RequiredDealFields] =
        Collection(
            RequiredDealFields(
                "redeemedTokenType",
                Collection(
                    FieldMeta("price", FieldType.Numeric)
                )
            ),
            RequiredDealFields(
                "techTokenType",
                Collection.empty[FieldMeta]
            )
        )

    private val requiredRedeemDealFields = requiredDealFields.find(_.attributeId == "redeemedTokenType")
        .getOrElse(FailErr(IndexTradeRedeemError.Fields)).fields


    private case class TradeResult(operationId: String, tokenIds: Collection[String], dealValue: BigInteger, selectedQuote: BigInteger)

    private case class DealFields(price: BigInteger)

    private case class Quote(quoteEntryId: String, quotesTimeStamp: String, symbol: String, tenorValue: String, valueDate: String,
        bandPrice: String, tolerancePrice: String, maxBandVolume: String, quoteCondition: String)

    private case class ToTransfer(operationId: String, relatedDealReference: RelatedDealReference, tokenOwner: TokenOwner, techTokenId: String)

    private def FailErr(err: String, data: String*) = {
        throw new Exception(ErrorEncoder.encode(err, data))
    }
}

case class DealParams(operationId: String, tokenIds: Collection[String], sum: BigInteger, priceRequest: BigInteger)

case class TokenValue(id: String, value: BigInteger)

case class PayResult(operationId: String, tokens: Collection[TokenValue])

case class DealValue(operationId: String, value: BigInteger)

case class RedeemBalance(operationId: String, tokenIds: Collection[String], dealOwner: TokenOwner, relatedDealReference: RelatedDealReference, amount: String, quote: String, sum: String, symbol: String, tenorValue: String)

object RedeemBalance {

    import ru.sberbank.blockchain.BytesAsBase64RW

    implicit val RW: default.ReadWriter[RedeemBalance] = upickle.default.macroRW
    implicit val TokenOwnerRW: default.ReadWriter[TokenOwner] = upickle.default.macroRW
    implicit val RelatedDealReferenceRW: default.ReadWriter[RelatedDealReference] = upickle.default.macroRW
}