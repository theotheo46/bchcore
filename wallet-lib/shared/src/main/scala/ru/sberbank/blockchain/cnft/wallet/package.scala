package ru.sberbank.blockchain.cnft

import ru.sberbank.blockchain.cnft.wallet.walletmodel._
import upickle.default._

/**
 * @author Alexey Polubelov
 */
package object wallet {

    // keep the import below for correct Collections and Bytes serialization
    import ru.sberbank.blockchain._

    implicit val WalletIdentityRW: ReadWriter[WalletIdentity] = macroRW
    implicit val WalletOfferRW: ReadWriter[WalletOffer] = macroRW
    implicit val OfferCandidateRW: ReadWriter[OfferCandidate] = macroRW
    implicit val SellerApproveRW: ReadWriter[SellerApprove] = macroRW
    implicit val RegulatoryBurnTokenRequestRW: ReadWriter[RegulatoryBurnTokenRequest] = macroRW
    implicit val FreezeInfoRW: ReadWriter[FreezeInfo] = macroRW
    implicit val CreateProfileInfoRW: ReadWriter[CreateProfileInfo] = macroRW
    implicit val WalletIssueTokenRequestRW: ReadWriter[WalletIssueTokenRequest] = macroRW
    implicit val AddressInfoRW: ReadWriter[AddressInfo] = macroRW
    implicit val SCRejectedResultRW: ReadWriter[SCRejectedResult] = macroRW

}
