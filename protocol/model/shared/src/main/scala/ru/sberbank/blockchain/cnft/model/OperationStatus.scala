package ru.sberbank.blockchain.cnft.model

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Maxim Fedin
 */
@JSExportAll
@JSExportTopLevel("OperationStatus")
object OperationStatus {

    val AcceptPending = "Accept_Pending"

    val IssuePendingRegulation = "Issue_PendingRegulation"
    val DealPendingRegulation = "Deal_PendingRegulation"
    val BurnPendingRegulation = "Burn_PendingRegulation"

    val IssueRejectedByRegulation = "Issue_RejectedByRegulation"
    val DealRejectedByRegulation = "Deal_RejectedByRegulation"
    val BurnRejectedByRegulation = "Burn_RejectedByRegulation"

    val DealDoneByRegulator = "Deal_DoneByRegulator"
    val BurnDoneByRegulator = "Burn_DoneByRegulator"

    val IssueSCPendingRegulation = "Issue_SCPendingRegulation"
    val DealSCPendingRegulation = "Deal_SCPendingRegulation"
    val BurnSCPendingRegulation = "Burn_SCPendingRegulation"

    val DealRejectedBySmartContract = "Deal_RejectedBySmartContract"

    val IssueDone = "Issue_Done"
    val DealDone = "Deal_Done"
    val BurnDone = "Burn_Done"

//    val IssueFail = "Issue_Fail"
//    val DealFail = "Deal_Fail"
//    val BurnFail = "Burn_Fail"
//    val ChangeFail = "Change_Fail"

    val TransferProposed = "Transfer_Proposed"
    val TokenRequested = "Token_Requested"
}
