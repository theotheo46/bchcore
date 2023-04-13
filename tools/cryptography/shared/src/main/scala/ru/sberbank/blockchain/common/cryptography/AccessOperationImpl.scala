package ru.sberbank.blockchain.common.cryptography

import ru.sberbank.blockchain.cnft.commons.ROps.summonHasOps
import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, ROps, asByteArray, asBytes, collectionFromIterable}
import ru.sberbank.blockchain.common.cryptography.elliptic.ECOps
import ru.sberbank.blockchain.common.cryptography.model.RingDecoy
import ru.sberbank.blockchain.common.cryptography.sag.{Hasher, RingSigner}

import java.math.BigInteger
import scala.language.higherKinds
import scala.math.abs

/**
 * @author Alexey Polubelov
 */
class AccessOperationImpl[R[+_], P](
    hasher: Hasher[R],
    ecOps: ECOps[R, P],
    key: (BigInteger, P)
)(implicit
    R: ROps[R]
) extends AccessOperations[R] {
    private val signer = new RingSigner[R, P](hasher, ecOps, key)
    private val MyPublicKey = ecOps.serialize(key._2)

    override def publicKey(): R[Bytes] = R(MyPublicKey)

    override def isPublicValid(public: Bytes): R[Unit] =
        signer.isPublicKeyValid(public)

    override def createAccessToken(content: Bytes, keys: Collection[Bytes]): R[Bytes] =
        signer.sign(content, keys)

    override def exportData(): R[Bytes] = R {
        asBytes(key._1.toByteArray)
    }

}


class AccessOperationsFactoryImpl[R[+_], P](
    hasher: Hasher[R],
    ecOps: ECOps[R, P]
)(implicit R: ROps[R]) extends AccessOperationsFactory[R] {

    import ROps._

    override def create(identity: String): R[AccessOperations[R]] =
        ecOps.generatePair().map { key =>
            new AccessOperationImpl[R, P](hasher, ecOps, key)
        }

    override def importFrom(identity: String, data: Bytes): R[AccessOperations[R]] = R {
        val key = new BigInteger(1, asByteArray(data))
        val p = ecOps.multECpoints(ecOps.getG, key)
        new AccessOperationImpl[R, P](hasher, ecOps, (key, p))
    }
}

class DecoyProvider[R[+_]](implicit R: ROps[R]) {

    import ru.sberbank.blockchain.cnft.common.types.BytesOps

    def decoyKeys(ids: Bytes, allKeys: Collection[Bytes]): R[Collection[Bytes]] = R {
        val decoyIndexes = RingDecoy.parseFrom(asByteArray(ids))
        val decoyKeys = allKeys.zipWithIndex.filter(e => decoyIndexes.ids.contains(e._2)).map(_._1)
        decoyKeys
    }

    def generateDecoy(myPublic: Bytes, allKeys: Collection[Bytes], decoyLength: Int, secureRandomGenerator: SecureRandomGenerator): R[(Collection[Bytes], Bytes)] =
        if (allKeys.length <= 1)
            R((Collection.empty, Bytes.empty))
        else
            R.fromOption(
                allKeys
                    .zipWithIndex
                    .find { case (k, _) => k =?= myPublic }
                    .map(_._2), s"can not find access public key")
                .map { myPublicId =>
                    val totalKeys = allKeys.length
                    val decoyIndexes =
                        collectionFromIterable(
                            if (totalKeys <= decoyLength)
                                allKeys.indices
                            else
                                Stream.iterate(myPublicId)(_ => abs(secureRandomGenerator.nextInt()) % totalKeys)
                                    .distinct
                                    .take(decoyLength)
                                    .sorted
                        )

                    (decoyIndexes.map(allKeys),
                        asBytes(RingDecoy(ids = decoyIndexes).toByteArray))
                }

}