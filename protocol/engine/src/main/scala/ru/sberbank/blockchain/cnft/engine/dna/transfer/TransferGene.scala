package ru.sberbank.blockchain.cnft.engine.dna.transfer

import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.cnft.engine.dna.GeneExecutionContext
import ru.sberbank.blockchain.cnft.model.{DealLeg, DealRequest, FieldMeta}

/**
 * @author Alexey Polubelov
 */
trait TransferGene {
    def genInstanceAttributes: Seq[FieldMeta]

    def requireAdditionalFields: Seq[FieldMeta]

    def canTransfer(context: GeneExecutionContext, leg: DealLeg, deal: DealRequest): Result[Boolean]
}