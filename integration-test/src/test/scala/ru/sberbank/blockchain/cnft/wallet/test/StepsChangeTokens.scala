package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Fail, scenarioContext}

class StepsChangeTokens extends ScalaDsl with EN with LoggingSupport {

    When("""{string} changes token {string} with two new tokens with values {string} and {string}""") { (clientName: String, tokenName: String, newValue1: String, newValue2: String) =>
        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"Unknown tokenName $tokenName"))
        val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
            //           if (events.owner.tokensReceived.nonEmpty || events.owner.tokensBurn.nonEmpty) {
            //                logger.info(s"tokens recieved: ${events.owner.tokensReceived.length}")
            //                logger.info(s"token burned: ${events.owner.tokensBurn.length}")
            //                Some((events.owner.tokensReceived, events.owner.tokensBurn))
            //            } else
            //                None
            //        }
            if (events.owner.tokenChanged.nonEmpty) {
                logger.debug(s">>>token change result:")
                logger.debug(s"length: ${events.owner.tokenChanged.length}")
                events.owner.tokenChanged.foreach { tc =>
                    tc.deleted.foreach { d =>
                        logger.debug(s"deleted $d")
                    }
                    tc.added.foreach { a =>
                        logger.debug(s"added: ${a.tokenId}")
                    }
                }
                Some(events.owner.tokenChanged)
            } else
                None
        }
        wallet.changeToken(
            tokenId = tokenId,
            amounts = Collection(
                newValue1,
                newValue2
            )
        )
        val changes = await.get
        //        logger.debug(s">>>token changes results:")
        //        changes._1.foreach(a => logger.debug(s"added: $a"))
        //        changes._2.foreach(d => logger.debug(s"deleted: $d"))
        //
        //        val changes1 = await.get
        //        logger.debug(s">>>token changes results:")
        //        changes1._1.foreach(a => logger.debug(s"added: $a"))
        //        changes1._2.foreach(d => logger.debug(s"deleted: $d"))

        logger.debug(s"<<<token change result:")
        changes.foreach { tc =>
            tc.deleted.foreach { d =>
                logger.debug(s"deleted $d")
            }
            tc.added.foreach { a =>
                logger.debug(s"added: ${a.tokenId}")
            }
        }
    }

    When("""{string} changes token {string} owned {string} with two new tokens with values {string} and {string}""") {
        (regulator: String, tokenName: String, clientName: String, newValue1: String, newValue2: String) =>

            val regulatorWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown client $regulator"))
            val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"Unknown tokenName $tokenName"))

            val regulatorAwait = Util.awaitWalletEvents(regulatorWallet) { case (_, events) =>
                if (events.regulator.tokenChanged.nonEmpty) {
                    logger.debug(s"token change result:")
                    events.regulator.tokenChanged.foreach { tc =>
                        tc.deleted.foreach { d =>
                            logger.debug(s"deleted $d")
                        }
                        tc.added.foreach { a =>
                            logger.debug(s"added: ${a.tokenId}")
                        }
                    }
                    Some(events.regulator.tokenChanged)
                } else
                    None
            }

            val clientAwait = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
                if (events.owner.tokenChanged.nonEmpty)
                    Some(events.owner.tokenChanged)
                else
                    None
            }

            val tokenIds = regulatorWallet.regulatoryChangeToken(
                tokenId = tokenId,
                amounts = Collection(
                    newValue1,
                    newValue2
                )
            ).orFail("")
            logger.info(s"$tokenIds")
            val regulatorChanges = regulatorAwait.get
            logger.info(s"[$regulator view] Added: \n${regulatorChanges.head.added.map(_.toProtoString).mkString("\n")} \nDeleted: \n${regulatorChanges.head.deleted.mkString("Array(", ", ", ")")}")
            val clientChanges = clientAwait.get
            logger.info(s"[$clientName view] Changed: \n${clientChanges.length}")
    }

    When("""{string} tries to change token {string} with two new tokens with values {string} and {string} but gets errors as the amounts are not even""") { (clientName: String, tokenName: String, newValue1: String, newValue2: String) =>
        logger.info(s"$clientName tries to change token $tokenName with two new tokens with values $newValue1 and $newValue2 but gets errors as the amounts are not even")

        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"Unknown tokenName $tokenName"))
        val result = wallet.changeToken(
            tokenId = tokenId,
            amounts = Collection(
                newValue1,
                newValue2
            )
        )
        logger.info(s"$result")
        assert(result.isLeft)
    }

    When("""{string} tries to change token {string} owned {string} with two new tokens with values {string} and {string} but gets errors as the amounts are not even""") {
        (regulator: String, tokenName: String, clientName: String, newValue1: String, newValue2: String) =>
            logger.info(s"$clientName tries to change token $tokenName with two new tokens with values $newValue1 and $newValue2 but gets errors as the amounts are not even")

            val regulatorWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown client $regulator"))
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"Unknown tokenName $tokenName"))
            val result = regulatorWallet.regulatoryChangeToken(
                tokenId = tokenId,
                amounts = Collection(
                    newValue1,
                    newValue2
                )
            )
            logger.info(s"$result")
            assert(result.isLeft)
    }

    When("""{string} tries to change frozen token {string} with two new tokens with values {string} and {string} but gets errors as token is frozen""") { (clientName: String, tokenName: String, newValue1: String, newValue2: String) =>
        logger.info(s"$clientName tries to change frozen token $tokenName with two new tokens with values $newValue1 and $newValue2 but gets errors as token is frozen")

        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"Unknown tokenName $tokenName"))
        val result = wallet.changeToken(
            tokenId = tokenId,
            amounts = Collection(
                newValue1,
                newValue2
            )
        )
        logger.info(s"$result")
        assert(result.isLeft)
    }
    When("""{string} tries to change frozen token {string} owned {string} with two new tokens with values {string} and {string} but gets errors as other regulator""") { (regulatorName: String, tokenName: String, clientName: String, newValue1: String, newValue2: String) =>
        logger.info(s"$regulatorName tries to change frozen token $tokenName owned $clientName with two new tokens with values $newValue1 and $newValue2 but gets errors as other regulator")

        val regulatoryWallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"Unknown tokenName $tokenName"))
        logger.info(s"tokenId: ${tokenId}")
        val result = regulatoryWallet.regulatoryChangeToken(
            tokenId = tokenId,
            amounts = Collection(
                newValue1,
                newValue2
            )
        )
        logger.info(s"$result")
        assert(result.isLeft)
    }

}
