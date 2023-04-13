package ru.sberbank.blockchain.cnft.engine.contract

import ru.sberbank.blockchain.cnft.common.types.{Bytes, Collection, Result, ResultOps}
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.commons.ROps.IterableR_Ops
import ru.sberbank.blockchain.cnft.engine.contract
import ru.sberbank.blockchain.cnft.errors.{ErrorEncoder, IndexTradeError}
import ru.sberbank.blockchain.cnft.model.{AcceptedToken, DataFeedValue, Deal, DealLeg, DealRequest, FeedType, FieldMeta, FieldType, IssueToken, IssueTokenRequest, IssueTokens, OwnerType, RelatedDealReference, RequestActor, RequestActorType, RequiredDealFields, TokenContent, TokenId, TokenOwner}

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time._
import java.time.format.DateTimeFormatter
import java.util.Base64
import scala.math.BigInt.javaBigInteger2bigInt

/**
 * @author Alexey Polubelov
 */
object IndexTradeSmartContract extends SmartContractDSL with ISmartContract with LoggingSupport {
    override protected val templateId: String = "IndexTrade"

    // Attributes:
    private val issuerAddress = attribute.Text("issuerAddress")
    private val investmentTokenType = attribute.Text("investmentTokenType")
    private val hardcapSum = attribute.Numeric("hardcapSum")
    private val softcapSum = attribute.Numeric("softcapSum")
    private val maxTransactionSum = attribute.Numeric("maxTransactionSum")
    private val minTransactionSum = attribute.Numeric("minTransactionSum")
    private val subscriptionStartDate = attribute.Date("subscriptionStartDate")
    private val subscriptionEndDate = attribute.Date("subscriptionEndDate")
    private val quoteTTL = attribute.Numeric("quoteTTL")

    // ExRt State constants:
    private val Exchanging = "Open"
    private val OutOfService = "OutOfService"

    // State
    private val serviceStatus = field.Text("serviceStatus", "OutOfService")
    private val totalIssued = field.Numeric("totalIssued", "0".toNumeric)

    override protected val feedsRequire: Collection[FeedType] =
        Collection(
            FeedType(
                Collection(
                    FieldMeta("Bandprice", FieldType.Text)
                )
            )
        )

    val issuingTokenFields = Array[String]("amount", "price", "symbol", "tenorValue", "valueDate", "bandPrice", "tolerancePrice", "maxBandVolume")

    override def init(implicit context: SmartContractOperationContext): Result[Unit] =
        for {
            _ <- Result {
                logger.debug(s"initializing contract with issuer address ${issuerAddress.get}")
            }
            _ <- Result.expect(context.contract.feeds.length == 1, s"Only 1 feeds expected - exchange and control")
            addressIssuer <- Result {
                Base64.getDecoder.decode(issuerAddress)
            }
            ownerAddressIssuer <- Result {
                TokenOwner.parseFrom(addressIssuer)
            }
            _ <- Result {
                logger.debug(s"tokenOwner type: ${ownerAddressIssuer.ownerType} address: ${ownerAddressIssuer.address.mkString(" ")}")
            }
            _ <- Result.expect((quoteTTL: BigInteger) >= "0".toNumeric, s"quote ttl should be positive")

            _ <- Result.expect((softcapSum: BigInteger) >= "0".toNumeric, s"softcapSum should be positive")

            _ <- Result.expect((hardcapSum: BigInteger) >= (softcapSum: BigInteger), s"hardcapSum shall be no less than softcap")

            _ <- Result.expect((minTransactionSum: BigInteger) >= "0".toNumeric, s"minimum number of tokens per operation shall be positive")

            _ <- Result.expect((maxTransactionSum: BigInteger) >= (minTransactionSum: BigInteger), s"maximum number of tokens per operation shall be no less than minimum")

            _ <- Result.expect((subscriptionEndDate: Instant) > subscriptionStartDate, s"end date shall be later than start date")

            tokenType <- ResultOps.fromOption(context.getTokenType(context.contract.id), s"can not get token type to type id ${context.contract.id}")

            _ <- Result.expect(tokenType.meta.fields.map(_.id) sameElements issuingTokenFields, s"token type has not contain proper metafields")

        } yield ()


