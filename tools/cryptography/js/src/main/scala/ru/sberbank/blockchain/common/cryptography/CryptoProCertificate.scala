package ru.sberbank.blockchain.common.cryptography

import upickle.default.{ReadWriter => RW, macroRW}

/**
 * @author Vladimir Sidorov
 */
case class CryptoProCertificate(
    content: String,
    thumbprint: String,
    pubKey: String
)

object CryptoProCertificate {
    implicit val RW: RW[CryptoProCertificate] = macroRW
}
