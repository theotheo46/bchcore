package ru.sberbank.blockchain.common.cryptography.store

import io.getquill.{PostgresJdbcContext, SnakeCase}
import ru.sberbank.blockchain.cnft.commons.{Result => R}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.HDPathStore

import java.io.Closeable
import javax.sql.DataSource

class DBHDPathStore(
    dataSource: DataSource with Closeable,
    val identity: String,
    val rootId: String
) extends HDPathStore[R] {
    private val db = new PostgresJdbcContext[SnakeCase](SnakeCase, dataSource)

    import db._

    private case class HDKeyPath(identityIndex: String, value: KeyIdentifier)

    override def saveKeyPath(keyPath: KeyIdentifier): R[Unit] = R {
        db.run {
            query[HDKeyPath].insert(lift(HDKeyPath(identity + rootId, keyPath))).onConflictIgnore
        }
        ()
    }

    override def getCurrentKeyPath: R[KeyIdentifier] = R {
        db.run {
            query[HDKeyPath].filter(_.identityIndex == lift(identity + rootId))
        }
            .map(_.value)
            .headOption
            .getOrElse(s"1/1")
    }
}
