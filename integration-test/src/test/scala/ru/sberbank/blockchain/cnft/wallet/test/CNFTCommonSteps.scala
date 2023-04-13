package ru.sberbank.blockchain.cnft.wallet.test

import ru.sberbank.blockchain.cnft.common.types.{Collection, Result}
import ru.sberbank.blockchain.cnft.model._
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.spec.CNFTWalletSpec
import ru.sberbank.blockchain.cnft.wallet.test.CNFTUtil._
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIssueTokenRequest

import scala.collection.mutable

object CNFTCommonSteps {

    trait ScenarioContext

    trait WalletsContext extends ScenarioContext {

        val wallets: mutable.Map[String, CNFTWalletSpec[Result]] = mutable.HashMap.empty

        val walletIdentities: mutable.Map[String, String] = mutable.HashMap.empty[String, String]

        def getWalletIdentityByClientName(clientName: String): Result[String] =
            walletIdentities.get(clientName).toRight(s"Unknown client '$clientName'")

        def withWallet(clientName: String)(f: CNFTWalletSpec[Result] => Unit): Unit =
            f(
                wallets
                    .get(clientName)
                    .toRight(s"Unknown client '$clientName'")
                    .orFail(s"Unknown client")
            )

    }

    trait TokenTypesContext extends ScenarioContext {

        val tokenTypeIds: mutable.Map[String, String] = mutable.HashMap.empty

        def withTokenType(tokenTypeName: String)(f: String => Unit): Unit =
            f(
                tokenTypeIds
                    .get(tokenTypeName)
                    .toRight(s"Unknown token type name '$tokenTypeName'")
                    .orFail("Unknown token type")
            )

    }

    trait TokensContext extends ScenarioContext {

        val tokenIds: mutable.Map[String, String] = mutable.HashMap.empty

        def withToken(tokenName: String)(f: String => Unit): Unit =
            f(
                tokenIds
                    .get(tokenName)
                    .toRight(s"Unknown token name '$tokenName'")
                    .orFail("Unknown token")
            )

    }

    def thereIsAClient(
        context: ScenarioContext with WalletsContext,
        clientName: String
    ): Unit = {
        val crypto = createWalletCrypto
        registerMemberHook(crypto)
        val wallet = Chain.newWallet(crypto).orFail(s"Failed to create wallet for '$clientName'")
        val walletIdentity = wallet.getIdentity.orFail(s"Failed to get wallet identity for '$clientName'")
        context.wallets.put(clientName, wallet)
        context.walletIdentities.put(clientName, walletIdentity)
        logger.info(s"Created wallet for client $clientName")
    }

    def clientRegistersTokenType(
        context: ScenarioContext with WalletsContext with TokenTypesContext,
        clientName: String,
        tokenTypeName: String,
        gene: Option[Gene] = None
    ): Unit =
        context.withWallet(clientName) { wallet =>
            (for {
                tokenTypeId <- wallet.createId
                dnaChange =
                    Collection(gene).collect {
                        case Some(gene) => gene
                    }
            } yield {
                context.tokenTypeIds.put(tokenTypeName, tokenTypeId)
                CNFTUtil.registerEmittableTokenType(wallet, tokenTypeId, tokenTypeName, dnaChange).value
            })
                .orFail("Failed to create id for token type")
            ()
        }

    def clientCreatesIdForTokenOfType(
        context: ScenarioContext with WalletsContext with TokenTypesContext with TokensContext,
        clientName: String,
        tokenName: String,
        tokenTypeName: String
    ): Unit =
        context.withWallet(clientName) { wallet =>
            context.withTokenType(tokenTypeName) { tokenTypeId =>
                (for {
                    tokenId <- wallet.createTokenId(tokenTypeId)
                } yield context.tokenIds.put(tokenName, tokenId))
                    .orFail("Failed to create token id")
                ()
            }
        }

    def clientIssuesTokenForClientWithValue(
        context: ScenarioContext with WalletsContext with TokensContext,
        issuerName: String,
        clientName: String,
        tokenName: String,
        tokenValue: String
    ): Unit =
        context.withWallet(issuerName) { issuerWallet =>
            context.withWallet(clientName) { clientWallet =>
                context.withToken(tokenName) { tokenId =>
                    (for {
                        owner <- clientWallet.createSingleOwnerAddress
                        to <- clientWallet.getIdentity
                        body = TokenContent(Collection(tokenValue))
                        request = WalletIssueTokenRequest(tokenId, owner, body, to)
                        _ <- issuerWallet.issue(Collection(request))
                    } yield ())
                        .orFail(s"Failed to issue token '$tokenName' for client '$clientName'")
                    ()
                }
            }
        }

}
