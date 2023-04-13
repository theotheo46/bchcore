package ru.sberbank.blockchain.common.cryptography

import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import tools.http.service.annotations.{HttpGet, HttpHeaderValue, HttpPost}

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
object Cryptography {
    type KeyIdentifier = String
}

//
// Signing
//

trait SignatureOperations[R[+_]] {

    @HttpGet("/request-new-key")
    @HttpHeaderValue("Content-type", "application/xml")
    def requestNewKey(): R[KeyIdentifier]

    @HttpPost("/public-key")
    @HttpHeaderValue("Content-type", "application/xml")
    def publicKey(keyIdentifier: KeyIdentifier): R[Bytes]

    @HttpPost("/key-exists")
    @HttpHeaderValue("Content-type", "application/xml")
    def keyExists(keyIdentifier: KeyIdentifier): R[Boolean]

    @HttpPost("/create-signature")
    @HttpHeaderValue("Content-type", "application/xml")
    def createSignature(key: KeyIdentifier, content: Bytes): R[Bytes]

    @HttpPost("/verify-signature")
    @HttpHeaderValue("Content-type", "application/xml")
    def verifySignature(key: Bytes, content: Bytes, signature: Bytes): R[Boolean]

    @HttpPost("/find-keys-by-public")
    @HttpHeaderValue("Content-type", "application/xml")
    def findKeysByPublic(publicKeyBytes: Collection[Bytes]): R[Collection[KeyIdentifier]]

    def exportData(): R[Bytes]
}

trait SignatureOperationsFactory[R[+_]] {

    def create(identity: String): R[SignatureOperations[R]]

    def importFrom(identity: String, data: Bytes): R[SignatureOperations[R]]
}

trait Hash[R[+_]] {

    def sha256(content: Bytes): R[Bytes]

    def sha1(content: Bytes): R[Bytes]
}

trait HashFactory[R[+_]] {

    def create: R[Hash[R]]
}

trait SecureRandomGenerator {
    def nextInt(): Int

    def nextBytes(num: Int): Bytes
}

//
// Secure Random Generator
//

trait SecureRandomGeneratorFactory {

    def create: SecureRandomGenerator
}

//
// Encryption
//

trait EncryptionOperations[R[+_]] {

    def requestNewKey(): R[KeyIdentifier]

    def publicKey(keyIdentifier: KeyIdentifier): R[Bytes]

    def keyExists(keyIdentifier: KeyIdentifier): R[Boolean]

    def encrypt(message: Bytes, receiverPublicKeys: Collection[Bytes]): R[Bytes]

    def decrypt(decryptionData: Bytes, keyIdentifier: KeyIdentifier): R[Bytes]

    def exportData(): R[Bytes]
}

trait EncryptionOperationsFactory[R[+_]] {

    def create(identity: String): R[EncryptionOperations[R]]

    def importFrom(identity: String, data: Bytes): R[EncryptionOperations[R]]
}

//
// Access token create/verify
//

trait AccessOperations[R[+_]] {

    def publicKey(): R[Bytes]

    def isPublicValid(public: Bytes): R[Unit]

    def createAccessToken(content: Bytes, keys: Collection[Bytes]): R[Bytes]

    def exportData(): R[Bytes]
}

trait AccessOperationsFactory[R[+_]] {

    def create(identity: String): R[AccessOperations[R]]

    def importFrom(identity: String, data: Bytes): R[AccessOperations[R]]
}
