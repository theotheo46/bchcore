package ru.sberbank.blockchain.cnft.engine.dna.emission

import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.cnft.engine.dna.GeneExecutionContext
import ru.sberbank.blockchain.cnft.model.{FieldMeta, IssueTokenRequest}

/**
 * @author Alexey Polubelov
 */
trait EmissionGene {
    def genInstanceAttributes: Seq[FieldMeta]

    def requireAdditionalFields: Seq[FieldMeta]

    def canIssue(context: GeneExecutionContext, request: IssueTokenRequest): Result[Boolean]
}
