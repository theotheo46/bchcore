package ru.sberbank.blockchain.common.cryptography.bouncycastle

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.{ECDomainParameters, ECKeyGenerationParameters, ECPrivateKeyParameters, ECPublicKeyParameters}
import org.bouncycastle.crypto.signers.{ECDSASigner, HMacDSAKCalculator}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.math.ec.ECPoint
import ru.sberbank.blockchain.cnft.commons.{Bytes, LoggingSupport, Result, concatBytes}
import ru.sberbank.blockchain.common.cryptography.elliptic.ECOps
import ru.sberbank.blockchain.common.cryptography.sag.Hasher

import java.math.BigInteger
import java.security._

class EllipticOps(
    curve: ECNamedCurveParameterSpec,
    compressed: Boolean = true
) extends ECOps[Result, ECPoint] with LoggingSupport {
    // NOTE: keep the line below (registration of BC provider) at top of this class:
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(new BouncyCastleProvider)

    override def compare(p: ECPoint, o: ECPoint): Boolean = p.equals(o)

    override def serialize(p: ECPoint): Bytes = p.getEncoded(compressed)

    override def deserialize(a: Bytes): ECPoint = curve.getCurve.decodePoint(a)

    override def multECpoints(p: ECPoint, k: BigInteger): ECPoint = p.multiply(k)

    override def addECPoints(p: ECPoint, o: ECPoint): ECPoint = p.add(o)

    override def getG: ECPoint = curve.getG

    override def getQ: BigInteger = curve.getCurve.getOrder

    private val One = BigInteger.valueOf(1)
    private val HalfCurveOrder = curve.getN.shiftRight(1)

    //
    //
    //

    private def createGenerator(): ECKeyPairGenerator = {
        val kpGen = new ECKeyPairGenerator()
        kpGen.init(
            new ECKeyGenerationParameters(
                new ECDomainParameters(
                    curve.getCurve,
                    curve.getG,
                    curve.getN,
                    curve.getH
                ),
                new SecureRandom()
            )
        )
        kpGen
    }

    private val keyPairGenerator = createGenerator()

    override def generatePair(): Result[(BigInteger, ECPoint)] = Result {
        val kp: AsymmetricCipherKeyPair = keyPairGenerator.generateKeyPair()
        (
            kp.getPrivate.asInstanceOf[ECPrivateKeyParameters].getD,
            kp.getPublic.asInstanceOf[ECPublicKeyParameters].getQ
        )
    }

    override def sign(key: BigInteger, data: Bytes): Result[Bytes] = Result {
        val signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest))
        val params = new ECDomainParameters(curve.getCurve, curve.getG, curve.getN, curve.getH)
        val privateKeyParameters = new ECPrivateKeyParameters(key, params)
        signer.init(true, privateKeyParameters)
        val Array(r, s) = signer.generateSignature(data)
        val (r1, s1) = // if s > N/2 then s = N - s
            if (s.compareTo(HalfCurveOrder) > 0) (r, curve.getN.subtract(s))
            else (r, s)

        leftPad32(r1.toByteArray.dropWhile(_ == 0)) ++
            leftPad32(s1.toByteArray.dropWhile(_ == 0))
    }

    override def verify(key: ECPoint, data: Bytes, signature: Bytes): Result[Boolean] = {
        val keyHex = new BigInteger(1, key.getEncoded(false)).toString(16)
        val dataHex = new BigInteger(1, data).toString(16)
        val (r, s) = decodeSignatureCompact(signature)
        for {
            _ <- Result.expect(r.compareTo(One) >= 0, "Invalid signature: r must be >= 1")
            _ <- Result.expect(r.compareTo(curve.getN) < 0, "Invalid signature: r must be < N")
            _ <- Result.expect(s.compareTo(One) >= 0, "Invalid signature: s must be >= 1")
            _ <- Result.expect(s.compareTo(curve.getN) < 0, "Invalid signature: s must be < N")

            signer = new ECDSASigner
            params =
                new ECPublicKeyParameters(key,
                    new ECDomainParameters(
                        curve.getCurve,
                        curve.getG,
                        curve.getN,
                        curve.getH
                    )
                )

            result <- Result {
                signer.init(false, params)
                signer.verifySignature(data, r, s)
            }
        } yield {
            logger.trace(
                "\nVERIFY " +
                    s"Data: $dataHex " +
                    s"Key: $keyHex " +
                    s"R: ${r.toString(16)} " +
                    s"S: ${s.toString(16)} " +
                    s"Result: $result"
            )
            result
        }
    }

    private def decodeSignatureCompact(signature: Bytes): (BigInteger, BigInteger) = {
        val r = new BigInteger(1, signature.take(32))
        val s = new BigInteger(1, signature.takeRight(32))
        (r, s)
    }

    private def leftPad32(data: Bytes): Bytes = {
        val dataLengthDelta = 32 - data.length
        val padByteArray = new Array[Byte](dataLengthDelta)
        padByteArray.map { _ => 0.toByte } ++ data
    }

}

object BouncyCastleHasher extends Hasher[Result] {
    override def hash(bytes: Seq[Bytes]): Result[Bytes] = Result {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.digest(concatBytes(bytes))
    }

}
