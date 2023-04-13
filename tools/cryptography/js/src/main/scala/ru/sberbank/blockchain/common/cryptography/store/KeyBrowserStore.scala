//package ru.sberbank.blockchain.common.cryptography
//
//import org.scalajs.dom
//import ru.sberbank.blockchain.cnft.commons.{Bytes, Result}
//import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
//import upickle.core.Visitor
//import upickle.default._
//import java.nio.charset.StandardCharsets
//import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
//import scala.scalajs.js.typedarray.{Int8Array, byteArray2Int8Array}
//
//@JSExportAll
//@JSExportTopLevel("KeyBrowserStore")
//class KeyBrowserStore extends KeysStore[WebCryptoKey]{
//    implicit val WebCryptoKeyRW: ReadWriter[WebCryptoKey] = macroRW
//
//    implicit val BytesAsBase64RW: ReadWriter[Bytes] = new ReadWriter[Bytes] with SimpleReader[Bytes] {
//
//        override def expectedMsg: String = "expected string"
//
//        override def visitString(s: CharSequence, index: Int): Bytes =
//            byteArray2Int8Array(
//                java.util.Base64.getDecoder.decode(
//                    s.toString.getBytes(StandardCharsets.UTF_8)
//                )
//            ).buffer
//
//
//        override def write0[V](out: Visitor[_, V], v: Bytes): V =
//            out.visitString(
//                new String(
//                    java.util.Base64.getEncoder.encode(
//                        new Int8Array(v).toArray
//                    ),
//                    StandardCharsets.UTF_8
//                ),
//                -1
//            )
//
//    }
//
//    def saveToStore[T: ReadWriter](storeName: String, key: String, value: T): Result[Unit] = Result {
//        if (dom.window.localStorage.hasOwnProperty(storeName)) {
//            val currentStoreState: String = dom.window.localStorage.getItem(storeName)
//            val desSerStore = read[Map[String, T]](currentStoreState)
//            val newState = desSerStore + (key -> value)
//            val data: String = write[Map[String, T]](newState)
//            dom.window.localStorage.setItem(storeName, data)
//        }
//        else {
//            val newState: Map[String, T] = Map(key -> value)
//            val data: String = write[Map[String, T]](newState)
//            dom.window.localStorage.setItem(storeName, data)
//        }
//    }
//
//    def readFromStore[T: ReadWriter](storeName: String, key: String): Result[Option[T]] = Result {
//        if (dom.window.localStorage.hasOwnProperty(storeName)) {
//            val currentStoreState: String = dom.window.localStorage.getItem(storeName)
//            read[Map[String, T]](currentStoreState).get(key)
//        }
//        else None
//    }
//
//    def readMapFromStore[T: ReadWriter](storeName: String): Result[Option[Map[String, T]]] = Result {
//        if (dom.window.localStorage.hasOwnProperty(storeName)) {
//            val currentStoreState: String = dom.window.localStorage.getItem(storeName)
//            Some(read[Map[String, T]](currentStoreState))
//        }
//        else None
//    }
//
//    def removeFromStore[T: ReadWriter](storeName: String, key: String): Result[Unit] = Result {
//        if (dom.window.localStorage.hasOwnProperty(storeName)) {
//            val currentStoreState: String = dom.window.localStorage.getItem(storeName)
//            val desSerStore = read[Map[String, T]](currentStoreState).filterKeys(_ != key)
//            val data: String = write[Map[String, T]](desSerStore)
//            dom.window.localStorage.setItem(storeName, data)
//        }
//    }
//
//    private val keyStoreName: String = "keys"
//
//    override def save(keyId: KeyIdentifier, key: WebCryptoKey): Result[Unit] = saveToStore(keyStoreName, keyId, key)
//
//    override def get(keyId: KeyIdentifier): Result[Option[WebCryptoKey]] = {
//        val res = readFromStore[WebCryptoKey](keyStoreName, keyId)
//        res
//    }
//
//    def clearStore(): Result[Unit] = Result(dom.window.localStorage.removeItem(keyStoreName))
//}