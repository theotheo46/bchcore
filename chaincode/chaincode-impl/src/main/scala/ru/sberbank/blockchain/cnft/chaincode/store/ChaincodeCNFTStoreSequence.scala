package ru.sberbank.blockchain.cnft.chaincode.store

import org.enterprisedlt.fabric.contract.OperationContext
import org.enterprisedlt.spec.Key
import ru.sberbank.blockchain.cnft.engine.CNFTStoreSequence

import java.nio.charset.StandardCharsets
import scala.reflect.ClassTag

/**
 * @author Alexey Polubelov
 */
class ChaincodeCNFTStoreSequence[T: ClassTag](
    name: String,
    seed: String
) extends CNFTStoreSequence {
    private[this] val mspId = OperationContext.clientIdentity.mspId
    private[this] val sequenceKey = Key(mspId, seed, name)
    private[this] var sequence: Long = OperationContext.store.get[Long](sequenceKey).getOrElse(0)

    override def next: String = {
        var value = maybeNext
        while (value.isEmpty)
            value = maybeNext
        value.get
    }

    override def end(): Unit = {
        OperationContext.store.put[Long](sequenceKey, sequence)
    }

    private[this] def maybeNext: Option[String] = {
        sequence = sequence + 1
        val key = deface(s"${name}_${mspId}_${seed}_$sequence")
        OperationContext.store.get[T](key) match {
            case Some(_) => None
            case None => Some(key)
        }
    }

    private[this] def deface(v: String): String =
        v
            .getBytes(StandardCharsets.UTF_8)
            .map(b => java.lang.Integer.toString(b.toInt, 36))
            .mkString
}
