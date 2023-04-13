//package ru.sberbank.blockchain.common.cryptography.cryptopro
//
//import ru.sberbank.blockchain.cnft.commons.ROps._
//import ru.sberbank.blockchain.cnft.commons.{Base64R, Bytes, Result, asByteArray, asBytes}
//import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
//import ru.sberbank.blockchain.common.cryptography._
//import upickle.default._
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.scalajs.js.JSConverters._
//import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
//
//
///**
// * @author Vladimir Sidorov
// */
//@JSExportAll
//@JSExportTopLevel("CryptoProEncryptionOperations")
//class CryptoProEncryptionOperations(
//    selectKeyCallback: SelectKeyCallback
//) extends EncryptionOperations[Result] {
//
//    override def requestNewKey(): Result[KeyIdentifier] =
//        for {
//            certificates <- CryptoProLib.initCertList()
//            thumbprint <- selectKeyCallback.selectKey(certificates)
//            rawCryptoProCert <- CryptoProLib.exportCertificateByThumbprint(thumbprint)
//            cert <- Result(read[CryptoProCertificate](rawCryptoProCert))
//        } yield cert.thumbprint
//
//
//    override def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] =
//        CryptoProLib.exportCertificateByThumbprint(keyIdentifier)
//            .toFuture
//            .map(_ => true)
//            .recover { case _ => false }
//            .toJSPromise
//
//    override def publicKey(keyIdentifier: KeyIdentifier): Result[Option[Bytes]] = {
//        for {
//            rawCryptoProCert <- CryptoProLib.exportCertificateByThumbprint(keyIdentifier)
//            cert <- Result(read[CryptoProCertificate](rawCryptoProCert))
//            decodedPubKey <- Base64R.decode(cert.content)
//        } yield Option(asBytes(decodedPubKey))
//    }
//
//    override def encrypt(message: Bytes, receiverPublicKey: Bytes): Result[Bytes] =
//        for {
//            b64Message <- Base64R.encode(asByteArray(message))
//            keyString <- Base64R.encode(asByteArray(receiverPublicKey))
//            encryptedMessage <- CryptoProLib.encrypt(b64Message, keyString)
//            decodedMessage <- Base64R.decode(encryptedMessage)
//        } yield asBytes(decodedMessage)
//
//    override def decrypt(decryptionData: Bytes, keyIdentifier: KeyIdentifier): Result[Bytes] =
//        for {
//            encodedB64 <- Base64R.encode(asByteArray(decryptionData))
//            decryptionMessage <- CryptoProLib.decrypt(encodedB64)
//            decodedMessage <- Base64R.decode(decryptionMessage)
//        } yield asBytes(decodedMessage)
//
////    override def importEncryptionKey(publicKey: Bytes, privateKey: Bytes): Result[String] =
////        Result.Fail("Method not supported")
////
////    override def exportEncryptionKey(keyIdentifier: KeyIdentifier): Result[Bytes] =
////        Result.Fail("Method not supported")
//}
//
//
