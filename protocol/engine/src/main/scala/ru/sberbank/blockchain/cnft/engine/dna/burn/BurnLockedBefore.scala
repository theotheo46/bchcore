package ru.sberbank.blockchain.cnft.engine.dna.burn

import ru.sberbank.blockchain.cnft.common.types._
import ru.sberbank.blockchain.cnft.engine.dna._
import ru.sberbank.blockchain.cnft.model.SignedBurnRequest

/**
 * @author Alexey Polubelov
 */
object BurnLockedBefore extends BurnGene
    with SignatureVerify
    with NoAdditionalFields
    with NoInstanceAttributes
    with LockUpByDateGene {

    override def canBurn(
        context: GeneExecutionContext, tokenId: String, request: SignedBurnRequest
    ): Result[Boolean] =
        lockUpPassed(context)

}
