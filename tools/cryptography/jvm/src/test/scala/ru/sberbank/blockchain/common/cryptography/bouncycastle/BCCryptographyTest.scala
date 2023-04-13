package ru.sberbank.blockchain.common.cryptography.bouncycastle

import org.scalatest.funsuite.AnyFunSuite
import ru.sberbank.blockchain.common.cryptography.store.InMemoryKeysStore

import java.nio.charset.StandardCharsets

class BCCryptographyTest extends AnyFunSuite {
    private val DummyContent = "hey there!".getBytes(StandardCharsets.UTF_8)

    test("Sign/verify on 256 bit keys should works fine") {
        val cryptography = new BouncyCastleSignatureOperations(CryptographySettings.EC256_SHA256, new InMemoryKeysStore)
        val isValid = for {
            keyIdentifier <- cryptography.requestNewKey()
            publicAsBytes <- cryptography.publicKey(keyIdentifier)
            signature <- cryptography.createSignature(keyIdentifier, DummyContent)
            isValid <- cryptography.verifySignature(publicAsBytes, DummyContent, signature)
        } yield isValid
        assert(isValid.contains(true))
    }

    test("Sign/verify on 512 bit keys should works fine") {
        val cryptography = new BouncyCastleSignatureOperations(CryptographySettings.EC256_SHA256, new InMemoryKeysStore)
        val isValid = for {
            keyIdentifier <- cryptography.requestNewKey()
            publicAsBytes <- cryptography.publicKey(keyIdentifier)
            signature <- cryptography.createSignature(keyIdentifier, DummyContent)
            isValid <- cryptography.verifySignature(publicAsBytes, DummyContent, signature)
        } yield isValid
        assert(isValid.contains(true))
    }

}
