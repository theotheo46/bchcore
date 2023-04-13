package ru.sberbank.blockchain.cnft.model

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Maxim Fedin
 */
@JSExportAll
@JSExportTopLevel("OperationType")
object OperationType {
    val Issue = "Issue"
    val Burn = "Burn"
    val Deal = "Deal"
    val Accept = "Accept"

}
