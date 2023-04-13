package ru.sberbank.blockchain.cnft.model

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Maxim Fedin
 */
@JSExportAll
@JSExportTopLevel("SmartContractEventType")
object SmartContractEventType {
    val Issue = "Issue"
    val MakeDeal = "MakeDeal"
    val Burn = "Burn"
    val DataFeed = "DataFeed"
    val Payment = "Payment"
}
