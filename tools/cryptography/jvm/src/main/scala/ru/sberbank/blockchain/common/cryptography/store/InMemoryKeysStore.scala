package ru.sberbank.blockchain.common.cryptography.store

import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

import java.util.concurrent.ConcurrentHashMap
import scala.collection.convert.ImplicitConversions.`map AsScalaConcurrentMap`

/**
 * @author Alexey Polubelov
 */
class InMemoryKeysStore extends CryptographicKeysStore {
    private val keys = new ConcurrentHashMap[KeyIdentifier, Array[Byte]]()

    override def save(key: KeyIdentifier, keyBytes: Array[Byte]): Unit = {
        keys.put(key, keyBytes)
        ()
    }

    override def get(key: KeyIdentifier): Option[Array[Byte]] = Option(keys.get(key))

    override def list: Seq[(KeyIdentifier, Array[Byte])] = keys.toSeq
}