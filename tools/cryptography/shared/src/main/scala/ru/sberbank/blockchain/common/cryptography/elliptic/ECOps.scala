package ru.sberbank.blockchain.common.cryptography.elliptic

import ru.sberbank.blockchain.cnft.commons.Bytes

import java.math.BigInteger
import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
trait ECOps[R[+_], ECPoint] {
    def compare(p: ECPoint, o: ECPoint): Boolean

    def serialize(p: ECPoint): Bytes

    def deserialize(a: Bytes): ECPoint

    def multECpoints(p: ECPoint, k: BigInteger): ECPoint

    def addECPoints(p: ECPoint, o: ECPoint): ECPoint

    def getG: ECPoint

    def getQ: BigInteger

    def generatePair(): R[(BigInteger, ECPoint)]

    def sign(key: BigInteger, data: Bytes): R[Bytes]

    def verify(key: ECPoint, data: Bytes, signature: Bytes): R[Boolean]
}
