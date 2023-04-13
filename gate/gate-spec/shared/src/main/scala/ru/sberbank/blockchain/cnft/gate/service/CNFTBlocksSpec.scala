package ru.sberbank.blockchain.cnft.gate.service

import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import tools.http.service.annotations.HttpGet

import scala.language.higherKinds
import scala.scalajs.js.annotation.JSExport

/**
 * @author Alexey Polubelov
 */
trait CNFTBlocksSpec[R[_]] {
    // =====================================================================================================================
    // Blocks API
    // =====================================================================================================================

    @JSExport
    @HttpGet("/block-number")
    def getLatestBlockNumber: R[Long]

    @JSExport
    @HttpGet("/block-events")
    def getTransactions(blockNumber: Long): R[BlockEvents]

    @JSExport
    @HttpGet("/block-event")
    def getTransaction(blockNumber: Long, txId: String): R[BlockEvents]

}
