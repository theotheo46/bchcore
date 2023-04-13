package ru.sberbank.blockchain.cnft.engine.dna

import ru.sberbank.blockchain.cnft.model.FieldMeta

/**
 * @author Alexey Polubelov
 */
trait NoInstanceAttributes {
    def genInstanceAttributes: Seq[FieldMeta] = Seq.empty
}