    override def acceptTransfer(
        context: SmartContractOperationContext,
        tokens: Collection[AcceptedToken]
    ): Result[SmartContractResult] = Result {
        implicit val cx: SmartContractOperationContext = context
        val currentStatus: String = serviceStatus

        logger.info(s"Accept transfer [${context.contract.id}] phase ($currentStatus)")

        if (!verifyTokensMetaInfo()) {
            FailErr(IndexTradeError.MetadataWrong)
        }

        currentStatus match {
            case Exchanging =>
                if (verifyExchangableTokens(tokens)) {
                    logger.debug(s"token is exchangeable, processing exchange...")
                    processTokensExchange()
                } else {
                    logger.debug(s"tokens types can not be accepted for exchange, rejecting")
                    FailErr(IndexTradeError.TtNotExc)
                }
            case OutOfService =>
                logger.debug(s"contract out of service")
                FailErr(IndexTradeError.OoS)
            case _ =>
                NoEffect
        }
    }

    private def isTokenOfType(tokens: Collection[AcceptedToken], checkType: RO[String])(implicit context: SmartContractOperationContext): Boolean = {
        val tokenTypeIds = tokens.toSeq.mapR(t => TokenId.from(t.id).map(_.typeId)).getOrElse(FailErr(IndexTradeError.TtGET))
        logger.debug(s"type : ${checkType: String}  tokens types is: ")
        tokenTypeIds.foreach { t =>
            logger.debug(s"type: $t")
        }
        val valid = tokens.toSeq.filterR(t => TokenId.from(t.id).map(_.typeId != (checkType: String))).map(_.isEmpty).getOrElse(false)
        logger.debug(s"type validation result: $valid")
        valid
    }

    private def verifyExchangableTokens(tokens: Collection[AcceptedToken])(implicit context: SmartContractOperationContext): Boolean = {
        isTokenOfType(tokens, investmentTokenType)
    }

    private def verifyTokensMetaInfo()(implicit context: SmartContractOperationContext): Boolean = {

        val deal = context.currentDeal.getOrElse(FailErr(IndexTradeError.NoDeal))

        deal.deal.deal.legs.forall { leg =>
            val tokenTypeId = TokenId.from(leg.tokenId).map(_.typeId)
                .getOrElse(FailErr(IndexTradeError.TtDecode))
            if ((investmentTokenType: String) == tokenTypeId) {
                leg.fields.length == requiredInvestmentDealFields.length
            } else {
                true
            }
        }
    }

    // Data feed processing section
    override def processDataFeed(
        context: SmartContractOperationContext,
        dataFeedValue: DataFeedValue
    ): Result[SmartContractResult] = Result {
        implicit val tx: SmartContractOperationContext = context

        val quoteData = processDataFeedValue(dataFeedValue)
            .getOrElse(FailErr(IndexTradeError.DfVl))

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
                FailErr(IndexTradeError.FdCnd)
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
                logger.debug(s"quote data:${quoteData.toString}")
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
            _ <- Result.expect(bv > "0".toNumeric, s"band volume shall be greater than 0")

        } yield quote


    private def processLegRequiredFields(dealValue: Collection[(String, String, String)]): Result[DealFields] =
        for {
            priceStr <- ResultOps.fromOption(dealValue.find(_._1 == "price"), s"can not get price")
                .map(_._3)
            price <- Result(priceStr.toNumeric)
            volumeStr <- ResultOps.fromOption(dealValue.find(_._1 == "volume"), s"can not get deal required volume")
                .map(_._3)
            volume <- Result(volumeStr.toNumeric)
        } yield
            DealFields(
                price = price,
                volume = volume
            )

