package ru.sberbank.blockchain.common.cryptography.store

import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

import scala.collection.mutable
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Alexey Polubelov
 */
@JSExportAll
@JSExportTopLevel("InMemoryKeysStore")
class InMemoryKeysStore extends KeysStore[WebCryptoKey] {
    private val store = new mutable.HashMap[KeyIdentifier, WebCryptoKey]()

    override def get(keyId: KeyIdentifier): Result[Option[WebCryptoKey]] = Result(store.get(keyId))

    override def save(keyId: KeyIdentifier, key: WebCryptoKey): Result[Unit] = Result {
        store.put(keyId, key)
        ()
    }

    override def list: Seq[(KeyIdentifier, WebCryptoKey)] = store.toSeq

}
