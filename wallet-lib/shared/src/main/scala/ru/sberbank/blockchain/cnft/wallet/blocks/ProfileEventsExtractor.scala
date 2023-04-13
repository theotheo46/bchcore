package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{ROps, collectionFromArray, collectionFromIterable}
import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.model.ProfileTokens
import ru.sberbank.blockchain.cnft.wallet.WalletCommonOps
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec.ProfileEvents
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity

import scala.language.higherKinds

class ProfileEventsExtractor[R[+_]](
    val id: WalletIdentity,
    val crypto: WalletCrypto[R],
    val chain: ChainServiceSpec[R],
)(implicit val R: ROps[R]) extends WalletCommonOps[R] {

    /** CAUTION!!!
     *
     * This function uses store for saving some information.
     */
    def extract(block: BlockEvents): R[ProfileEvents] =
        for {
            created <-
                R(
                    block
                        .profilesCreated
                        .map(_.event)
                        .filter(_.memberId == id.id)
                        .map(_.id)
                ).map(collectionFromArray)

            updated <-
                R(
                    block
                        .profilesUpdated
                        .map(_.event)
                        .filter(_.memberId == id.id)
                        .map(_.id)
                ).map(collectionFromArray)

            tokensLinked <- block
                .tokensLinkedToProfile
                .map(_.event)
                .toSeq
                .filterR { case ProfileTokens(profileId, _) =>
                    chain
                        .getProfile(profileId)
                        .map(_.memberId == id.id)
                }
                .map(collectionFromIterable)

            tokensUnlinked <- block
                .tokensUnlinkedFromProfile
                .map(_.event)
                .toSeq
                .filterR { case ProfileTokens(profileId, _) =>
                    chain
                        .getProfile(profileId)
                        .map(_.memberId == id.id)
                }
                .map(collectionFromIterable)

        } yield
            ProfileEvents(
                created,
                updated,
                tokensLinked,
                tokensUnlinked
            )

}