    def quoteSelectorEmission(clientPrice: BigInteger, bandPrice: BigInteger, tolerancePrice: BigInteger): Result[BigInteger] = Result {
        logger.debug(s"quote selector client price $clientPrice , bandPrice: $bandPrice, tolerancePrice $tolerancePrice")

        clientPrice match {
            case price if price == bandPrice =>
                logger.debug(s"client price match to band price, selecting client price")
                price
            case price if tolerancePrice <= price && price < bandPrice =>
                logger.debug(s"tolerancePrice <= price < bandPrice, selecting client price")
                price
            case price if price < tolerancePrice && tolerancePrice < bandPrice =>
                logger.debug(s"price < tolerancePrice && tolerancePrice < bandPrice, rejecting transaction")
                FailErr(IndexTradeError.PriceNotValid)
            case price if price > bandPrice =>
                logger.debug(s"price > bandPrice , selecting client price")
                bandPrice
            case _ =>
                FailErr(IndexTradeError.PriceCnd)
        }
    }

    private def processTokensExchange()(implicit context: SmartContractOperationContext): SmartContractResult = {

        val typeId = context.contract.id
        val contractIdBytes = context.contract.id.getBytes(StandardCharsets.UTF_8)
        logger.info(s"exchanging tokens [${typeId}]")

        val contractAddress =
            TokenOwner(
                ownerType = OwnerType.SmartContractId,
                address = contractIdBytes
            )
        logger.debug(s"proccess token exchange for contract with address ${contractAddress}")

        val feedValue = context.getFeedValue(context.contract.feeds.head)
            .getOrElse(FailErr(IndexTradeError.DfVl))

        val quote: Quote = processDataFeedValue(feedValue)
            .getOrElse(FailErr(IndexTradeError.DfVl))
        setServiceStatus(quote.quoteCondition)

        val quoteTimeStamp = LocalDateTime.parse(quote.quotesTimeStamp, DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"))
        val quoteValueDate = LocalDate.parse(quote.valueDate, DateTimeFormatter.ofPattern("yyyyMMdd"))
        logger.debug(s"parsed quote timestamp: $quoteTimeStamp , value date $quoteValueDate")
        val maxQuoteTTL: BigInteger = quoteTTL

        val deal = context.currentDeal.getOrElse(FailErr(IndexTradeError.NoDeal))
        val dealIndex = context.dealsAccepted.length

        val tokenDealParams = {
            val dealTS = context.timestamp


            val dealTimeStamp = Instant.parse(dealTS)
            val dealTimeStampZone = dealTimeStamp.atZone(ZoneOffset.UTC)
            logger.debug(s"deal timestamp string: $dealTS, parsed: $dealTimeStampZone")
            val diffTimeStampQuote = Duration.between(quoteTimeStamp, dealTimeStampZone).toSeconds
            logger.debug(s"max quote ttl: $maxQuoteTTL , deal vs quote difference: $diffTimeStampQuote")
            if (diffTimeStampQuote > maxQuoteTTL.toLong) {
                logger.debug(s"quote ttl exceeded.")
                FailErr(IndexTradeError.TtL)
            }
            if (dealTimeStamp < subscriptionStartDate) {
                logger.debug(s"deal $dealTimeStamp can not be early than start date ${subscriptionStartDate: Instant}")
                FailErr(IndexTradeError.DateStart)
            }
            if (dealTimeStamp > subscriptionEndDate) {
                logger.debug(s"deal $dealTimeStamp can not be later than end date ${subscriptionEndDate: Instant}")
                FailErr(IndexTradeError.DateEnd)
            }


            deal.deal.deal.legs.map { leg =>
                val dealFieldsValue =
                    requiredInvestmentDealFields.zipWithIndex.map { case (f, index) =>
                        (f.id.toLowerCase(), f.typeId, leg.fields(index))
                    }
                logger.debug(s"received deal required field values:")
                dealFieldsValue.foreach { d =>
                    logger.debug(s"field: ${d._1} \t\t (${d._2}): ${d._3}")
                }
                val dealFields = processLegRequiredFields(dealFieldsValue)
                    .getOrElse(FailErr(IndexTradeError.DealMeta))
                logger.debug(s"deal fields: $dealFields")
                (leg.tokenId,
                    context.getTokenValue(leg.tokenId)
                        .getOrElse(FailErr(IndexTradeError.TokenValue, leg.tokenId))
                    , dealFields)
            }
        }
        val tradeResultForToken =
            tokenDealParams.map { case (id, tokenValue, params) =>
                logger.debug(s"deal parameters for id ${id} with value $tokenValue")
                logger.debug(s"client price request: ${params.price}")
                logger.debug(s"client volume request: ${params.volume}")
                logger.debug(s"current quote band price ${quote.bandPrice} tolerance price ${quote.tolerancePrice}")
                val selectedQuote = quoteSelectorEmission(params.price, quote.bandPrice.toNumeric, quote.tolerancePrice.toNumeric)
                    .getOrElse(FailErr(IndexTradeError.QuoteSelect))
                logger.debug(s"selected quote $selectedQuote")
                val volume = params.volume
                logger.debug(s"trade volume $volume")
                val requiredSum = volume * selectedQuote
                logger.debug(s"sum required : ${requiredSum}")
                if (requiredSum > tokenValue) FailErr(IndexTradeError.ValueNotEnough)
                val difference = tokenValue - requiredSum
                logger.debug(s"difference between required sum and token value: $difference")
                TradeResult(id, tokenValue, selectedQuote, volume, requiredSum, difference)
            }

        val totalTradeRequest = tradeResultForToken.map(_.volume).foldLeft("0".toNumeric)(_.add(_))
        logger.debug(s"total requested trade volume ${totalTradeRequest: BigInteger}")
        if (totalTradeRequest < (minTransactionSum: BigInteger)) {
            logger.debug(s"requested trade volume $totalTradeRequest less than soft cap ${softcapSum: BigInteger}")
            FailErr(IndexTradeError.TvExcdTs)
        }
        if (totalTradeRequest < (softcapSum: BigInteger)) {
            logger.debug(s"requested trade volume $totalTradeRequest less than soft cap ${softcapSum: BigInteger}")
            FailErr(IndexTradeError.TvSoftCap)
        }

        if (totalTradeRequest > (maxTransactionSum: BigInteger)) {
            logger.debug(s"requested trade volume $totalTradeRequest can not be greate than max Tokens per operations ${maxTransactionSum: BigInteger}")
            FailErr(IndexTradeError.TvTpo)
        }

        val newTotalIssued = (totalIssued: BigInteger) + totalTradeRequest

        if (newTotalIssued > (hardcapSum: BigInteger)) {
            logger.debug(s"requested trade volume $totalTradeRequest exceed hard cap ${hardcapSum: BigInteger}")
            FailErr(IndexTradeError.TvHardCap)
        }

        totalIssued := newTotalIssued

        logger.debug(s"trade to execute:")
        val toChange =
            tradeResultForToken.flatMap { trade =>
                logger.debug(s"trade: $trade")
                if (trade.change > "0".toNumeric) {
                    logger.debug(s"change needed to token ${trade.tokenId}")
                    Option(
                        context.changeTokens(trade.tokenId, Collection(trade.requiredSum, trade.change))
                            .getOrElse(FailErr(IndexTradeError.Change, trade.requiredSum.toString, trade.change.toString))
                    )
                } else None
            }
        logger.debug(s"change request:")
        toChange.foreach { c =>
            logger.debug(s"id to change: ${c.toChange}")
            logger.debug(s"ids to create:")
            c.toCreate.foreach { n =>
                logger.debug(s"new token id: ${n.id} with value ${n.value}")
            }
        }

        val ownerAddressIssuer = TokenOwner.parseFrom(Base64.getDecoder.decode(issuerAddress))

        val result =
            contract.SmartContractResult(
                emission =
                    Collection(
                        IssueTokenRequest(
                            IssueTokens(
                                operationId = context.nextUniqueId,
                                timestamp = context.timestamp,
                                tokens = deal.tokens.map { token =>
                                    val amount = tradeResultForToken.find(_.tokenId == token.id)
                                        .getOrElse(FailErr(IndexTradeError.TradeResult, token.id)).volume

                                    logger.info(s"trade amount: $amount")
                                    TokenId.encode(typeId, context.nextUniqueId).map { tokenId =>
                                        val currentTokenTradeResult =
                                            tradeResultForToken.find(_.tokenId == token.id)
                                                .getOrElse(FailErr(IndexTradeError.TradeResult, token.id))
                                        IssueToken(
                                            tokenId = tokenId,
                                            owner = token.from,
                                            body = TokenContent(
                                                Collection(
                                                    amount.toString,
                                                    currentTokenTradeResult.selectedQuote.toString,
                                                    quote.symbol,
                                                    quote.tenorValue,
                                                    quote.valueDate,
                                                    quote.bandPrice,
                                                    quote.tolerancePrice,
                                                    quote.maxBandVolume
                                                )
                                            ),
                                            relatedDealRef = RelatedDealReference(dealIndex, token.leg),
                                            extra = Bytes.empty
                                        )

                                    }.getOrElse(FailErr(IndexTradeError.CrTid))
                                }
                            ),
                            Collection(
                                RequestActor(
                                    RequestActorType.SmartContract,
                                    context.contract.id.getBytes(StandardCharsets.UTF_8)
                                )
                            )
                        )
                    ),
                transfer =
                    deal.tokens.flatMap { token =>
                        toChange.find(_.toChange == token.id)
                        match {
                            case Some(ch) =>
                                Collection(
                                    newDealRequest(ch.toCreate.head.id, ownerAddressIssuer, contractAddress, RelatedDealReference(dealIndex, token.leg)),
                                    newDealRequest(ch.toCreate.last.id, context.getTokenOwner(ch.toChange), contractAddress, RelatedDealReference(dealIndex, token.leg))
                                )
                            case None =>
                                Collection(
                                    newDealRequest(token.id, ownerAddressIssuer, contractAddress, RelatedDealReference(dealIndex, token.leg))
                                )
                        }
                    },
                burn = Collection.empty,
                change = toChange,
            )
        result
    }

    private def newDealRequest(tokenId: String, ownerAddressIssuer: TokenOwner, contractAddress: TokenOwner, relatedDealReference: RelatedDealReference)(implicit context: SmartContractOperationContext): DealRequest =
        DealRequest(
            Deal(
                operationId = context.nextUniqueId,
                timestamp = context.timestamp,
                dealId = context.nextUniqueId,
                legs = Collection(
                    DealLeg(
                        tokenId,
                        ownerAddressIssuer,
                        contractAddress,
                        relatedDealReference,
                        Collection.empty
                    )
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
                "investmentTokenType",
                Collection(
                    FieldMeta("price", FieldType.Numeric),
                    FieldMeta("volume", FieldType.Numeric)
                )
            )
        )

    private val requiredInvestmentDealFields = requiredDealFields.find(_.attributeId == "investmentTokenType")
        .getOrElse(FailErr(IndexTradeError.Fields)).fields

    private def FailErr(err: String, data: String*) = {
        throw new  Exception(ErrorEncoder.encode(err, data))
    }
}

case class TradeResult(tokenId: String, tokenValue: BigInteger, selectedQuote: BigInteger, volume: BigInteger, requiredSum: BigInteger, change: BigInteger)

case class DealFields(price: BigInteger, volume: BigInteger)

case class Quote(quoteEntryId: String, quotesTimeStamp: String, symbol: String, tenorValue: String, valueDate: String,
    bandPrice: String, tolerancePrice: String, maxBandVolume: String, quoteCondition: String)


