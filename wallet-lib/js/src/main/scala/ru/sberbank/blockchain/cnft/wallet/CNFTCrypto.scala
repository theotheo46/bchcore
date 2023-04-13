package ru.sberbank.blockchain.cnft.wallet

import org.scalajs.dom.crypto.{Crypto, CryptoKey, HashAlgorithm, HmacKeyAlgorithm, KeyFormat, KeyUsage}
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons._
import ru.sberbank.blockchain.cnft.wallet.crypto.CryptographyContext
import ru.sberbank.blockchain.common.cryptography._
import ru.sberbank.blockchain.common.cryptography.cryptopro.{CryptoProSignatureOperations, SelectKeyCallback}
import ru.sberbank.blockchain.common.cryptography.hd.HDSignatureOperations
import ru.sberbank.blockchain.common.cryptography.lib_elliptic._
import ru.sberbank.blockchain.common.cryptography.model.{HDKey, KeysList}
import ru.sberbank.blockchain.common.cryptography.sag.Hasher
import ru.sberbank.blockchain.common.cryptography.store.{InMemoryKeysStore, WebCryptoKey}
import ru.sberbank.blockchain.common.cryptography.webcrypto.WebCryptoEncryptionOperations
import tools.http.service.HttpService._

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import scala.scalajs.js.typedarray.{ArrayBufferView, Int32Array, Uint8Array}


/**
 * @author Alexey Polubelov
 */
@JSExportAll
@JSExportTopLevel("CNFTCrypto")
object CNFTCrypto extends LoggingSupport {

    def newContext(
        identityOpsFactory: SignatureOperationsFactory[Result],
        encryptionOpsFactory: EncryptionOperationsFactory[Result],
        accessOpsFactory: AccessOperationsFactory[Result],
        addressOpsFactory: SignatureOperationsFactory[Result],
        hashFactory: HashFactory[Result],
        secureRandomGeneratorFactory: SecureRandomGeneratorFactory
    ): CryptographyContext[Result] =
        new CryptographyContext[Result](
            identityOpsFactory, encryptionOpsFactory,
            accessOpsFactory, addressOpsFactory,
            hashFactory, secureRandomGeneratorFactory
        )

    //    def webCryptoSign() = new WebCryptoSignatureOperations(GlobalCrypto.crypto.subtle, new KeyBrowserStore)
    //
    //    def webCryptoSign(crypto: SubtleCrypto) = new WebCryptoSignatureOperations(crypto, new InMemoryKeysStore)

    def cryptoProSign(selectKey: SelectKeyCallback): SignatureOperationsFactory[Result] =
        new SignatureOperationsFactory[Result] {
            override def create(identity: String): Result[SignatureOperations[Result]] = Result {
                new CryptoProSignatureOperations(selectKey)
            }

            override def importFrom(identity: String, data: Bytes): Result[SignatureOperations[Result]] = Result {
                new CryptoProSignatureOperations(selectKey)
            }
        }

    //    def customSign(c: CustomSignJS): SignatureOperations[Result] = new CustomSignSJS(c)

    //

    def webCryptoEncryption(crypto: Crypto): EncryptionOperationsFactory[Result] = {
        new EncryptionOperationsFactory[Result] {

            override def create(identity: String): Result[EncryptionOperations[Result]] = Result {
                new WebCryptoEncryptionOperations(crypto.subtle, secureRandomGenerator(crypto).create, new InMemoryKeysStore)
            }

            override def importFrom(identity: String, data: Bytes): Result[EncryptionOperations[Result]] =
                Result(KeysList.parseFrom(asByteArray(data))).map { list =>
                    val store = new InMemoryKeysStore
                    list.keys.toSeq.mapR { key =>
                        val id = new String(Base64.getEncoder.encode(asByteArray(key.pk)), StandardCharsets.UTF_8)
                        store.save(
                            id, WebCryptoKey(id, key.pk, key.sk)
                        )
                    }
                    new WebCryptoEncryptionOperations(crypto.subtle, secureRandomGenerator(crypto).create, store)
                }
        }
    }

