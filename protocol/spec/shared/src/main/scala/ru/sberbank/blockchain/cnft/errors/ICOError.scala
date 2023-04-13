package ru.sberbank.blockchain.cnft.errors

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("ICOError")
object ICOError {

    /**
     * s"Can not accept tokens, current phase is currentPhase"
     */
    val InvestmentPhase = "investment_phase"

    /**
     * s"wrong token type"
     */
    val WrongType = "wrong_type"

    /**
     * s"Token token.id does not exist"
     */
    val NoToken = "no_token"

    /**
     * s"Investment must divide by investmentCoefficient"
     */
    val Coefficient = "coefficient"

    /**
     *
     */
    val HardCap = "hard_cap"

    /**
     * s"no datafeed value"
     */
    val DFValue = "df_value"

    /**
     * s"failed to create tokenId"
     */
    val CreateId = "create_id"

}