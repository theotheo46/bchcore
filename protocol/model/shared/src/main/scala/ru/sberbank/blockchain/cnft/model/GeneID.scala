package ru.sberbank.blockchain.cnft.model

/**
 * @author Alexey Polubelov
 */
@scala.scalajs.js.annotation.JSExportAll
@scala.scalajs.js.annotation.JSExportTopLevel("GeneID")
object GeneID {
    val EmissionControlledByIssuer = "emission-controlled-by-issuer"
    val EmissionControlledBySmartContract = "emission-controlled-by-smart-contract"

    val RequireOwnerSignatureForTransfer = "require-owner-signature-for-transfer"
    val RequireRecipientSignatureForTransfer = "require-recipient-signature-for-transfer"
    val TransferLockedBefore = "transfer-locked-before"
    val TransferLockedAfter = "transfer-locked-after"

    val RequireOwnerSignatureForBurn = "require-owner-signature-for-burn"
    val BurnControlledBySmartContract = "burn-controlled-by-smartcontract"
    val BurnLockedBefore = "burn-locked-before"
    val BurnLockedAfter = "burn-locked-after"
    val BurnLockedBetween = "burn-locked-between"

    val Fungible = "fungible"
}
