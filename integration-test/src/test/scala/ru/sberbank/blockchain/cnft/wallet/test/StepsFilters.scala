package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import org.slf4j.LoggerFactory
import ru.sberbank.blockchain.cnft.common.types._
import ru.sberbank.blockchain.cnft.model._
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.test.CNFTCommonSteps._
import ru.sberbank.blockchain.cnft.wallet.test.CNFTUtil.Chain
import ru.sberbank.blockchain.cnft.wallet.test.Util.{mkTokenTypesString, mkTokensString}

class StepsFilters extends ScalaDsl with EN {

    private val log = LoggerFactory.getLogger(classOf[StepsFilters])

    //    After { _: Scenario =>
    //        log.info("Stopping wallets ...")
    //        context.wallets.values.foreach(_.stopListenBlocks())
    //        log.info("All wallets stopped.")
    //    }

    Given("""[Filters] There is a client {string}""") {
        clientName: String =>
            log.info(s"Creating new wallet for $clientName")
            thereIsAClient(context, clientName)
            log.info(s"Created new client $clientName")
    }

    When("""[Filters] Client {string} registers token type {string} with fungible gene""") {
        (clientName: String, tokenTypeName: String) =>
            log.info(s"Registering fungible token type $tokenTypeName for $clientName...")
            clientRegistersTokenType(
                context,
                clientName,
                tokenTypeName,
                Some(
                    Gene(
                        id = GeneID.Fungible,
                        parameters = Collection.empty
                    )
                )
            )
            context.withTokenType(tokenTypeName) { tokenTypeId =>
                log.info(s"Registered fungible token type $tokenTypeName for $clientName ($tokenTypeId)")
            }
    }

    When("""[Filters] Client {string} registers token type {string} without fungible gene""") {
        (clientName: String, tokenTypeName: String) =>
            log.info(s"Registering non-fungible token type $tokenTypeName for $clientName...")
            clientRegistersTokenType(context, clientName, tokenTypeName)
            context.withTokenType(tokenTypeName) { tokenTypeId =>
                log.info(s"Registered non-fungible token type $tokenTypeName for $clientName ($tokenTypeId)")
            }
    }

    When("""[Filters] Client {string} creates ID for token {string} of type {string}""") {
        (clientName: String, tokenName: String, tokenTypeName: String) =>
            clientCreatesIdForTokenOfType(context, clientName, tokenName, tokenTypeName)
    }

    When("""[Filters] Client {string} issues token {string} for client {string} with value {string}""") {
        (issuerName: String, tokenName: String, clientName: String, tokenValue: String) =>
            log.info(s"Issuing token '$tokenName' for $clientName ...")

            val clientWallet = context.wallets.getOrElse(clientName, throw new Exception(s"Unknown client $clientName"))
            val issueTokenEvent = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
                val tokensReceived = events.owner.tokensReceived
                if (tokensReceived.nonEmpty)
                    log.info(s"[ClientFilter Listener] New token received: ${tokensReceived.toSeq}.")

                tokensReceived.headOption
            }

            clientIssuesTokenForClientWithValue(
                context,
                issuerName,
                clientName,
                tokenName,
                tokenValue
            )
            issueTokenEvent.get
            log.info(s"Issued token '$tokenName' for '$clientName'")
    }

    private def clientHaveTokenTypesFiltered(
        clientName: String,
        tokenTypesCountExpected: Int,
        changeGeneNegation: Boolean
    ): Result[Unit] = {
        for {
            walletIdentity <- context.getWalletIdentityByClientName(clientName)
            tokenTypesFiltered <-
                Chain
                    .listTokenTypesFiltered(
                        TokenTypeFilter(
                            changeGeneId = GeneID.Fungible,
                            negation = changeGeneNegation
                        )
                    )
            myTokenTypes = tokenTypesFiltered.filter(_.issuerId == walletIdentity)
        } yield {
            log.info(mkTokenTypesString(myTokenTypes))
            assert(myTokenTypes.length == tokenTypesCountExpected)
        }
    }

    Then("""[Filters] Client {string} have {int} token types filtered by fungible""") {
        (clientName: String, tokenTypesCountExpected: Int) =>
            log.info("Checks fungible token types")
            clientHaveTokenTypesFiltered(clientName, tokenTypesCountExpected, changeGeneNegation = false)
                .orFail("Failed to check fungible token types")
    }

    Then("""[Filters] Client {string} have {int} token types filtered by non-fungible""") {
        (clientName: String, tokenTypesCountExpected: Int) =>
            log.info("Checks non-fungible token types")
            clientHaveTokenTypesFiltered(clientName, tokenTypesCountExpected, changeGeneNegation = true)
                .orFail("Failed to check non-fungible token types")
    }

    Then("""[Filters] Client {string} sees {int} token\(s) in his list filtered by fungible""") {
        (clientName: String, expectedTokensCount: Int) =>
            log.info(s"Checks fungible tokens for $clientName...")
            context.withWallet(clientName) { wallet =>
                val tokensFiltered =
                    wallet
                        .listTokensFiltered(
                            TokenTypeFilter(
                                changeGeneId = GeneID.Fungible,
                                negation = false
                            )
                        )
                        .orFail(s"Failed to check fungible tokens for $clientName")
                log.info(mkTokensString(tokensFiltered))
                assert(tokensFiltered.length == expectedTokensCount)
            }
    }

    Then("""[Filters] Client {string} sees {int} token\(s) in his list filtered by non-fungible""") {
        (clientName: String, expectedTokensCount: Int) =>
            log.info(s"Checks non-fungible tokens for $clientName...")
            context.withWallet(clientName) { wallet =>
                val tokensFiltered =
                    wallet
                        .listTokensFiltered(
                            TokenTypeFilter(
                                changeGeneId = GeneID.Fungible,
                                negation = true
                            )
                        )
                        .orFail(s"Failed to check non-fungible tokens for $clientName")
                log.info(mkTokensString(tokensFiltered))
                assert(tokensFiltered.length == expectedTokensCount)
            }

    }

    private val _context = new ThreadLocal[ScenarioContext]() {
        override def initialValue(): ScenarioContext = new ScenarioContext
    }

    private def context: ScenarioContext = _context.get()

    class ScenarioContext extends WalletsContext with TokenTypesContext with TokensContext

}
