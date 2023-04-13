package ru.sberbank.blockchain.common.cryptography

import ru.sberbank.blockchain.cnft.commons.ROps
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

import scala.collection.mutable
import scala.language.higherKinds
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("InMemoryHDPathStore")
class InMemoryHDPathStore[R[+_]](val rootId: String)(implicit val R: ROps[R]) extends HDPathStore[R] {
    private val keyPaths = new mutable.HashMap[String, KeyIdentifier]()

    override def saveKeyPath(keyPath: KeyIdentifier): R[Unit] =
        R(keyPaths(rootId) = keyPath)

    override def getCurrentKeyPath: R[KeyIdentifier] =
        R(keyPaths.getOrElse(rootId, s"1/1"))

}