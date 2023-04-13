package ru.sberbank.blockchain.cnft.engine.dna.burn

import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.cnft.engine.dna.GeneExecutionContext
import ru.sberbank.blockchain.cnft.model.{SignedBurnRequest, FieldMeta}

/**
 * @author Alexey Polubelov
 */
trait BurnGene {
    def genInstanceAttributes: Seq[FieldMeta]

    def requireAdditionalFields: Seq[FieldMeta]

    def canBurn(context: GeneExecutionContext, tokenId: String, request: SignedBurnRequest): Result[Boolean]
}
