package ru.sberbank.blockchain.common.cryptography

import ru.sberbank.blockchain.cnft.commons.{Collection, Result}

import scala.scalajs.js
//import scala.scalajs.js.Promise
import scala.scalajs.js.annotation._

@js.native
@JSGlobalScope
object CryptoProLib extends js.Object {

    def signTextByThumbprint(dataToSign: String, thumbprint: String = ""): Result[String] = js.native

    def verify(signature: String, content: String): Result[Unit] = js.native

    def exportCertificateByThumbprint(thumbprint: String): Result[String] = js.native

    def initCertList(): Result[Collection[Collection[String]]] = js.native

    def encrypt(message: String, certificate: String): Result[String] = js.native

    def decrypt(message: String): Result[String] = js.native

}
