package ru.sberbank.blockchain.cnft.engine.dna

import ru.sberbank.blockchain.cnft.commons.Result

/**
 * @author Alexey Polubelov
 */
trait TypeFieldsAccess {

    def getFieldIndex(context: GeneExecutionContext, name: String): Result[Int] = {
        val theType = context.getType
        theType.meta.fields.zipWithIndex.find(_._1.id == name).map(_._2).toRight(s"Mandatory field $name missing in type ${theType.typeId}")
    }

}
