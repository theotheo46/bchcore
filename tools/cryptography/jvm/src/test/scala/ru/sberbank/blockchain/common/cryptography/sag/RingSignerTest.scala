package ru.sberbank.blockchain.common.cryptography.sag

import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.{ECDomainParameters, ECKeyGenerationParameters, ECPrivateKeyParameters, ECPublicKeyParameters}
import org.bouncycastle.crypto.{AsymmetricCipherKeyPair, CryptoServicesRegistrar}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.BigIntegers
import org.scalatest.funsuite.AnyFunSuite
import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, LoggingSupport, Result, BytesOps}
import ru.sberbank.blockchain.common.cryptography.bouncycastle.{BouncyCastleHasher, EllipticOps}

import java.math.BigInteger
import java.security.{SecureRandom, Security}
import java.util.Base64
import scala.reflect.ClassTag

class RingSignerTest extends AnyFunSuite with LoggingSupport {

    private val curveSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
    private val ecOps = new EllipticOps(curveSpec)

    private def toB64(data: Array[Byte]): String = Base64.getEncoder.encode(data).toUTF8

    private implicit class RExt[+T](value: Result[T]) {
        @inline def orFail(msg: String): T =
            value match {
                case Right(value) => value
                case Left(error) => fail(s"$msg: $error")
            }

        @inline def expectFail: String =
            value match {
                case Right(_) => fail("Expected fail")
                case Left(error) => error
            }

    }

    test("check is pub key valid") {
        val numOfSigners = 2
        val (privKeys, pubKeys) = GenerateKeyPairs(numOfSigners)

        val myPrivate = privKeys(0)
        val myPublic = ecOps.deserialize(pubKeys(0))

        val rs = new RingSigner[Result, ECPoint](
            BouncyCastleHasher,
            ecOps,
            (myPrivate, myPublic)
        )

        rs.isPublicKeyValid(pubKeys(1)).orFail("result should be true")
        logger.info(s"public key verification result is OK as expected")

        rs.isPublicKeyValid(s"some strange string".getBytes()).expectFail
        logger.info(s"incorrect public key verification result is Not OK as expected")

    }

    test("verify valid signature") {

        val numOfSigners = 10
        val (privKeys, pubKeys) = GenerateKeyPairs(numOfSigners)
        val message = getTestMessage

        for (signerIndex <- 0 until numOfSigners) {

            val myPrivate = privKeys(signerIndex)
            val myPublic = ecOps.deserialize(pubKeys(signerIndex))

            val rs = new RingSigner[Result, ECPoint](
                BouncyCastleHasher,
                ecOps,
                (myPrivate, myPublic)
            )
            val signature =
                rs.sign(message, pubKeys)
                    .orFail("failed to sign")

            logger.info(s"signer id: $signerIndex e: ${toB64(signature)}")

            val isValid = rs.verify(message, pubKeys, signature).orFail("Failed to verify")
            logger.info(s"verification result $isValid")
            assert(isValid)
        }
    }

    test("verify invalid signature") {

        val numOfSigners = 11
        val (privKeys, pubKeys) = GenerateKeyPairs(numOfSigners)
        val message = getTestMessage

        val signerIndex = 3

        val signKeys = pubKeys.slice(0, 10)

        val myPrivate = privKeys(signerIndex)
        val myPublic = ecOps.deserialize(signKeys(signerIndex))

        val rs = new RingSigner[Result, ECPoint](
            BouncyCastleHasher,
            ecOps,
            (myPrivate, myPublic)
        )

        val signature =
            rs.sign(message, signKeys)
                .orFail("failed to sign")
        logger.info(s"signer id: $signerIndex e: ${toB64(signature)}")

        val verifyKeys = pubKeys.slice(1, 11)

        val isValid = rs.verify(message, verifyKeys, signature).orFail("Failed to verify")
        logger.info(s"verification result $isValid")
        assert(!isValid)
    }

    test("sag verifier no signer key in the ring ") {
        val numOfSigners = 10
        val message = getTestMessage
        val pubKeys = GenerateKeyPairs(numOfSigners)._2

        val (myPrivate, myPublic) = Secp256k1.generateKeyPair()

        val rs = new RingSigner[Result, ECPoint](
            BouncyCastleHasher,
            ecOps,
            (myPrivate, myPublic)
        )

        val msg =
            rs
                .sign(message, pubKeys)
                .expectFail

        println(s"Failed with msg: $msg")
    }

    test("sag verifier if less than 2 keys in ring signature shall be null") {
        val numOfSigners = 1
        val message = getTestMessage
        val RingKeys = GenerateKeyPairs(numOfSigners)._2

        val (myPrivate, myPublic) = Secp256k1.generateKeyPair()

        val rs = new RingSigner[Result, ECPoint](BouncyCastleHasher,
            ecOps,
            (myPrivate, myPublic))

        val msg =
            rs
                .sign(message, RingKeys)
                .expectFail

        println(s"Failed with msg: $msg")
    }

    // getTestMessage produce test message byte array
    def getTestMessage: Array[Byte] = {
        "test message".getBytes
    }

    // GenerateKeyPairs generate array of (private, public) key pairs
    def GenerateKeyPairs(numOfKeys: Int): (Collection[BigInteger], Collection[Bytes]) = {

        val keys =
            for (_ <- 0 until numOfKeys)
                yield {
                    val (priv, pubEC) = Secp256k1.generateKeyPair()

                    (priv, pubEC.getEncoded(true))
                }

        (
            keys.map(_._1).toArray,
            keys.map(_._2).toArray
        )
    }

    @inline def collectionFromSequence[T: ClassTag](v: scala.collection.Seq[T]): Collection[T] = v.toArray

    //  implicit def SequenceToCollection[T: ClassTag](a: IndexedSeq[T]): Collection[T] = collectionFromSequence(a)

}

object Secp256k1 {
    Security.addProvider(new BouncyCastleProvider)

    val ECSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
    val G: ECPoint = ECSpec.getG
    val q: BigInteger = ECSpec.getCurve.getOrder

    private val keyPairGenerator = createGenerator()

    private def createGenerator(): ECKeyPairGenerator = {
        val kpGen = new ECKeyPairGenerator()
        kpGen.init(
            new ECKeyGenerationParameters(
                new ECDomainParameters(
                    ECSpec.getCurve,
                    ECSpec.getG,
                    ECSpec.getN,
                    ECSpec.getH
                ),
                new SecureRandom()
            )
        )
        kpGen
    }

    def generateKeyPair(): (BigInteger, ECPoint) = {
        val kp: AsymmetricCipherKeyPair = keyPairGenerator.generateKeyPair()
        (
            kp.getPrivate.asInstanceOf[ECPrivateKeyParameters].getD,
            kp.getPublic.asInstanceOf[ECPublicKeyParameters].getQ
        )
    }

    def randNonce: BigInteger = {
        val ECSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        BigIntegers.createRandomInRange(BigInteger.ONE, ECSpec.getN.subtract(BigInteger.ONE), CryptoServicesRegistrar.getSecureRandom)
    }

}