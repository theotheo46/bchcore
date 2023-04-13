package ru.sberbank.blockchain.cnft

import ru.sberbank.blockchain.cnft.wallet.CNFTCrypto
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.store.HDPathStore

import java.util.Base64
import scala.sys.exit

/**
 * @author Alexey Polubelov
 */
object WalletGen extends App {
    val usage =
    """
    Usage: wallet-generator [--key] [value]
    --help - display this message
    --signOpsKey - input key identifier for wallet_crypto

    Examples:
    1) wallet-generator - default, generates wallet_crypto for hd config.
    2) wallet-generator --signOpsKey VALUE - generates wallet_crypto with VALUE as key identifier.
    """

    if (args.length != 0 && args(0) == "--help") {
        println(usage)
        exit(1)
    }

    val argMap = args.sliding(2, 2).toList.map {
        case Array("--signOpsKey", v: String) => "signOpsKey" -> v
    }.toMap

    val context =
        CNFTCrypto
            .newContext(
                argMap.get("signOpsKey")
                    .map { key =>
                        CNFTCrypto.staticKeySignatureOperations(key)
                    }
                    .getOrElse {
                        CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory())
                    },
                CNFTCrypto.bouncyCastleEncryption(),
                CNFTCrypto.bouncyCastleAccessOperations(),
                CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                CNFTCrypto.hash(),
                CNFTCrypto.secureRandomGenerator()
            )

    val crypto = context.create().orFail("Failed to create crypto")
    val data = crypto.exportData().orFail("Failed to export crypto")

    val b64 = Base64.getEncoder.encodeToString(data)
    println(b64)
}
