package ru.sberbank.blockchain.cnft.engine.dna

import ru.sberbank.blockchain.cnft.model.FieldMeta

/**
 * @author Alexey Polubelov
 */
trait NoAdditionalFields {
    def requireAdditionalFields: Seq[FieldMeta] = Seq.empty
}