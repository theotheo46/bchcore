//package ru.sberbank.blockchain.cnft.wallet
//
//import ru.sberbank.blockchain.cnft.common.types.{Collection, Optional}
//import ru.sberbank.blockchain.cnft.gate.model.TxResult
//import ru.sberbank.blockchain.cnft.model.{Offer, TokenDescription}
//import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletOffer
//
//import scala.language.higherKinds
//
///**
// * @author Alexey Polubelov
// */
//class Offers[R[+_]](wallet: CNFTWalletInternal[R]) {
//
//    //    import wallet._
//    //
//    //
//    // Buy and sell tokens
//    //
//
//    def putOffer(supply: TokenDescription, demand: TokenDescription): R[TxResult[Offer]] = ???
//    //        for {
//    //            response <- chainTx.putOffers(Collection(PutOfferRequest(id.id, supply, demand)))
//    //            result <- expectOne(response, "Invalid putOffer result")
//    //        } yield result
//
//    def listTokenSupplyCandidates(offerId: String): R[Collection[String]] = ???
//    //        for {
//    //            maybeWalletOffer <- configuration.store.getWalletOffer(offerId)
//    //            walletOffer <- R.fromOption(maybeWalletOffer, s"No such offer $offerId")
//    //            tokens <- listTokens
//    //        } yield tokens
//    //            .filter(_.regulations.forall(_.frozen == false))
//    //            .filter { token =>
//    //                token.signedToken.tokenBody.tokenType == walletOffer.offer.demand.tokenType &&
//    //                    walletOffer.offer.demand.content.forall {
//    //                        demandedField =>
//    //                            token.signedToken.tokenBody.content(demandedField.index) == demandedField.value
//    //                    }
//    //            }
//
//    def listTokenDemandCandidates(offerId: String): R[Collection[String]] = ???
//    //        for {
//    //            maybeWalletOffer <- configuration.store.getWalletOffer(offerId)
//    //            walletOffer <- R.fromOption(maybeWalletOffer, s"No such offer $offerId")
//    //            tokens <- listTokens
//    //        } yield tokens
//    //            .filter(_.regulations.forall(_.frozen == false))
//    //            .filter { token =>
//    //                token.signedToken.tokenBody.tokenType == walletOffer.offer.supply.tokenType &&
//    //                    walletOffer.offer.supply.content.forall { supplyField =>
//    //                        token.signedToken.tokenBody.content(supplyField.index) == supplyField.value
//    //                    }
//    //            }
//
//
//    def approveOffer(offerId: String, dealId: String, signedToken: String): R[Unit] = ???
//    //        configuration.store.execTransactional {
//    //            for {
//    //                myId <- myWalletIdentity
//    //                maybeMyKey <- configuration.crypto.encryptionOperations.publicKey(myId.encryptionKey)
//    //                myEncryptionKey <- R.fromOption(maybeMyKey, "Encryption key for Identity is missing")
//    //                maybeWalletOffer <- configuration.store.getWalletOffer(offerId)
//    //                walletOffer <- R.fromOption(maybeWalletOffer, "No walletOffer received")
//    //                keyIdentifier <- createKey
//    //                _ <- _createSingleOwnerAddress(keyIdentifier)
//    //                maybePublicAsBytes <- configuration.crypto.tokenOperations.publicKey(keyIdentifier)
//    //                publicAsBytes <- R.fromOption(maybePublicAsBytes, "publicAsBytes haven't found")
//    //                offerCandidate <- R.fromOption(walletOffer.offerCandidate.find(_.dealId == dealId), "No proper offer candidate exists")
//    //                buyer <- configuration.gate.getMember(offerCandidate.buyerOrgName)
//    //                deal = Deal(
//    //                    dealId = dealId,
//    //                    legs = Collection(
//    //                        signedToken.tokenBody.tokenId ->
//    //                            TokenOwner(require = 1, Collection(offerCandidate.buyerTokenKey)),
//    //
//    //                        offerCandidate.signedTokenBuyer.tokenBody.tokenId ->
//    //                            TokenOwner(require = 1, Collection(publicAsBytes))
//    //                    )
//    //                )
//    //                maybeKey <- configuration.store.getTokenIdKey(signedToken.tokenBody.tokenId)
//    //                keyForSign <- R.fromOption(maybeKey, s"No such key for tokenId ${signedToken.tokenBody.tokenId}")
//    //                dealSignature <- configuration.crypto.tokenOperations.createSignature(keyForSign, deal.toBytes)
//    //                signatureForIssuer <- makeEncryptedMembersSignature(deal, signedToken.tokenBody.tokenType, myId.signingKey)
//    //                signature =
//    //                    DealSignature(
//    //                        tokenId = signedToken.tokenBody.tokenId,
//    //                        ownerSignature = dealSignature,
//    //                        memberSignature = signatureForIssuer
//    //                    )
//    //                approveOfferRequest = ApproveOffer(
//    //                    offerId = offerId,
//    //                    signedToken = signedToken,
//    //                    dealRequest = DealRequest(
//    //                        deal = deal,
//    //                        signatures = Collection(signature)
//    //                    )
//    //                )
//    //                sellerApprove = SellerApprove(
//    //                    signedTokenSeller = approveOfferRequest.signedToken,
//    //                    dealRequest = approveOfferRequest.dealRequest
//    //                )
//    //                _ <- configuration.store.putWalletOffer(
//    //                    walletOffer.copy(
//    //                        offerCandidate = walletOffer.offerCandidate.map { offerCandidate =>
//    //                            if (isEqualBytes(offerCandidate.buyerEncryptionKey, buyer.encryptionPublic))
//    //                                offerCandidate.copy(sellerApprove = Some(sellerApprove))
//    //                            else offerCandidate
//    //                        }
//    //                    )
//    //                )
//    //                _ <- publishMessage(offerCandidate.buyerOrgName, approveOfferRequest)
//    //            } yield {}
//    //        }
//
//
//    def finalizeOffer(offerId: String, dealId: String): R[Unit] = ???
//    //        for {
//    //            maybeWalletOffer <- configuration.store.getWalletOffer(offerId)
//    //            walletOffer <- R.fromOption(maybeWalletOffer, "No walletOffer received")
//    //            myId <- myWalletIdentity
//    //            maybeWalletPublicAsBytes <- configuration.crypto.encryptionOperations.publicKey(myId.encryptionKey)
//    //            walletPublicKeysAsBytes <- R.fromOption(maybeWalletPublicAsBytes, "No wallet public key exist")
//    //            offerCandidate <-
//    //                R.fromOption(
//    //                    walletOffer.offerCandidate.find(offerCandidate => isEqualBytes(offerCandidate.buyerEncryptionKey, walletPublicKeysAsBytes) && offerCandidate.dealId == dealId),
//    //                    "No proper offer candidate exists")
//    //            maybeKey <- configuration.store.getTokenIdKey(offerCandidate.signedTokenBuyer.tokenBody.tokenId)
//    //            keyIdentifier <- R.fromOption(maybeKey, s"No such key for tokenId ${offerCandidate.signedTokenBuyer.tokenBody.tokenId}")
//    //            dealRequest <- R.fromOption(offerCandidate.sellerApprove.map(_.dealRequest), "No dealRequest in offer candidate")
//    //            dealSignature <- configuration.crypto.tokenOperations.createSignature(keyIdentifier, dealRequest.deal.toBytes)
//    //            signatureForIssuer <- makeEncryptedMembersSignature(
//    //                entity = dealRequest.deal,
//    //                tokenType = offerCandidate.signedTokenBuyer.tokenBody.tokenType,
//    //                signKey = myId.signingKey)
//    //            signature =
//    //                DealSignature(
//    //                    tokenId = offerCandidate.signedTokenBuyer.tokenBody.tokenId,
//    //                    ownerSignature = dealSignature,
//    //                    memberSignature = signatureForIssuer
//    //                )
//    //            updatedDealRequest = dealRequest.copy(signatures = signature +: dealRequest.signatures)
//    //            response <- configuration.gate.makeDeal(updatedDealRequest)
//    //        } yield response.value
//
//
//    def closeOffer(offerId: String): R[TxResult[Unit]] = ???
//    //        chainTx.closeOffers(Collection(offerId))
//
//    def listOffers: R[Collection[WalletOffer]] = ???
//
//    def getOffer(offerId: String): R[Optional[WalletOffer]] = ???
//
//}
