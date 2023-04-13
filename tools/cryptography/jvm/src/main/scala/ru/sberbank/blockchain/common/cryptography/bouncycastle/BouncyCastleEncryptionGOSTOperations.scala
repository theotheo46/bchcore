//package ru.sberbank.blockchain.common.cryptography.bouncycastle
//
//import org.bouncycastle.cms.jcajce.{JceCMSContentEncryptorBuilder, JceKeyTransEnvelopedRecipient, JceKeyTransRecipientInfoGenerator}
//import org.bouncycastle.cms.{CMSAlgorithm, CMSEnvelopedData, CMSEnvelopedDataGenerator, CMSProcessableByteArray}
//import org.bouncycastle.jce.provider.BouncyCastleProvider
//import ru.sberbank.blockchain.cnft.commons.{Base64R, Bytes, LoggingSupport, Result}
//import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
//import ru.sberbank.blockchain.common.cryptography.{CryptographicKeysStore, EncryptionOperations}
//
//import java.io.ByteArrayInputStream
//import java.math.BigInteger
//import java.security._
//import java.security.cert.{CertificateFactory, X509Certificate}
//import java.security.spec.{ECGenParameterSpec, PKCS8EncodedKeySpec}
//import java.util.Date
//import java.util.concurrent.TimeUnit
//
///**
// * @author Alexey Polubelov
// */
//class BouncyCastleEncryptionGOSTOperations(
//    settings: EncryptDecryptCryptographySettings,
//    val keysStore: CryptographicKeysStore
//) extends BouncyCastleCryptoOpsBase with EncryptionOperations[Result] with LoggingSupport {
//
//    // NOTE: keep the line below (registration of BC provider) at top of this class:
//    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(new BouncyCastleProvider)
//
//    private val random = new SecureRandom()
//
//    override def requestNewKey(): Result[KeyIdentifier] =
//        for {
//            keyPair <- Result {
//                val curve = new ECGenParameterSpec(settings.ECCurve)
//                val keyFactory = KeyPairGenerator.getInstance(settings.ECAlgorithm)
//                keyFactory.initialize(curve, random)
//                keyFactory.generateKeyPair()
//            }
//            certificate <- Result {
//                val subject = new org.bouncycastle.asn1.x500.X500Name("CN=" + "ServiceCertificate");
//                val issuer = subject; // self-signed
//                val serial = BigInteger.ONE; // serial number for self-signed does not matter a lot
//                val notBefore = new Date();
//                val notAfter = new Date(notBefore.getTime + TimeUnit.DAYS.toMillis(365));
//
//                val certificateBuilder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
//                    issuer, serial,
//                    notBefore, notAfter,
//                    subject, keyPair.getPublic
//                )
//                val certificateHolder = certificateBuilder.build(
//                    new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("GOST3411WITHGOST3410-2012-256")
//                      .build(keyPair.getPrivate)
//                )
//                val certificateConverter = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter();
//
//                certificateConverter.getCertificate(certificateHolder)
//            }
//
//            privateKeyEncoded = keyPair.getPrivate.getEncoded
//            publicKeyB64 <- Base64R.encode(certificate.getEncoded) // put certificate!
//
//            _ <- Result(keysStore.save(publicKeyB64, privateKeyEncoded))
//
//        } yield publicKeyB64
//
//    override def encrypt(message: Bytes, receiverPublicKey: Bytes): Result[Bytes] = Result {
//        val cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator
//        val cert = CertificateFactory
//          .getInstance("X.509", "BC")
//          .generateCertificate(new ByteArrayInputStream(receiverPublicKey)).asInstanceOf[X509Certificate]
//        val jceKey = new JceKeyTransRecipientInfoGenerator(cert)
//        cmsEnvelopedDataGenerator.addRecipientInfoGenerator(jceKey);
//        val msg = new CMSProcessableByteArray(message);
//        val encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.GOST28147_GCFB).setProvider("BC").build();
//        val cmsEnvelopedData = cmsEnvelopedDataGenerator.generate(msg, encryptor);
//        val encryptedData = cmsEnvelopedData.getEncoded
//        new CMSEnvelopedData(encryptedData).getEncoded
//    }
//
//
//    override def decrypt(decryptionData: Bytes, keyIdentifier: KeyIdentifier): Result[Bytes] =
//        for {
//            privKeyBytes <- keysStore.get(keyIdentifier).toRight(s"[Decrypt] Unknown key: $keyIdentifier")
//            privKey <- Result {
//                val keyFactory = KeyFactory.getInstance(settings.ECAlgorithm)
//                val privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes)
//                keyFactory.generatePrivate(privateKeySpec)
//            }
//            decyptedData <- Result {
//                val envelopedData = new CMSEnvelopedData(decryptionData)
//                val recipients = envelopedData.getRecipientInfos.getRecipients.iterator()
//                recipients.next().getContent(new JceKeyTransEnvelopedRecipient(privKey))
//            }
//        } yield decyptedData
//
////    override def importEncryptionKey(publicKey: Bytes, privateKey: Bytes): Result[String] =
////        Left("Unsupported method")
////
////    override def exportEncryptionKey(keyIdentifier: KeyIdentifier): Result[Bytes] =
////        Left("Unsupported method")
//
//}
