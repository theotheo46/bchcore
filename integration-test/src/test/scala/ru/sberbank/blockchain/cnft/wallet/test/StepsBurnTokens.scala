package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.datatable.DataTable
import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Fail, scenarioContext}

import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

class StepsBurnTokens extends ScalaDsl with EN with LoggingSupport {
    // TODO refactor burns his first token
    When("""[Regulation] {string} burns his first token in his tokens list""") {
        (clientName: String) =>
            logger.info(s"$clientName burns his first token in his list")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            //            val waiters = scenarioContext.waiters.getOrElse("Regulator", Fail(s"No waiters for $clientName"))
            val tokenToBurn = wallet
                .listTokens
                .orFail(s"Failed to list tokens")
                .head
            //            val await = waiters.awaitRegulationNeeded[Collection[PendingBurn]] { case (w, pendingIssue, pendingDeal, pendingBurn) =>
            //                if (pendingIssue.nonEmpty) Some(pendingIssue) else None
            //            }
            wallet.burnTokens(
                tokens = Collection(tokenToBurn.id),
                extra = Array.empty,
                extraFields = Array.empty
            )
                .orFail(s"Failed to burn token")
        //            await.get()
    }

    // TODO refactor burns his first token
    When("""{string} tries to burn his first token in his tokens list""") {
        (clientName: String) =>
            logger.info(s"$clientName burns his first token in his list")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val tokenToBurn = wallet
                .listTokens
                .orFail(s"Failed to list tokens")
                .head
            val result = wallet.burnTokens(
                tokens = Collection(tokenToBurn.id),
                extra = Array.empty,
                extraFields = Array.empty
            )
            println(result)
            assert(result.isLeft)
    }

    // TODO refactor burns his first token
    When("""{string} burns his first token in his tokens list""") {
        (clientName: String) =>
            logger.info(s"$clientName burns his first token in his list")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val tokenToBurn = wallet
                .listTokens
                .orFail(s"Failed to list tokens")
                .headOption.getOrElse(Fail("No tokens in a list"))

            val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
                if (events.owner.tokensBurn.nonEmpty)
                    Some(events.owner.tokensBurn)
                else
                    None
            }
            wallet.burnTokens(
                tokens = Collection(tokenToBurn.id),
                extra = Array.empty,
                extraFields = Array.empty
            )
                .orFail(s"Failed to burn token")
            val deleted = await.get
            logger.info(s"Token has been deleted:${deleted.mkString("\n\t", "\n\t", "")}")
    }

    When("""{string} burns his first token in his tokens list regulated by {string}""") {
        (clientName: String, regulatorName: String) =>
            logger.info(s"$clientName burns his first token in his list")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val regulator = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $clientName"))
            val tokenToBurn = wallet
                .listTokens
                .orFail(s"Failed to list tokens")
                .headOption.getOrElse(Fail("No tokens in a list"))

            val await = Util.awaitWalletEvents(regulator) { case (_, events) =>
                if (events.regulator.pendingBurn.nonEmpty)
                    Some(events.regulator.pendingBurn)
                else
                    None
            }

            wallet.burnTokens(
                tokens = Collection(tokenToBurn.id),
                extra = Array.empty,
                extraFields = Array.empty
            )
                .orFail(s"Failed to burn token")


            val deleted = await.get
            logger.info(s"Token has been deleted:${deleted.mkString("\n\t", "\n\t", "")}")
    }

    When("""{string} not able to burn first token in his tokens list""") {
        (clientName: String) =>
            logger.info(s"$clientName burns his first token in his list")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val tokenToBurn = wallet
                .listTokens
                .orFail(s"Failed to list tokens")
                .headOption.getOrElse(Fail("No tokens in a list"))

            val msg = wallet.burnTokens(Collection(tokenToBurn.id), Array.empty, Array.empty).expectFail

            logger.info(s"Unable to burn: $msg}")
    }

    When("""{string} burns his first token in his tokens list with extra data:""") {
        (clientName: String, data: DataTable) =>
            logger.info(s"$clientName burns his first token in his list with extra data")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val extraFields = data.asMaps().asScala.map { attribute =>
                val value = attribute.get("value")
                logger.info(s"Extra Data value: $value")
                value
            }.toArray
            val tokenToBurn = wallet
                .listTokens
                .orFail(s"Failed to list tokens")
                .head
            val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
                if (events.owner.tokensBurn.nonEmpty)
                    Some(events.owner.tokensBurn)
                else
                    None
            }
            wallet.burnTokens(
                tokens = Collection(tokenToBurn.id),
                extra = Array.empty,
                extraFields = extraFields
            ).orFail(s"Failed to burn token")

            val deleted = await.get
            logger.info(s"Token has been deleted:${deleted.mkString("\n\t", "\n\t", "")}")
    }

    When("""{string} burns all his tokens with extra data:""") {
        (clientName: String, data: DataTable) =>
            logger.info(s"$clientName burns all his tokens with extra data")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val extraFields = data.asMaps().asScala.map { attribute =>
                val value = attribute.get("value")
                logger.info(s"Extra Data value: $value")
                value
            }.toArray
            val tokensToBurn = wallet
                .listTokens
                .orFail(s"Failed to list tokens")

            val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
                if (events.owner.tokensBurn.nonEmpty)
                    Some(events.owner.tokensBurn)
                else
                    None
            }
            wallet.burnTokens(
                tokens = tokensToBurn.map(_.id),
                extra = Array.empty,
                extraFields = extraFields
            ).orFail(s"Failed to burn token")

            val deleted = await.get
            logger.info(s"Token has been deleted:${deleted.mkString(", ")}")
    }
}