    def hdSignatureOperations(crypto: Crypto, jsec: JSEC, pathStore: HDPathStoreFactory[Result]): SignatureOperationsFactory[Result] =
        new SignatureOperationsFactory[Result] {
            private val hasher: HDHasher[Result] = new HDHasher[Result] {
                override def hmac512(key: Bytes, data: Bytes): Result[Bytes] = {
                    for {
                        importedKey <-
                            crypto.subtle.importKey(
                                KeyFormat.raw,
                                key,
                                HmacKeyAlgorithm(
                                    "HMAC",
                                    HashAlgorithm.`SHA-512`,
                                    256),
                                extractable = true,
                                js.Array(KeyUsage.sign, KeyUsage.verify)
                            )
                        hmac <- crypto.subtle.sign(
                            HmacKeyAlgorithm(
                                "HMAC",
                                HashAlgorithm.`SHA-512`,
                                256),
                            importedKey.asInstanceOf[CryptoKey],
                            data
                        ).map(_.asInstanceOf[Bytes])
                    } yield hmac
                }

                override def sha256(data: Bytes): Result[Bytes] = crypto.subtle.digest("SHA-256", data).map(_.asInstanceOf[Bytes])

            }
            private val libElliptic = new LibEllipticOps(jsec)
            private val bip32 = new Bip32[Result, JSECPoint](
                hasher,
                libElliptic
            )

            private def randomBytes(l: Int): Bytes =
                crypto.getRandomValues(new Uint8Array(l)).buffer

            override def create(identity: String): Result[SignatureOperations[Result]] = Result {
                new HDSignatureOperations(
                    pathStore.newStoreFor(identity),
                    bip32,
                    HDKey(
                        k = randomBytes(32),
                        cc = randomBytes(32)
                    ),
                    None
                )
            }

            override def importFrom(identity: String, data: Bytes): Result[SignatureOperations[Result]] =
                for {
                    key <-
                        parseBase58Seed(data).recover { _ =>
                            Result(HDKey.parseFrom(asByteArray(data)))
                        }
                    ops <-
                        Result {
                            new HDSignatureOperations(
                                pathStore.newStoreFor(identity),
                                bip32,
                                key,
                                None
                            )
                        }
                } yield ops

            private def parseBase58Seed(data: Bytes): Result[HDKey] = {
                for {
                    seedString <- Result(new String(asByteArray(data), StandardCharsets.UTF_8))
                    _ <- Result.expect(seedString.startsWith("xprv"), "Must start with xprv")
                    decoded <- Result(Base58.decode(seedString)).map(_.dropRight(4))
                    _ <- Result.expect(decoded.length == 78, "Invalid length")
                    key <- Result(HDKey(asBytes(decoded.slice(46, 78)), asBytes(decoded.slice(13, 45))))
                } yield key
            }
        }

    def remoteSignatureOperations(url: String): SignatureOperationsFactory[Result] = {
        new SignatureOperationsFactory[Result] {

            import ru.sberbank.blockchain.BytesAsBase64RW

            override def create(identity: String): Result[SignatureOperations[Result]] = Result {
                createService[SignatureOperations[Result]](url)
            }

            override def importFrom(identity: String, data: Bytes): Result[SignatureOperations[Result]] = Result {
                createService[SignatureOperations[Result]](url)
            }
        }
    }


    def webCryptoAccessOperations(
        crypto: Crypto,
        jsec: JSEC
    ): AccessOperationsFactory[Result] = {
        new AccessOperationsFactoryImpl[Result, JSECPoint](
            new Hasher[Result] {
                override def hash(b: Seq[Bytes]): Result[Bytes] = {
                    crypto.subtle
                        .digest(HashAlgorithm.`SHA-256`, concatBytes(b))
                        .map(_.asInstanceOf[Bytes])
                }
            },
            new LibEllipticOps(jsec, compressed = false)
        )
    }

    def hash(crypto: Crypto): HashFactory[Result] =
        new HashFactory[Result] {
            override def create: Result[Hash[Result]] = {
                Result(
                    new Hash[Result] {
                        override def sha256(content: Bytes): Result[Bytes] = {
                            crypto.subtle
                                .digest(HashAlgorithm.`SHA-256`, content)
                                .map(_.asInstanceOf[Bytes])
                        }

                        override def sha1(content: Bytes): Result[Bytes] = {
                            crypto.subtle
                                .digest(HashAlgorithm.`SHA-1`, content)
                                .map(_.asInstanceOf[Bytes])
                        }
                    }
                )
            }
        }

    def secureRandomGenerator(crypto: Crypto): SecureRandomGeneratorFactory =
        new SecureRandomGeneratorFactory {
            override def create: SecureRandomGenerator =
                new SecureRandomGenerator {
                    override def nextInt(): Int = {
                        val a = crypto.getRandomValues(new Int32Array(1).asInstanceOf[ArrayBufferView])
                            .asInstanceOf[scalajs.js.typedarray.Int32Array]
                        a(0)
                    }

                    override def nextBytes(num: Int): Bytes =
                        crypto.getRandomValues(new Uint8Array(num).asInstanceOf[ArrayBufferView]).buffer
                }
        }
}
//
//    def jsSignOperationFactory(ops: SignatureOperations[Result]): SignatureOperationsFactory[Result] =
//        new SignatureOperationsFactory[Result] {
//            override def create(): Result[SignatureOperations[Result]] = Result {
//                ops
//            }
//
//            override def importFrom(config: Bytes): Result[SignatureOperations[Result]] = ???
//        }

