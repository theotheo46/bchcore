package ru.sberbank.blockchain.cnft.megacuks

import ru.sberbank.blockchain.cnft.commons.Bytes
import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

/**
 * @author Andrew Pudovikov
 */
case class MegaCuksKey(
    identifier: KeyIdentifier,
    publicAsBytes: Bytes,
    publicAsString: String
)
