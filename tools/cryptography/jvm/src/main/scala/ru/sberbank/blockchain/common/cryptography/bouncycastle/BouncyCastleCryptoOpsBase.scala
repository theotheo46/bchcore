package ru.sberbank.blockchain.common.cryptography.bouncycastle

import ru.sberbank.blockchain.cnft.commons.{Base64R, Bytes, Result}
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier
import ru.sberbank.blockchain.common.cryptography.store.CryptographicKeysStore

/**
 * @author Alexey Polubelov
 */
private[bouncycastle] trait BouncyCastleCryptoOpsBase {
    def keysStore: CryptographicKeysStore

    def publicKey(keyIdentifier: KeyIdentifier): Result[Bytes] =
        keyExists(keyIdentifier).flatMap { exist =>
            if (exist) Base64R.decode[Result](keyIdentifier)
            else Result.Fail(s"Unknown key: $keyIdentifier")
        }

    def keyExists(keyIdentifier: KeyIdentifier): Result[Boolean] = Result {
        !keysStore.get(keyIdentifier).forall(_.isEmpty)
    }

}
