package ru.sberbank.blockchain.cnft.errors

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("IndexTradeError")
object IndexTradeError {

    /**
     * s"tokens required metadata incorrect"
     */
    val MetadataWrong = "metadata_wrong"

    /**
     * s"tokens types can not be accepted for exchange"
     */
    val TtNotExc = "tt_not_exc"

    /**
     * s"contract out of service"
     */
    val OoS = "oos"

    /**
     * s"can not get tokens type"
     */
    val TtGET = "tt_get"

    /**
     * s"can not get current deal"
     */
    val NoDeal = "no_deal"

    /**
     * s"can not decode token type"
     */
    val TtDecode = "tt_decode"

    /**
     * s"no datafeed value"
     */
    val DfVl = "df_vl"

    /**
     * s"unexpected quote condition"
     */
    val FdCnd = "fd_cnd"

    /**
     * s"invalid price, can not execute trade, rejecting"
     */
    val PriceNotValid = "price_not_valid"

    /**
     * s"unexpected price condition, rejecting trade"
     */
    val PriceCnd = "price_cnd"

    /**
     * s"quote ttl exceeded, rejecting trade"
     */
    val TtL = "ttl"

    /**
     * s"can not execute deal prior subscription start date"
     */
    val DateStart = "date_start"

    /**
     * s"can not execute deal after subscription end date"
     */
    val DateEnd = "date_end"

    /**
     * s"failed to process deal fields"
     */
    val DealMeta = "deal_meta"

    /**
     * s"can not get token value for id leg.tokenId"
     */
    val TokenValue = "token_value"

    /**
     * s"can not select quote, rejecting trade"
     */
    val QuoteSelect = "quote_select"

    /**
     * s"not enough token value by required amount and current quote"
     */
    val ValueNotEnough = "value_not_enough"

    /**
     * s"requested trade volume shall be no less than min transaction sum"
     */
    val TvExcdTs = "tv_excd_ts"

    /**
     * s"requested trade volume shall be no less than softcap"
     */
    val TvSoftCap = "tv_softcap"

    /**
     * s"requested trade volume shall be less than max tokens per operation"
     */
    val TvTpo = "tv_tpo"

    /**
     * s"requested trade volume can not exceed hard cap"
     */
    val TvHardCap = "tv_hardcap"

    /**
     * s"token can not be change to required sum trade.requiredSum and change trade.change"
     */
    val Change = "change"

    /**
     * s"can not find trade result for id token.id"
     */
    val TradeResult = "trade_result"

    /**
     * s"failed to create tokenId"
     */
    val CrTid = "cr_tid"

    /**
     * s"can not get deal fields"
     */
    val Fields = "fields"

}