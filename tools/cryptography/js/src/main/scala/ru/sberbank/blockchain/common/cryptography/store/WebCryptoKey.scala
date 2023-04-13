package ru.sberbank.blockchain.common.cryptography.store;


import ru.sberbank.blockchain.cnft.commons.Bytes
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("WebCryptoKey")
case class WebCryptoKey(
    identifier: KeyIdentifier,
    publicAsBytes: Bytes,
    privateKeyAsBytes: Bytes
)