//    def jsEncryptionOperationFactory(ops: EncryptionOperations[Result]): EncryptionOperationsFactory[Result] =
//        new EncryptionOperationsFactory[Result] {
//            override def create(): Result[EncryptionOperations[Result]] = Result {
//                ops
//            }
//
//            override def importFrom(config: Bytes): Result[EncryptionOperations[Result]] = ???
//        }


//
//class AccessOperationsFactoryImpl[R[+_], P](
//    hasher: Hasher[R],
//    ecOps: ECOps[R, P]
//)(implicit R: ROps[R]) extends AccessOperationsFactory[R] {


////
//// Signature
////
//
//class CustomSignSJS(c: CustomSignJS) extends SignatureOperations[Result] {
//
//    override def requestNewKey(): Result[KeyIdentifier] = c.requestNewKey()
//
//    override def publicKey(keyIdentifier: KeyIdentifier): Result[Option[Bytes]] = c.publicKey(keyIdentifier).map(_.toOption)
//
//    override def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] = c.keyExists(keyIdentifier)
//
//    override def createSignature(key: KeyIdentifier, content: Bytes): Result[Bytes] = c.createSignature(key, content)
//
//    override def verifySignature(key: Bytes, content: Bytes, signature: Bytes): Result[Boolean] = c.verifySignature(key, content, signature)
//
//    override def findKeysByPublic(publicKeyBytes: Collection[Bytes]): Result[Collection[KeyIdentifier]] = c.findKeysByPublic(publicKeyBytes)
//
//}
//
//@js.native
//trait CustomSignJS extends js.Object {
//
//    def requestNewKey(): Result[KeyIdentifier] = js.native
//
//    def publicKey(keyIdentifier: KeyIdentifier): Result[UndefOr[Bytes]] = js.native
//
//    def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] = js.native
//
//    def createSignature(key: KeyIdentifier, content: Bytes): Result[Bytes] = js.native
//
//    def verifySignature(key: Bytes, content: Bytes, signature: Bytes): Result[Boolean] = js.native
//
//    def findKeysByPublic(publicKeyBytes: Collection[Bytes]): Result[Collection[KeyIdentifier]] = js.native
//}
//
////
//// Encryption
////
//
//class CustomEncryptionSJS(c: CustomEncryptionJS) extends EncryptionOperations[Result] {
//
//    override def requestNewKey(): Result[KeyIdentifier] = c.requestNewKey()
//
//    override def publicKey(keyIdentifier: KeyIdentifier): Result[Option[Bytes]] = c.publicKey(keyIdentifier).map(_.toOption)
//
//    override def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] = c.keyExists(keyIdentifier)
//
//    override def encrypt(message: Bytes, receiverPublicKey: Bytes): Result[Bytes] = c.encrypt(message, receiverPublicKey)
//
//    override def decrypt(decryptionData: Bytes, keyIdentifier: KeyIdentifier): Result[Bytes] = c.decrypt(decryptionData, keyIdentifier)
//
//    //    override def importEncryptionKey(publicKey: Bytes, privateKey: Bytes): Result[String] = c.importEncryptionKey(publicKey, privateKey)
//    //
//    //    override def exportEncryptionKey(keyIdentifier: KeyIdentifier): Result[Bytes] = c.exportEncryptionKey(keyIdentifier: KeyIdentifier)
//
//}
//
//@js.native
//trait CustomEncryptionJS extends js.Object {
//
//    def requestNewKey(): Result[KeyIdentifier] = js.native
//
//    def publicKey(keyIdentifier: KeyIdentifier): Result[UndefOr[Bytes]] = js.native
//
//    def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] = js.native
//
//    def encrypt(message: Bytes, receiverPublicKey: Bytes): Result[Bytes] = js.native
//
//    def decrypt(decryptionData: Bytes, keyIdentifier: KeyIdentifier): Result[Bytes] = js.native
//
//    def importEncryptionKey(publicKey: Bytes, privateKey: Bytes): Result[String] = js.native
//
//    def exportEncryptionKey(keyIdentifier: KeyIdentifier): Result[Bytes] = js.native
//
//}
