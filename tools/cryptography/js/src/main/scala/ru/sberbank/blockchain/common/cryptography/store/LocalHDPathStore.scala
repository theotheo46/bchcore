package ru.sberbank.blockchain.common.cryptography.store

import org.scalajs.dom
import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.HDPathStore
import upickle.default._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import scala.scalajs.js.JSConverters._

@JSExportAll
@JSExportTopLevel("LocalHDPathStore")
class LocalHDPathStore(val identity: String, val rootId: String) extends HDPathStore[Result] {
    private val keyStoreName: String = "hdPaths"

    override def saveKeyPath(keyPath: KeyIdentifier): Result[Unit] =
        saveToStore(keyStoreName, identity + rootId, keyPath)

    override def getCurrentKeyPath: Result[KeyIdentifier] =
        readFromStore[KeyIdentifier](keyStoreName, identity + rootId).toFuture
            .map(_.getOrElse(s"1/1")).toJSPromise

    def saveToStore[T: ReadWriter](storeName: String, key: String, value: T): Result[Unit] = Result {
        if (dom.window.localStorage.hasOwnProperty(storeName)) {
            val currentStoreState: String = dom.window.localStorage.getItem(storeName)
            val desSerStore = read[Map[String, T]](currentStoreState)
            val newState = desSerStore + (key -> value)
            val data: String = write[Map[String, T]](newState)
            dom.window.localStorage.setItem(storeName, data)
        }
        else {
            val newState: Map[String, T] = Map(key -> value)
            val data: String = write[Map[String, T]](newState)
            dom.window.localStorage.setItem(storeName, data)
        }
    }

    def readFromStore[T: ReadWriter](storeName: String, key: String): Result[Option[T]] = Result {
        if (dom.window.localStorage.hasOwnProperty(storeName)) {
            val currentStoreState: String = dom.window.localStorage.getItem(storeName)
            read[Map[String, T]](currentStoreState).get(key)
        }
        else None
    }

}
