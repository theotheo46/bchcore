package ru.sberbank.blockchain.cnft.engine.dna.change

import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.cnft.engine.dna.GeneExecutionContext
import ru.sberbank.blockchain.cnft.model.{FieldMeta, TokenChangeRequest}

/**
 * @author Alexey Polubelov
 */
trait ChangeGene {
    def genInstanceAttributes: Seq[FieldMeta]

    def requireAdditionalFields: Seq[FieldMeta]

    def canChange(context: GeneExecutionContext, changeRequest: TokenChangeRequest): Result[Boolean]
}
