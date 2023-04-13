package ru.sberbank.blockchain.cnft.engine.dna

import ru.sberbank.blockchain.cnft.engine.dna.burn.{BurnLockedAfter, BurnLockedBefore, BurnLockedBetween, RequireOwnerSignatureForBurn, BurnControlledBySmartContract}
import ru.sberbank.blockchain.cnft.engine.dna.change.Fungible
import ru.sberbank.blockchain.cnft.engine.dna.emission.{ControlledByIssuer, ControlledBySmartContract}
import ru.sberbank.blockchain.cnft.engine.dna.transfer.{RequireOwnerSignatureForTransfer, RequireRecipientSignatureForTransfer, TransferLockedAfter, TransferLockedBefore}
import ru.sberbank.blockchain.cnft.model.GeneID

/**
 * @author Alexey Polubelov
 */
object GenesRegistry {

    val EmissionGenes = Map(
        GeneID.EmissionControlledByIssuer -> ControlledByIssuer,
        GeneID.EmissionControlledBySmartContract -> ControlledBySmartContract
    )

    val TransferGenes = Map(
        GeneID.RequireOwnerSignatureForTransfer -> RequireOwnerSignatureForTransfer,
        GeneID.RequireRecipientSignatureForTransfer -> RequireRecipientSignatureForTransfer,
        GeneID.TransferLockedBefore -> TransferLockedBefore,
        GeneID.TransferLockedAfter -> TransferLockedAfter
    )

    val BurnGenes = Map(
        GeneID.RequireOwnerSignatureForBurn -> RequireOwnerSignatureForBurn,
        GeneID.BurnControlledBySmartContract -> BurnControlledBySmartContract,
        GeneID.BurnLockedBefore -> BurnLockedBefore,
        GeneID.BurnLockedAfter -> BurnLockedAfter,
        GeneID.BurnLockedBetween -> BurnLockedBetween
    )

    val ChangeGenes = Map(
        GeneID.Fungible -> Fungible
    )
}
