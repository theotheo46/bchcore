package ru.sberbank.blockchain.cnft.model

import ru.sberbank.blockchain.cnft.common.types._

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Andrew Pudovikov
 */
@JSExportAll
@JSExportTopLevel("RegulatorOperation")
object RegulatorOperation {
    val Freeze = "freeze" // freeze/unfreeze
    val Transfer = "transfer"
    val Burn = "burn"

    val IssueControl = "ctl-issue"
    val DealControl = "ctl-deal"
    val BurnControl = "ctl-burn"

    val All: Collection[String] =
        Collection(
            Freeze, Transfer, Burn,
            IssueControl, DealControl, BurnControl
        )
}