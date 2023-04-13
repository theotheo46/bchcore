package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{LogAware, Logger, ROps}
import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.wallet.blocks._
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec.WalletEvents
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
class BlocksProcessing[R[+_]](
    id: WalletIdentity,
    crypto: WalletCrypto[R],
    chain: ChainServiceSpec[R],
)(implicit
    val R: ROps[R],
    val logger: Logger
) extends LogAware {

    private val messagesExtractor = new MessagesExtractor[R](id, crypto, chain)
    private val tokensWithOperationsExtractor = new TokensWithOperationsExtractor[R](id, crypto, chain)
    private val memberEventsExtractor = new MemberEventsExtractor[R](id)
    private val ownerEventsExtractor = new OwnerEventsExtractor[R](id, crypto, chain)
    private val issuerEventsExtractor = new IssuerEventsExtractor[R](id, crypto, chain)
    private val smartContractEventsExtractor = new SmartContractEventsExtractor[R](id, crypto, chain)
    private val dataFeedEventsExtractor = new DataFeedEventsExtractor[R](id)
    private val regulatorEventsExtractor = new RegulatorEventsExtractor[R](id, crypto, chain)
    private val operationsListEventsExtractor = new OperationsListEventsExtractor[R](id, crypto, chain)
    private val profileEventsExtractor = new ProfileEventsExtractor[R](id, crypto, chain)

    def extractEvents(blockNumber: Long, block: BlockEvents, skipSignaturesCheck: Boolean): R[WalletEvents] =
        for {
            messages <- messagesExtractor.extract(block, skipSignaturesCheck)
            tokensWithOperations <- tokensWithOperationsExtractor.extract(block)
            operations <- operationsListEventsExtractor.extract(block, tokensWithOperations)
            member <- memberEventsExtractor.extract(block, messages)
            owner <- ownerEventsExtractor.extract(block, messages, tokensWithOperations)
            issuer <- issuerEventsExtractor.extract(block)
            smartContracts <- smartContractEventsExtractor.extract(block)
            dataFeeds <- dataFeedEventsExtractor.extract(block)
            regulator <- regulatorEventsExtractor.extract(block, skipSignaturesCheck)
            profiles <- profileEventsExtractor.extract(block)

        } yield
            WalletEvents(
                blockNumber,
                member,
                owner,
                issuer,
                smartContracts,
                dataFeeds,
                regulator,
                operations,
                profiles,
            )


    //
    //    private def cancelTokenRequest(incomingMessage: IncomingMessage[CancelTokenRequest]): R[Unit] = {
    //        val cancelRequest = incomingMessage.message
    //        myWalletIdentity.flatMap { myIdentity =>
    //            configuration.store.getAddress(cancelRequest.address).flatMap {
    //                case Some(addressInfo) =>
    //                    if (addressInfo.status != AddressStatus.USED)
    //                        configuration.store.archiveAddress(cancelRequest.address)
    //                    else R(())
    //                case None => R(())
    //            }
    //        }
    //    }
    //
    //    private def updateApplyForOffer(incomingMessage: IncomingMessage[ApplyForOffer]): R[Unit] = {
    //        val applyForOffer = incomingMessage.message
    //        configuration.store.getWalletOffer(applyForOffer.offerId).flatMap {
    //            case Some(walletOffer) =>
    //                configuration.store.putWalletOffer(
    //                    walletOffer.copy(
    //                        offerCandidate = OfferCandidate(
    //                            dealId = applyForOffer.dealId,
    //                            signedTokenBuyer = applyForOffer.signedToken,
    //                            buyerEncryptionKey = incomingMessage.from.encryptionPublic,
    //                            buyerTokenKey = applyForOffer.buyerTokenPublicKey,
    //                            buyerOrgName = applyForOffer.buyerOrgName,
    //                            sellerApprove = None
    //                        ) +: walletOffer.offerCandidate
    //                    )
    //                )
    //            case None => R(()) // ignore non existing offers
    //        }
    //    }
    //
    //
    //    private def updateApproveOffer(incomingMessage: IncomingMessage[ApproveOffer]): R[Unit] = {
    //        val applyForOffer = incomingMessage.message
    //        for {
    //            myId <- myWalletIdentity
    //            maybeWalletPublicAsBytes <- configuration.crypto.encryptionOperations.publicKey(myId.encryptionKey)
    //            walletPublicKeysAsBytes <- R.fromOption(maybeWalletPublicAsBytes, "No wallet public key exists")
    //            maybeWalletOffer <- configuration.store.getWalletOffer(applyForOffer.offerId)
    //            _ <-
    //                maybeWalletOffer match {
    //                    case Some(walletOffer) =>
    //                        configuration.store.putWalletOffer(
    //                            walletOffer.copy(
    //                                offerCandidate = walletOffer.offerCandidate.map { offerCandidate =>
    //                                    if (isEqualBytes(offerCandidate.buyerEncryptionKey, walletPublicKeysAsBytes))
    //                                        offerCandidate.copy(
    //                                            sellerApprove = Some(
    //                                                SellerApprove(
    //                                                    signedTokenSeller = applyForOffer.signedToken,
    //                                                    dealRequest = applyForOffer.dealRequest
    //                                                )
    //                                            )
    //                                        )
    //                                    else offerCandidate
    //                                }
    //                            )
    //                        )
    //
    //                    case None => R(())
    //                }
    //        } yield ()
    //    }

    //    private def createSignedToken(tokenId: String, tokenType: String, content: Collection[String]): R[SignedToken] = {
    //        val tokenBody = TokenBody(tokenId, tokenType, content)
    //        for {
    //            maybeTypeKey <- configuration.store.getTokenTypeKey(tokenType)
    //            typeKey <- R.fromOption(maybeTypeKey, s"No Key for type '$tokenType'")
    //            signature <- configuration.crypto.issuerOperations.createSignature(typeKey, tokenBody.toBytes)
    //        } yield
    //            SignedToken(
    //                tokenBody = tokenBody,
    //                signature = signature
    //            )
    //    }

}
