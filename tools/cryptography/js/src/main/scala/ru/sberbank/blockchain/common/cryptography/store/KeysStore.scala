package ru.sberbank.blockchain.common.cryptography.store

import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

/**
 * @author Alexey Polubelov
 */
trait KeysStore[Key] {
    def get(keyId: KeyIdentifier): Result[Option[Key]]

    def save(keyId: KeyIdentifier, key: Key): Result[Unit]

    def list: Seq[(KeyIdentifier, Key)]
}
