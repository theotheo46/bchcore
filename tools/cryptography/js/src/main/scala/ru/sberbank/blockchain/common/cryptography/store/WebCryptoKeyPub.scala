package ru.sberbank.blockchain.common.cryptography.store

import ru.sberbank.blockchain.cnft.commons.Bytes
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Vladimir Sidorov
 */
@JSExportAll
@JSExportTopLevel("WebCryptoKeyPub")
case class WebCryptoKeyPub(
    identifier: KeyIdentifier,
    publicAsBytes: Bytes,
    publicAsString: String
)
