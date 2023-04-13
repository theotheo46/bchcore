package ru.sberbank.blockchain.cnft.gate

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.{PEMKeyPair, PEMParser}
import org.enterprisedlt.fabric.client.AppUser
import org.hyperledger.fabric.sdk.identity.X509Enrollment
import org.slf4j.LoggerFactory

import java.io.{File, FileReader}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.PrivateKey
import java.util.{Base64, Collections}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/**
 * @author Maxim Fedin
 */
object Util {

    private val logger = LoggerFactory.getLogger(this.getClass)

    //    def parse[A <: GeneratedMessage with Message[A]](content: Array[Byte], p: GeneratedMessageCompanion[A]): Either[String, A] =
    //        Try(p.parseFrom(content)).toEither.left.map(t => s"Failed to parse ${p.getClass.getSimpleName}: ${t.getMessage}")

    def decodeB64(b64: String, name: String): Either[String, Array[Byte]] =
        Try(Base64.getDecoder.decode(b64)).toEither.left.map(t => s"Failed to decode $name: ${t.getMessage}")

    def encodeB64(data: Array[Byte], name: String): Either[String, String] =
        Try(new String(Base64.getEncoder.encode(data), StandardCharsets.UTF_8))
            .toEither.left.map(t => s"Failed to encode $name: ${t.getMessage}")

    def toB64(data: Array[Byte]): String = new String(Base64.getEncoder.encode(data), StandardCharsets.UTF_8)

    def loadFabricUser(orgName: String, crtMspPath: String, keyMspPath: String): AppUser = {
        val signedCert = getSignedCertFromFile(crtMspPath)
        val privateKey = getPrivateKeyFromFile(keyMspPath)
        val adminEnrollment = new X509Enrollment(privateKey, signedCert)
        AppUser("user1", Collections.emptySet(), "", "", adminEnrollment, orgName)
    }

    def getPrivateKeyFromFile(filePath: String): PrivateKey = {
        val fileName = new File(filePath)
        val pemReader = new FileReader(fileName)
        val pemParser = new PEMParser(pemReader)
        try {
            pemParser.readObject() match {
                case pemKeyPair: PEMKeyPair => new JcaPEMKeyConverter().getKeyPair(pemKeyPair).getPrivate
                case keyInfo: PrivateKeyInfo => new JcaPEMKeyConverter().getPrivateKey(keyInfo)
                case null => throw new Exception(s"Unable to read PEM object")
                case other => throw new Exception(s"Unsupported PEM object ${other.getClass.getCanonicalName}")
            }
        } finally {
            pemParser.close()
            pemReader.close()
        }
    }

    def getSignedCertFromFile(filePath: String): String = {
        val fileName = new File(filePath)
        val r = Files.readAllBytes(Paths.get(fileName.toURI))
        new String(r, StandardCharsets.UTF_8)
    }

    def try2EitherWithLogging[T](obj: => T): Either[String, T] = {
        Try(obj) match {
            case Success(something) => Right(something)
            case Failure(err) =>
                val msg = s"Error: ${err.getMessage}"
                logger.error(msg, err)
                Left(msg)
        }
    }


    def environmentMandatory(name: String): String =
        sys.env.getOrElse(name,
            throw new Exception(s"Mandatory environment variable $name is missing.")
        )

    def environmentOptional(name: String, default: String): String =
        sys.env.getOrElse(name, default)

    def stringToDuration(s: String): Duration = scala.concurrent.duration.Duration.apply(s)

}