package ru.sberbank.blockchain.common.cryptography.hd

import ru.sberbank.blockchain.cnft.common.types.{Bytes, Collection, asBytes}
import ru.sberbank.blockchain.cnft.commons.{LogAware, Logger, ROps, asByteArray, collectionFromIterable, isEqualBytes}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.PublicKeyAlgorithm.HD
import ru.sberbank.blockchain.common.cryptography.model.{ChallengeSpec, HDKey}
import ru.sberbank.blockchain.common.cryptography.{Bip32, HDPath, HDPathStore, SignatureOperations}

import java.math.BigInteger
import scala.language.higherKinds
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Vladimir Sidorov
 */
@JSExportAll
@JSExportTopLevel("HDSignatureOperations")
class HDSignatureOperations[R[+_], ECPoint](
    pathStore: HDPathStore[R],
    cryptoImpl: Bip32[R, ECPoint],
    seed: HDKey,
    rootId: Option[Int]
)(implicit
    R: ROps[R],
    val logger: Logger
) extends SignatureOperations[R] with LogAware {

    import ROps._

    private val rootKey: BigInteger = new BigInteger(1, asByteArray(seed.k))
    private val cc: Bytes = seed.cc

    override def requestNewKey(): R[KeyIdentifier] =
        pathStore.getCurrentKeyPath.map { keyIdentifier =>
            val incPath = HDPath.getIncrementFromKeyIdentifier(keyIdentifier)
            pathStore.saveKeyPath(incPath)
            incPath
        }

    override def createSignature(keyPath: KeyIdentifier, content: Bytes): R[Bytes] = {
        val hdKeyPath = HDPath.fromKeyIdentifier(keyPath)
        for {
            derivedPath <- derivePath(hdKeyPath)
            signedContent <- cryptoImpl.sign(derivedPath._1, content)
        } yield signedContent
    }

    override def verifySignature(keyBytes: Bytes, content: Bytes, signature: Bytes): R[Boolean] =
        for {
            public <- R(ChallengeSpec.parseFrom(asByteArray(keyBytes)))
            result <- cryptoImpl.verify(public.value, content, signature)
        } yield result

    override def publicKey(keyPath: KeyIdentifier): R[Bytes] = {
        for {
            hdPath <- R(HDPath.fromKeyIdentifier(keyPath))
            derivedPath <- derivePath(hdPath)
            publicKey <- cryptoImpl.publicForPrivate(derivedPath._1)
        } yield
            asBytes(
                ChallengeSpec(
                    algorithm = HD,
                    value = publicKey,
                    extra = s"m/${pathStore.rootId}/${hdPath._1}/${hdPath._2}"
                ).toByteArray
            )
    }

    override def keyExists(keyIdentifier: KeyIdentifier): R[Boolean] = R {
        true
    }

    override def findKeysByPublic(publicKeyBytes: Collection[Bytes]): R[Collection[KeyIdentifier]] =
        publicKeyBytes.toSeq.mapR { publicKeyByte =>
            for {
                pubKey <- R(ChallengeSpec.parseFrom(asByteArray(publicKeyByte)))
                hdPath <- R(HDPath.fromKeyIdentifier(pubKey.extra))
                derivedPath <- derivePath(hdPath)
                publicKey <- cryptoImpl.publicForPrivate(derivedPath._1)
                equalStatus = isEqualBytes(publicKey, pubKey.value)
                path <-
                    if (equalStatus) {
                        val keyIdentifier = pubKey.extra.split("/")
                        val keyLength = keyIdentifier.length
                        updatePathStore(hdPath).map { _ =>
                            Some(s"${keyIdentifier(keyLength - 2)}/${keyIdentifier(keyLength - 1)}")
                        }
                    } else R(None)
            } yield path
        }
            .map(_.flatten)
            .recover { t =>
                logger.error(s"findKeysByPublic: ignoring invalid key ($t)")
                R(Seq.empty[KeyIdentifier])
            }
            .map(collectionFromIterable)


    def derivePath(hdKeyPath: HDPath): R[(BigInteger, Bytes)] =
        for {
            root <-
                rootId
                    .map(cryptoImpl.derivePrivateKey(rootKey, cc, _))
                    .getOrElse(R((rootKey, cc)))

            derivedKeyFirst <- cryptoImpl.derivePrivateKey(root._1, root._2, hdKeyPath._1)
            derivedKeySecond <- cryptoImpl.derivePrivateKey(derivedKeyFirst._1, derivedKeyFirst._2, hdKeyPath._2)
        } yield derivedKeySecond

    private def updatePathStore(path: HDPath): R[Unit] =
        for {
            currentPath <- pathStore.getCurrentKeyPath
            currentHdPath = HDPath.fromKeyIdentifier(currentPath)
            maxPath = HDPath.comparePaths(currentHdPath, path)
            _ <- if (!((maxPath._1 == currentHdPath._1) && (maxPath._2 == currentHdPath._2))) {
                pathStore.saveKeyPath(s"m/${pathStore.rootId}/${maxPath._1}/${maxPath._2}")
            } else {
                R(())
            }
        } yield ()

    override def exportData(): R[Bytes] = R(asBytes(seed.toByteArray))

}
