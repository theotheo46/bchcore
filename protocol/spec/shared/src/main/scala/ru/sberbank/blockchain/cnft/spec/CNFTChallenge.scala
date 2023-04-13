package ru.sberbank.blockchain.cnft.spec

import ru.sberbank.blockchain.cnft.commons.Collection

import java.nio.charset.StandardCharsets

/**
 * @author Alexey Polubelov
 */
object CNFTChallenge {
    def forIdAccess(tokenId: String): Array[Byte] =
        s"ISSUE-$tokenId".getBytes(StandardCharsets.UTF_8)

    def forTokenBurn(tokenId: String): Array[Byte] =
        s"BURN-$tokenId".getBytes(StandardCharsets.UTF_8)

    def freezeTokenByRegulator(freeze: Boolean, tokenIds: Collection[String]): Array[Byte] =
        s"REGULATORY_${if (freeze) "FREEZE" else "UNFREEZE"}-${tokenIds.mkString("[", ", ", "]")}".getBytes(StandardCharsets.UTF_8)

    def changeOwnerByRegulator(tokenId: String): Array[Byte] =
        s"REGULATORY_CHANGE_OWNER-$tokenId".getBytes(StandardCharsets.UTF_8)

    def burnByRegulator(tokenId: String): Array[Byte] =
        s"REGULATORY_BURN-$tokenId".getBytes(StandardCharsets.UTF_8)
}
