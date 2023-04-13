package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.wallet.test.CNFTCommonSteps.{TokenTypesContext, TokensContext, WalletsContext, clientCreatesIdForTokenOfType, clientIssuesTokenForClientWithValue, clientRegistersTokenType, thereIsAClient}
import org.slf4j.LoggerFactory
import ru.sberbank.blockchain.cnft.wallet.test.CNFTUtil.Chain

class StepsChainGetTokens extends ScalaDsl with EN {

    class ScenarioContext extends WalletsContext with TokenTypesContext with TokensContext

    private val _context = new ThreadLocal[ScenarioContext]() {
        override def initialValue(): ScenarioContext = new ScenarioContext
    }

    private def context: ScenarioContext = _context.get()

    private val log = LoggerFactory.getLogger(classOf[StepsChainGetTokens])

    Given("""[GetTokens] There is a client {string}""") {
        (clientName: String) =>
            log.info(s"Creating new wallet for $clientName")

            thereIsAClient(context, clientName)
            log.info(s"Created new client $clientName")
    }

    When("""[GetTokens] client {string} registers token type {string}""") {
        (clientName: String, tokenTypeName: String) =>
            log.info(s"Registering token type '$tokenTypeName' for client $clientName")
            clientRegistersTokenType(context, clientName, tokenTypeName)
    }

    When("""[GetTokens] client {string} issues token {string} with type {string} and value {string}""") {
        (clientName: String, tokenName: String, tokenTypeName: String, tokenValue: String) =>
            log.info(s"Issuing token by $clientName with name '$tokenName' and type '$tokenTypeName' and value '$tokenValue'")
            clientCreatesIdForTokenOfType(context, clientName, tokenName, tokenTypeName)
            clientIssuesTokenForClientWithValue(context, clientName, clientName, tokenName, tokenValue)

            context.withWallet(clientName) { wallet =>
                val walletTokens = wallet.listTokens
                walletTokens.map { tokens =>
                    log.info(s"${tokens.length} tokens have been issued")
                }
                ()
            }
    }

    Then("""[GetTokens] web users see tokens of type {string}""") {
        (tokenTypeName: String) =>
            log.info(s"Getting tokens with type '$tokenTypeName'")
            context.withTokenType(tokenTypeName) { tokenType =>
                Chain.getTokensByTypeId(tokenType) match {
                    case Left(message: String) => log.info(s"Unable to get tokens of type: $tokenTypeName, got error: $message")
                    case Right(tokens) => {
                        val count = tokens.length
                        tokenTypeName match {
                            case "Type_1" => assert(count == 3)
                            case "Type_2" => assert(count == 4)
                            case "Type_3" => assert(count == 5)
                        }
                        log.info(s"Got $count tokens of type: $tokenTypeName")
                    }
                }
            }


    }

}
