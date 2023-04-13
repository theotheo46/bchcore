package ru.sberbank.blockchain.cnft.gate.model

import upickle.default

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Maxim Fedin
 */
@JSExportAll
@JSExportTopLevel("TxResult")
case class TxResult[T](
    blockNumber: Long,
    txId: String,
    value: T
)

object TxResult {
    implicit def rw[T: default.ReadWriter]: default.ReadWriter[TxResult[T]] = upickle.default.macroRW
}