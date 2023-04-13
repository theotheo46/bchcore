package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.model.TokenContent
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Fail, scenarioContext}
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIssueTokenRequest

class StepsIssueTokens extends ScalaDsl with EN with LoggingSupport {

    When("""{string} issued token {string} for {string} of type {string} with value {string}""") {
        (issuerName: String, tokenName: String, clientName: String, tokenTypeName: String, tokenValue: String) =>

            logger.info(s"Issuing token of type $tokenTypeName for $clientName ...")
            val issuerWallet = scenarioContext.wallets.getOrElse(issuerName, Fail(s"Unknown client $issuerName"))
            val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val maybeTokenType = scenarioContext.investmentTokenType.get(tokenTypeName)
            val addr =
                clientWallet
                    .createAddress
                    .orFail(s"Failed to create  address for token $tokenName")

            val maybeTokenId = maybeTokenType.map { tt =>
                clientWallet.createTokenId(tt).orFail("failed to create TokenId")
            }
            scenarioContext.tokenAddress.put((clientName, tokenName), (addr, maybeTokenId))

            val owner = clientWallet.createSingleOwnerAddress
                .orFail(s"Failed to reserve id for $clientName")
            val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
                if (events.owner.tokensReceived.nonEmpty)
                    Some(events.owner.tokensReceived)
                else
                    None
            }
            maybeTokenId.map { tokenId =>
                issuerWallet
                    .issue(
                        Collection(
                            WalletIssueTokenRequest(
                                tokenId,
                                owner,
                                TokenContent(
                                    Collection(tokenValue)
                                ),
                                clientWallet.getIdentity.orFail("no identity")
                            )
                        )
                    )
                    .orFail(s"Failed to issue token for $clientName")
                scenarioContext.tokenIdByName.put(tokenName, tokenId)
                val added = await.get
                added.foreach { tokenId =>
                    logger.info(s"Issued token of type $tokenTypeName for $clientName ($tokenId).")
                }
            }
    }

    When("""{string} issued token {string} for address of {string} of type {string} with value {string}""") {
        (issuerName: String, tokenName: String, clientName: String, tokenTypeName: String, tokenValue: String) =>

            logger.info(s"Issuing token of type $tokenTypeName for $clientName ...")
            val issuerWallet = scenarioContext.wallets.getOrElse(issuerName, Fail(s"Unknown client $issuerName"))
            val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val owner = scenarioContext.tokenOwnerAddress.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val maybeTokenType = scenarioContext.investmentTokenType.get(tokenTypeName)
            val addr =
                clientWallet
                    .createAddress
                    .orFail(s"Failed to create  address for token $tokenName")

            val maybeTokenId = maybeTokenType.map { tt =>
                clientWallet.createTokenId(tt).orFail("failed to create TokenId")
            }
            scenarioContext.tokenAddress.put((clientName, tokenName), (addr, maybeTokenId))


            val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
                if (events.owner.tokensReceived.nonEmpty)
                    Some(events.owner.tokensReceived)
                else
                    None
            }
            maybeTokenId.map { tokenId =>
                issuerWallet
                    .issue(
                        Collection(
                            WalletIssueTokenRequest(
                                tokenId,
                                owner,
                                TokenContent(
                                    Collection(tokenValue)
                                ),
                                clientWallet.getIdentity.orFail("no identity")
                            )
                        )
                    )
                    .orFail(s"Failed to issue token for $clientName")
                scenarioContext.tokenIdByName.put(tokenName, tokenId)
                val added = await.get
                added.foreach { tokenId =>
                    logger.info(s"Issued token of type $tokenTypeName for $clientName ($tokenId).")
                }
            }
    }

    When("""{string} issued tokens {string} in quantity {int} for {string} of type {string} with value {string}""") {
        (issuerName: String, tokenNamePrefix: String, quantity: Int, clientName: String, tokenTypeName: String, tokenValue: String) =>

            for (tokenNumber <- 0 until quantity) {

                val tokenName = s"${tokenNamePrefix}_$tokenNumber"
                logger.info(s"Issuing token $tokenName of type $tokenTypeName for $clientName ...")
                val issuerWallet = scenarioContext.wallets.getOrElse(issuerName, Fail(s"Unknown client $issuerName"))
                val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

                val owner = clientWallet.createSingleOwnerAddress
                    .orFail(s"Failed to reserve id for $clientName")
                val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
                    if (events.owner.tokensReceived.nonEmpty)
                        Some(events.owner.tokensReceived)
                    else
                        None
                }

                val maybeTokenType = scenarioContext.tokenTypeIdByName.get(issuerName)

                val addr =
                    clientWallet
                        .createAddress
                        .orFail(s"Failed to create  address for token $tokenName")

                val maybeTokenId = maybeTokenType.map { tt =>
                    clientWallet.createTokenId(tt).orFail("failed to create TokenId")
                }
                scenarioContext.tokenAddress.put((clientName, tokenName), (addr, maybeTokenId))
                val tokenId = maybeTokenId.getOrElse(Fail("failed to get token Id"))

                issuerWallet
                    .issue(
                        Collection(
                            WalletIssueTokenRequest(
                                tokenId,
                                owner,
                                TokenContent(
                                    Collection(tokenValue)
                                ),
                                clientWallet.getIdentity.orFail("no identity")
                            )
                        )
                    )
                    .orFail(s"Failed to issue token for $clientName")
                scenarioContext.tokenIdByName.put(tokenName, tokenId)
                val added = await.get
                added.foreach { tokenId =>
                    logger.info(s"Issued token of type $tokenTypeName for $clientName ($tokenId).")
                }
            }

    }

    When("""{string} issued token {string} for {string} of type {string} with value {string} with regulation""") {
        (issuerName: String, tokenName: String, clientName: String, tokenTypeName: String, tokenValue: String) =>

            logger.info(s"Issuing token of type $tokenTypeName for $clientName ...")
            val issuerWallet = scenarioContext.wallets.getOrElse(issuerName, Fail(s"Unknown client $issuerName"))
            val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val Regulator = scenarioContext.wallets.getOrElse("Regulator", Fail("No Regulator"))
            val owner = clientWallet.createSingleOwnerAddress
                .orFail(s"Failed to reserve id for $clientName")
            val await = Util.awaitWalletEvents(Regulator) { case (_, events) =>
                if (events.regulator.pendingIssues.nonEmpty) Some(events.regulator.pendingIssues) else None
            }
            val maybeTokenType = scenarioContext.tokenTypeIdByName.get(issuerName)
            val addr =
                clientWallet
                    .createAddress
                    .orFail(s"Failed to create  address for token $tokenName")

            val maybeTokenId = maybeTokenType.map { tt =>
                clientWallet.createTokenId(tt).orFail("failed to create TokenId")
            }
            scenarioContext.tokenAddress.put((clientName, tokenName), (addr, maybeTokenId))

            maybeTokenId.map { tokenId =>
                issuerWallet
                    .issue(
                        Collection(
                            WalletIssueTokenRequest(
                                tokenId,
                                owner,
                                TokenContent(
                                    Collection(tokenValue)
                                ),
                                clientWallet.getIdentity.orFail("no identity")
                            )
                        )
                    )
                    .orFail(s"Failed to issue token for $clientName")
                scenarioContext.tokenIdByName.put(tokenName, tokenId)
                await.get
                logger.info(s"Issued token of type $tokenTypeName for $clientName ($tokenId).")
            }
    }

    When("""{string} issued token {string} for {string} of type {string} with value {string} with regulation {string}""") {
        (issuerName: String, tokenName: String, clientName: String, tokenTypeName: String, tokenValue: String, regulator: String) =>

            logger.info(s"Issuing token of type $tokenTypeName for $clientName ...")
            val issuerWallet = scenarioContext.wallets.getOrElse(issuerName, Fail(s"Unknown client $issuerName"))
            val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val Regulator = scenarioContext.wallets.getOrElse(regulator, Fail("No Regulator"))
            val owner = clientWallet.createSingleOwnerAddress
                .orFail(s"Failed to reserve id for $clientName")
            val await = Util.awaitWalletEvents(Regulator) { case (_, events) =>
                if (events.regulator.pendingIssues.nonEmpty) Some(events.regulator.pendingIssues) else None
            }
            val maybeTokenType = scenarioContext.tokenTypeIdByName.get(issuerName)
            val addr =
                clientWallet
                    .createAddress
                    .orFail(s"Failed to create  address for token $tokenName")

            val maybeTokenId = maybeTokenType.map { tt =>
                clientWallet.createTokenId(tt).orFail("failed to create TokenId")
            }
            scenarioContext.tokenAddress.put((clientName, tokenName), (addr, maybeTokenId))

            maybeTokenId.map { tokenId =>
                issuerWallet
                    .issue(
                        Collection(
                            WalletIssueTokenRequest(
                                tokenId,
                                owner,
                                TokenContent(
                                    Collection(tokenValue)
                                ),
                                clientWallet.getIdentity.orFail("no identity")
                            )
                        )
                    )
                    .orFail(s"Failed to issue token for $clientName")
                scenarioContext.tokenIdByName.put(tokenName, tokenId)
                await.get
                logger.info(s"Issued token of type $tokenTypeName for $clientName (${tokenId}).")
            }
    }
}
