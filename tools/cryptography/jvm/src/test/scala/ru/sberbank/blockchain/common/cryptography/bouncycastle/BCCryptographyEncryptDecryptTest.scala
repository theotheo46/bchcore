package ru.sberbank.blockchain.common.cryptography.bouncycastle

import org.scalatest.funsuite.AnyFunSuite
import ru.sberbank.blockchain.cnft.commons
import ru.sberbank.blockchain.common.cryptography.store.InMemoryKeysStore

import java.nio.charset.StandardCharsets
import java.util

/**
 * @author Vladimir Sidorov
 */

class BCCryptographyEncryptDecryptTest extends AnyFunSuite {

    val crypto = new BouncyCastleEncryptionOperations(CryptographySettings.AES256GCM, new InMemoryKeysStore)

    test("Crypto System Test") {
        val Message = "Hello world!".getBytes(StandardCharsets.UTF_8)
        val result =
            for {
                keyIdentifier <- crypto.generateECDHKeyPair()
                publicAsBytes <- crypto.publicKey(keyIdentifier)
                encryptedContent <- crypto.encrypt(Message, commons.Collection(publicAsBytes))
                decryptedMessage <- crypto.decrypt(encryptedContent, keyIdentifier)
            } yield decryptedMessage

        assert(result.isRight)
        assert(result.exists(decrypted => util.Arrays.equals(decrypted, Message)))
    }


}
