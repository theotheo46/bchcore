import org.bouncycastle.crypto.modes.gcm.GCMUtil.asBytes
import org.scalatest.funsuite.AnyFunSuite
import ru.sberbank.blockchain.cnft.commons.{Bytes, LoggingSupport, isEqualBytes}
import ru.sberbank.blockchain.cnft.wallet.CNFTCrypto
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.cnft.wallet.dsl._

import java.nio.charset.StandardCharsets


class MegaCuksCryptoTest extends AnyFunSuite with LoggingSupport {

    val host_url = "http://localhost:9191"

    val mcs_sign_ops = CNFTCrypto.remoteSignatureOperations(host_url).create("")
        .orFail(s"can not create sing ops")
    var keyIdentifier: KeyIdentifier = _
    var publicKey: Bytes = _
    val content = asBytes(Array(0, 1, 2, 3))
    var signature: Bytes = _

    test("client requests new key") {

        val maybeKey = mcs_sign_ops.requestNewKey()
        keyIdentifier = maybeKey.orFail("can not get key Identifier")
        logger.info(s"${keyIdentifier}")
    }

    test("client tries get public for unknown key idendifier") {
        val unknownKey = mcs_sign_ops.publicKey("abc").expectFail(0)
        logger.info(s"${unknownKey}")
    }

    test("client get public for key known idendifier") {
        val maybePublicKey = mcs_sign_ops.publicKey(keyIdentifier).orFail("can not get public key")
        logger.info(s"${maybePublicKey}")
        publicKey = maybePublicKey //.getOrElse(Bytes.empty)
    }

    test("client check if key exist") {
        val nonExistingPublicKey = mcs_sign_ops.publicKey("abc")
        logger.info(s"check non existing idendifier: ${nonExistingPublicKey}")

        val existingPublicKey = mcs_sign_ops.publicKey(keyIdentifier)
        logger.info(s"check existing idendifier: ${existingPublicKey}")
    }

    test("client create signature") {
        signature = mcs_sign_ops.createSignature(keyIdentifier, content).orFail("can not create signature")
        val mockSignature = "signature".getBytes(StandardCharsets.UTF_8)
        logger.info(s"signature: ${signature.mkString(" ")}")
        assert(isEqualBytes(signature, mockSignature))
    }

    test("client verify signature") {
        val check = mcs_sign_ops.verifySignature(publicKey, content, signature).orFail("can not create signature")
        logger.info(s"signature: ${check}")
        assert(check)
    }

}