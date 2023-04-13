package ru.sberbank.blockchain.cnft.model

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("BurntTokenStatus")
object WalletTokenStatus{
    val Burnt = 0
    val Changed = 1
    val Merged = 2
    val Issued = 3
}
