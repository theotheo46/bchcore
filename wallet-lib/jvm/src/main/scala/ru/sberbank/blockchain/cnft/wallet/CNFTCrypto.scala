package ru.sberbank.blockchain.cnft.wallet

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.math.ec.ECPoint
import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, LoggingSupport, Result, asByteArray, asBytes}
import ru.sberbank.blockchain.cnft.wallet.crypto.CryptographyContext
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography._
import ru.sberbank.blockchain.common.cryptography.bouncycastle._
import ru.sberbank.blockchain.common.cryptography.hd.{Bip32ELOps, HDSignatureOperations}
import ru.sberbank.blockchain.common.cryptography.model.{HDKey, KeysList}
import ru.sberbank.blockchain.common.cryptography.store.InMemoryKeysStore
import tools.http.service.HttpService._
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64

/**
 * @author Alexey Polubelov
 */
object CNFTCrypto extends LoggingSupport {
    private val random = new SecureRandom()

    def newContext(
        identityOpsFactory: SignatureOperationsFactory[Result],
        encryptionOpsFactory: EncryptionOperationsFactory[Result],
        accessOpsFactory: AccessOperationsFactory[Result],
        addressOpsFactory: SignatureOperationsFactory[Result],
        hashFactory: HashFactory[Result],
        randomGeneratorFactory: SecureRandomGeneratorFactory
    ): CryptographyContext[Result] =
        new CryptographyContext[Result](
            identityOpsFactory, encryptionOpsFactory,
            accessOpsFactory, addressOpsFactory,
            hashFactory, randomGeneratorFactory
        )

    //    def bouncyCastleSign(keyStore: CryptographicKeysStore): SignatureOperationsFactory[Result] =
    //        new SignatureOperationsFactory[Result] {
    //            override def create(): Result[SignatureOperations[Result]] = Result {
    //                new BouncyCastleSignatureOperations(CryptographySettings.EC256_SHA256, keyStore)
    //            }
    //
    //            override def importFrom(config: Bytes): Result[SignatureOperations[Result]] = ???
    //        }

    def bouncyCastleEncryption(): EncryptionOperationsFactory[Result] = {
        new EncryptionOperationsFactory[Result] {

            override def create(identity: String): Result[EncryptionOperations[Result]] = Result {
                new BouncyCastleEncryptionOperations(CryptographySettings.AES256GCM, new InMemoryKeysStore)
            }

            override def importFrom(identity: String, data: Bytes): Result[EncryptionOperations[Result]] =
                Result(KeysList.parseFrom(data)).map { list =>
                    val store = new InMemoryKeysStore
                    list.keys.foreach { key =>
                        store.save(
                            new String(Base64.getEncoder.encode(key.pk), StandardCharsets.UTF_8),
                            key.sk
                        )
                    }
                    new BouncyCastleEncryptionOperations(CryptographySettings.AES256GCM, store)
                }
        }
    }

    def staticKeySignatureOperations(keyId: KeyIdentifier): SignatureOperationsFactory[Result] = {
        new SignatureOperationsFactory[Result] {
            override def create(identity: String): Result[SignatureOperations[Result]] = Result {
                new SignatureOperations[Result] {
                    override def requestNewKey(): Result[KeyIdentifier] = Result(keyId)

                    override def publicKey(keyIdentifier: KeyIdentifier): Result[Bytes] = Result.Fail("Shell never be called")
                    override def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] = Result.Fail("Shell never be called")
                    override def createSignature(key: KeyIdentifier, content: Bytes): Result[Bytes] = Result.Fail("Shell never be called")
                    override def verifySignature(key: Bytes, content: Bytes, signature: Bytes): Result[Boolean] = Result.Fail("Shell never be called")
                    override def findKeysByPublic(publicKeyBytes: Collection[Bytes]): Result[Collection[KeyIdentifier]] = Result.Fail("Shell never be called")
                    override def exportData(): Result[Bytes] = Result(Bytes.empty)
                }
            }

            override def importFrom(identity: String, data: Bytes): Result[SignatureOperations[Result]] = Result.Fail("Shell never be called")
        }
    }

    def hdSignatureOperations(pathStore: HDPathStoreFactory[Result]): SignatureOperationsFactory[Result] = {
        val curve: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val cryptoImpl = new Bip32ELOps(curve)

        def randomBytes(l: Int): Bytes = {
            val seedKeyBytes = new Array[Byte](l)
            random.nextBytes(seedKeyBytes)
            asBytes(seedKeyBytes)
        }


        new SignatureOperationsFactory[Result] {
            override def create(identity: String): Result[SignatureOperations[Result]] = Result {
                val ps = pathStore.newStoreFor(identity)
                new HDSignatureOperations(
                    pathStore.newStoreFor(identity),
                    cryptoImpl,
                    HDKey(
                        k = randomBytes(32),
                        cc = randomBytes(32)
                    ),
                    Some(ps.rootId.toInt)
                )
            }


            override def importFrom(identity: String, data: Bytes): Result[SignatureOperations[Result]] =
                for {
                    key <- Result(HDKey.parseFrom(asByteArray(data)))
                    ps = pathStore.newStoreFor(identity)
                    ops <-
                      Result {
                          new HDSignatureOperations(
                              pathStore.newStoreFor(identity),
                              cryptoImpl,
                              key,
                              Some(ps.rootId.toInt)
                          )
                      }
                } yield ops
        }
    }

    def remoteSignatureOperations(url: String): SignatureOperationsFactory[Result] = {
        new SignatureOperationsFactory[Result] {

            override def create(identity: String): Result[SignatureOperations[Result]] = Result {
                createService[SignatureOperations[Result]](url)
            }

            override def importFrom(identity: String, data: Bytes): Result[SignatureOperations[Result]] = Result {
                createService[SignatureOperations[Result]](url)
            }
        }
    }

    def bouncyCastleAccessOperations(): AccessOperationsFactory[Result] =
        new AccessOperationsFactoryImpl[Result, ECPoint](
            BouncyCastleHasher,
            new EllipticOps(
                ECNamedCurveTable.getParameterSpec("secp256k1"),
                compressed = false
            )
        )

    def hash(): HashFactory[Result] =
        new HashFactory[Result] {
            override def create: Result[Hash[Result]] =
                Result(BouncyCastleHash)
        }

    def secureRandomGenerator(): SecureRandomGeneratorFactory =
        new SecureRandomGeneratorFactory {
            override def create: SecureRandomGenerator = JSecureRandomGenerator
        }
}
