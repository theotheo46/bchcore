//package ru.sberbank.blockchain.common.cryptography
//
//import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
//import io.getquill._
//
//import java.io.Closeable
//import javax.sql.DataSource
//
//class DBKeyStore (
//    dataSource: DataSource with Closeable
//) extends CryptographicKeysStore {
//    private val db = new PostgresJdbcContext[SnakeCase](SnakeCase, dataSource)
//    import db._
//
//    private case class Key(id: String, value: Array[Byte])
//
//    override def save(key: KeyIdentifier, keyBytes: Array[Byte]): Unit = {
//        db.run {
//            query[Key].insert(lift(Key(key,keyBytes))).onConflictIgnore
//        }
//        ()
//    }
//
//    override def get(key: KeyIdentifier): Option[Array[Byte]] = db.run {
//      query[Key].filter(_.id == lift(key))
//    }
//      .map(_.value)
//      .headOption
//}
