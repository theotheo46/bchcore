package ru.sberbank.blockchain.cnft.engine.dna.burn

import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.cnft.engine.dna.{GeneExecutionContext, LockUpByDateGene, NoAdditionalFields, NoInstanceAttributes, SignatureVerify}
import ru.sberbank.blockchain.cnft.model.SignedBurnRequest

object BurnLockedBetween extends BurnGene
    with SignatureVerify
    with NoAdditionalFields
    with NoInstanceAttributes
    with LockUpByDateGene {

    override def canBurn(
        context: GeneExecutionContext, tokenId: String, request: SignedBurnRequest
    ): Result[Boolean] =
        // requested as hotfix
        Result(true)

}
