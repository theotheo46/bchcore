package ru.sberbank.blockchain.cnft.wallet.store

import ru.sberbank.blockchain.cnft.common.types.Result
import ru.sberbank.blockchain.common.cryptography.store.DBHDPathStore
import ru.sberbank.blockchain.common.cryptography.{HDPathStore => HDS, HDPathStoreFactory, InMemoryHDPathStore}

import java.io.Closeable
import javax.sql.DataSource

/**
 * @author Alexey Polubelov
 */
object HDPathStore {

    def inMemory(): HDPathStoreFactory[Result] =
        new HDPathStoreFactory[Result] {
            override def newStoreFor(identity: String): HDS[Result] =
                new InMemoryHDPathStore[Result]("1")
        }

    def fromDataSource(source: DataSource with Closeable, root: String): HDPathStoreFactory[Result] =
        new HDPathStoreFactory[Result] {
            override def newStoreFor(identity: String): HDS[Result] =
                new DBHDPathStore(source, identity, root)
        }
}
