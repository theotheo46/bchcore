package ru.sberbank.blockchain.common.cryptography.bouncycastle

import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers

/**
 * @author Alexey Polubelov
 */


case class CryptographySettings(
    keyAlgorithm: String,
    keyLength: Int,
    signatureAlgorithm: String
)

case class EncryptDecryptCryptographySettings(
    symmetricKeyAlgorithm: String,
    symmetricKeyCipherAlgorithm: String,
    symmetricKeyLength: Int,
    symmetricTagLength: Int,
    ECAlgorithm: String,
    ECCurve: String
)


object CryptographySettings {
    val EC256_SHA256: CryptographySettings = CryptographySettings("EC", 256, "SHA256withECDSA")
    val EC512_SHA512: CryptographySettings = CryptographySettings("EC", 512, "SHA512withECDSA")

    val AES256GCM: EncryptDecryptCryptographySettings =
        EncryptDecryptCryptographySettings(
            symmetricKeyAlgorithm = "AES",
            symmetricKeyCipherAlgorithm = NISTObjectIdentifiers.id_aes256_GCM.toString,
            symmetricKeyLength = 256,
            symmetricTagLength = 128,
            ECAlgorithm = "ECDH",
            ECCurve = "P-256")

    val GOSTKeyParams: EncryptDecryptCryptographySettings =
        EncryptDecryptCryptographySettings(
            symmetricKeyAlgorithm = "GOST28147/CBC/PKCS5Padding",
            symmetricKeyCipherAlgorithm = CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_A_ParamSet.toString,
            symmetricKeyLength = 256,
            symmetricTagLength = 128,
            ECAlgorithm = "ECGOST3410-2012",
            ECCurve = "Tc26-Gost-3410-12-256-paramSetA")

}

