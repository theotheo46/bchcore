package ru.sberbank.blockchain.cnft.engine.dna.transfer

import ru.sberbank.blockchain.cnft.common.types.Result
import ru.sberbank.blockchain.cnft.engine.dna._
import ru.sberbank.blockchain.cnft.model.{DealLeg, DealRequest}

/**
 * @author Alexey Polubelov
 */
object TransferLockedBefore extends TransferGene
    with SignatureVerify
    with NoAdditionalFields
    with NoInstanceAttributes
    with LockUpByDateGene {

    def canTransfer(context: GeneExecutionContext, leg: DealLeg, deal: DealRequest): Result[Boolean] = lockUpPassed(context)


}
