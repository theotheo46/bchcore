package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.model.{Profile, ProfileTokens}
import ru.sberbank.blockchain.cnft.wallet.walletmodel.CreateProfileInfo

import scala.language.higherKinds

class Profiles[R[+_]](wallet: CNFTWalletInternal[R]) extends LoggingSupport {

    import wallet._

    def createProfile(profile: CreateProfileInfo): R[TxResult[Profile]] =
        for {
            profileId <- createId
            CreateProfileInfo(name, description, avatar, background) = profile
            result <- chainTx.createProfile(
                Profile(
                    profileId,
                    name,
                    description,
                    avatar,
                    background,
                    id.id
                )
            )
        } yield result

    def updateProfile(profile: Profile): R[TxResult[Profile]] = chainTx.updateProfile(profile)

    def listProfiles: R[Collection[Profile]] =
        chain.listProfiles.map(_.filter(_.memberId == id.id))

    def linkTokensToProfile(profileTokens: ProfileTokens): R[TxResult[Unit]] =
        for {
            _ <- checkProfileOwnership(profileTokens.profileId)
            _ <- checkTokensOwnership(profileTokens.tokenIds)
            result <- chainTx.linkTokensToProfile(profileTokens)
        } yield result

    def unlinkTokensFromProfile(profileTokens: ProfileTokens): R[TxResult[Unit]] =
        for {
            _ <- checkProfileOwnership(profileTokens.profileId)
            _ <- checkTokensOwnership(profileTokens.tokenIds)
            result <- chainTx.unlinkTokensFromProfile(profileTokens)
        } yield result

    private def checkProfileOwnership(profileId: String): R[Unit] =
        for {
            profile <- chain.getProfile(profileId)
            _ <- R.expect(profile.memberId == id.id, "Trying to link Not my profile address")
        } yield ()

    private def checkTokensOwnership(tokenIds: Collection[String]): R[Unit] =
        for {
            _ <- tokenIds
                .toSeq
                .mapR { tokenId =>
                    for {
                        tokenOwner <- chain.getTokenOwner(tokenId)
                        ownerId = tokenOwner.toBytes
                        isMyAddress <- isMyAddress(ownerId)
                        _ <- R.expect(isMyAddress, s"Not my token: $tokenId")
                    } yield ()
                }
        } yield ()

}
