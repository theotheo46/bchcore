package ru.sberbank.blockchain.common.cryptography.bouncycastle

import org.bouncycastle.util.encoders.Hex
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * @author Vladimir Sidorov
 */

class BCCryptographyWebCryptoTest extends AnyFunSuite {
    private val publickKey = "-----BEGIN PUBLIC KEY-----\nMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEVIciRD7nZqsEh+aPmfWtPPGSLspYsI4K\n0yzkkC5Ddzdt08qZkytzjeGBEBJyX+RBPwL0vE5jxPgBndT2uEOXgvVQdfjr7n5n\n/1cZ5RCLXeEZWdJjn2jvDrO5QrdL1Te4\n-----END PUBLIC KEY-----"
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\n", "")
        .filterNot(_.isWhitespace)

    private val signature = """3065023100f5787bfc9054876f147b6d8ede8e4d6a428fa125a406eb4c0f76c147a9715ff622941ffa2126335e32efbbc85c46fdbc0230391b6acaf46ed699364d09dbc5a825dcc03ba9c0aed2ca4403f450767e87f784646cbffe7580f35ac58c608a63192b6a"""
    private val message = "The eagle flies at twilight"

    def decodeBase64(encoded: String): Array[Byte] = {
        Base64.getDecoder.decode(encoded)
    }

    test("Verify WebCrypto signature") {
        val cryptography = new BCCryptographyVerifyOnly(CryptographySettings.EC256_SHA256)
        val isValid = for {
            isValid <- cryptography.verifySignature(decodeBase64(publickKey), message.getBytes(StandardCharsets.UTF_8), Hex.decode(signature))
        } yield isValid
        assert(isValid.contains(true))
    }
}
