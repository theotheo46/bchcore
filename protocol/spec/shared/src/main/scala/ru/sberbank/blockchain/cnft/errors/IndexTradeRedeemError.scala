package ru.sberbank.blockchain.cnft.errors

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("IndexTradeRedeemError")
object IndexTradeRedeemError {

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
     * s"no current deal"
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
     * s"can not execut deal after subscription end date"
     */
    val DateEnd = "date_end"

    /**
     *
     */
    val DealMeta = "deal_meta"

    /**
     * s"can not get token value"
     */
    val TokenValue = "token_value"

    /**
     * s"can not select quote, rejecting trade"
     */
    val QuoteSelect = "quote_select"

    /**
     *
     */
    val ValueNotEnough = "value_not_enough"

    /**
     * s"requested trade volume shall be no less than minTransactionSum"
     */
    val TvExcdTs = "tv_excd_ts"

    /**
     * s"requested trade volume shall be no less than SoftCap"
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
     *
     */
    val Change = "change"

    /**
     * s"can not find trade result for id token.id"
     */
    val TradeResult = "trade_result"

    /**
     *
     */
    val CrTid = "cr_tid"

    /**
     * s"can not get deal fields"
     */
    val Fields = "fields"

    /**
     * s"can not process tokens change"
     */
    val NoChangeData = "no_change_data"

    /**
     *
     */
    val NoTechToken = "no_tech_token"

    /**
     *   s"no linked operation id for transfer"
     */
    val NoLinkedOpId = "no_linked_op_id"

    /**
     * s"mismatch token mapping result"
     */
    val Mapping = "mapping"

    /**
     * s"mismatch redeem token to tech token"
     */
    val MappingRedeemTech = "mapping_redeem_tech"

    /**
     * s"redeemed id not found in redeem tokens"
     */
    val NoRedeemId = "no_redeem_id"

    /**
     * s"tech tokens ids does not found in tech tokens list"
     */
    val NoTechId = "no_tech_id"

    /**
     * s"transfer sum does not match redeem sum!"
     */
    val SumTransferRedeem = "sum_transfer_redeem"

    /**
     * s"deal request price shall be the same for all tokens"
     */
    val DealPriceRequest = "deal_price_request"

}