package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import org.slf4j.LoggerFactory
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.test.CNFTCommonSteps.{TokenTypesContext, WalletsContext, clientRegistersTokenType, thereIsAClient}

class StepsTokenTypes extends ScalaDsl with EN {

    private val log = LoggerFactory.getLogger(classOf[StepsTokenTypes])

    //

//    After { _: Scenario =>
//        log.info("Stopping wallets ...")
//        context.wallets.values.foreach(_.stopListenBlocks())
//        log.info("All wallets stopped.")
//    }

    Given("""[Token Types] There is a client {string}""") { clientName: String =>
        log.info(s"Creating a new wallet for client '$clientName'...")
        thereIsAClient(context, clientName)
        log.info(s"Created a new wallet for client '$clientName'")
    }

    When("""[Token Types] Client {string} registers token type {string}""") {
        (clientName: String, tokenTypeName: String) =>
            log.info(s"Client '$clientName' registering token type '$tokenTypeName'...")
            clientRegistersTokenType(
                context,
                clientName,
                tokenTypeName
            )
            context.withTokenType(tokenTypeName) { tokenTypeId =>
                log.info(s"Client '$clientName' registered token type '$tokenTypeName' ($tokenTypeId)")
            }
    }

    Then("""[Token Types] Client {string} sees {int} token types in his list""") {
        (clientName: String, expectedTokenTypesCount: Int) =>
            log.info(s"Checking client '$clientName' has $expectedTokenTypesCount token types in his list...")
            context.withWallet(clientName) { wallet =>
                val actualTokenTypes =
                    wallet
                        .listOwnedTokenTypes
                        .orFail("Failed to check token types")
                assert(actualTokenTypes.length == expectedTokenTypesCount)
            }
    }

    //

    private val _context = new ThreadLocal[ScenarioContext]() {
        override def initialValue(): ScenarioContext = new ScenarioContext
    }

    private def context: ScenarioContext = _context.get()

    class ScenarioContext extends WalletsContext with TokenTypesContext

}
