package ru.sberbank.blockchain.cnft.wallet.store

import ru.sberbank.blockchain.cnft.commons.{Result, ResultOps}
import ru.sberbank.blockchain.common.cryptography.store.LocalHDPathStore
import ru.sberbank.blockchain.common.cryptography.{HDPathStoreFactory, InMemoryHDPathStore, HDPathStore => HDS}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Alexey Polubelov
 */
@JSExportAll
@JSExportTopLevel("HDPathStore")
object HDPathStore {

    def inMemory(): HDPathStoreFactory[Result] =
        new HDPathStoreFactory[Result] {
            override def newStoreFor(identity: String): HDS[Result] =
                new InMemoryHDPathStore[Result]("1")
        }

    def localStorage(root: String): HDPathStoreFactory[Result] =
        new HDPathStoreFactory[Result] {
            override def newStoreFor(identity: String): HDS[Result] =
                new LocalHDPathStore(identity, root)
        }
}
