package ru.sberbank.blockchain.common.cryptography.store

import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

/**
 * @author Alexey Polubelov
 */
trait CryptographicKeysStore {

    def save(key: KeyIdentifier, keyBytes: Array[Byte]): Unit

    def get(key: KeyIdentifier): Option[Array[Byte]]

    def list: Seq[(KeyIdentifier, Array[Byte])]
}
