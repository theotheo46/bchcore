package ru.sberbank.blockchain.cnft.engine.dna

import ru.sberbank.blockchain.cnft.common.types.{Bytes, Collection}
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.model.Signatures

/**
 * @author Alexey Polubelov
 */
trait SignatureVerify extends LoggingSupport {

    def verifySignatures(
        context: GeneExecutionContext, content: Bytes,
        address: Signatures, signatures: Collection[Bytes]
    ): Boolean = {
        val keys = address.keys
        val require = address.require
        //        logger.debug(s"VerifySignatures for:\n${address.toProtoString}\n")
        var found = 0
        keys.takeWhile { k =>
            if (
                signatures.exists { s =>
                    context.cryptography
                        .verifySignature(k, content, s)
                        .contains(true)
                }
            ) found += 1
            found >= require
        }
        //        logger.debug(s"VerifySignatures: found $found from $require")
        found >= require
    }

}
