package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.common.types.{Bytes, Collection, collectionFromSequence}
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.model.DataFeedValue
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Fail, scenarioContext}

import java.util.UUID
import java.util.concurrent.Executors

class StepsTransferTokens extends ScalaDsl with EN with LoggingSupport {

    When("""{string} sends his {string} token to {string} smart contract""") {
        (clientName: String, tokenName: String, smartContractName: String) =>
            logger.info(s"$clientName sends his $tokenName token to $smartContractName")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val (_, maybeTokenId) = scenarioContext.tokenAddress.getOrElse((clientName, tokenName), Fail(s"No token address for $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
                if (events.owner.tokensBurn.nonEmpty)
                    Some(events.owner.tokensBurn)
                else
                    None
            }
            maybeTokenId.map { tokenId =>
                wallet
                    .sendTokenToSmartContract(destinationAddress, UUID.randomUUID().toString, Collection(tokenId), Bytes.empty, Collection.empty)
                    .orFail(s"Failed to send token to $smartContractName")
                val deleted = await.get
                logger.info(s"Token has been deleted:${deleted.mkString("\n\t", "\n\t", "")}")
            }
    }

    When("""{string} sends all his tokens to {string} smart contract and wait tokens gone""") {
        (clientName: String, smartContractName: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            logger.info(s"$clientName sends all his tokens to $smartContractName with id $destinationAddress")
            val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
                if (events.owner.tokensBurn.nonEmpty)
                    Some(events.owner.tokensBurn)
                else
                    None
            }
            val tokens = wallet.listTokens.orFail("Unable to list tokens").map(_.id)
            wallet
                .sendTokenToSmartContract(destinationAddress, UUID.randomUUID().toString, tokens, Bytes.empty, Collection.empty)
                .orFail(s"Failed to send token to $smartContractName")

            val deleted = await.get
            logger.info(s"lost ${deleted.length} tokens")
    }

    When("""{string} sends his {string} token to {string} smart contract in one deal multiple times""") {
        (clientName: String, tokenName: String, smartContractName: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            val (_, maybeTokenId) = scenarioContext.tokenAddress.getOrElse((clientName, tokenName), Fail(s"No token address for $clientName"))
            val tokenId = maybeTokenId.getOrElse(Fail(""))

            val tokens = collectionFromSequence(for (_ <- 0 until 10) yield tokenId)

            wallet
                .sendTokenToSmartContract(destinationAddress, UUID.randomUUID().toString, tokens, Bytes.empty, Collection.empty)

    }

    private def ExecuteConcurrently(tCount: Int, times: Int)(task: Int => Unit): Unit = {
        val pool = Executors.newFixedThreadPool(tCount)

        {
            for (index <- 0 until times) yield {
                pool.submit(new Runnable {
                    override def run(): Unit = task(index)
                })
            }
        }.foreach(_.get())
    }

    When("""{string} sends his {string} token to {string} smart contract in multiple deals concurrently""") {
        (clientName: String, tokenName: String, smartContractName: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            val (_, maybeTokenId) = scenarioContext.tokenAddress.getOrElse((clientName, tokenName), Fail(s"No token address for $clientName"))
            val tokenId = maybeTokenId.getOrElse(Fail(""))

            val tokens = Collection(tokenId, tokenId)
            ExecuteConcurrently(10, 10) { i: Int =>
                val dealId = wallet.createId.orFail("failed to create deal id")
                logger.info(s"Sending token ($i) ...")
                wallet
                    .sendTokenToSmartContract(destinationAddress, dealId, tokens, Bytes.empty, Collection.empty)
                match {
                    case Left(msg) => logger.info(s"Send token ($i) - FAILED: $msg")
                    case Right(_) => logger.info(s"Send token ($i) COMPLETE")
                }
            }
            logger.info("All tokens sent")
    }

    When("""{string} sends his {int} tokens to {string} smart contract and {int} tokens in {string} smart contract in one deal """) {
        (clientName: String, tokensNum1: Int, smartContractName1: String, tokensNum2: Int, smartContractName2: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val destinationAddress1 = scenarioContext.smartContractAddress.getOrElse(smartContractName1, Fail(s"No smart contract address for $smartContractName1"))
            logger.info(s"$clientName sends $tokensNum1 to $smartContractName1 with id $destinationAddress1")
            val destinationAddress2 = scenarioContext.smartContractAddress.getOrElse(smartContractName2, Fail(s"No smart contract address for $smartContractName2"))
            logger.info(s"$clientName sends $tokensNum2 to $smartContractName2 with id $destinationAddress2")
            val tokens = wallet.listTokens.orFail("Unable to list tokens").map(_.id)
            val tokens1 = tokens.take(tokensNum1)
            val tokens2 = tokens.slice(tokensNum1, tokensNum1 + tokensNum2)
            val dealId1 = wallet.createId.orFail("failed to create deal id")
            wallet
                .sendTokenToSmartContract(destinationAddress1, dealId1, tokens1, Bytes.empty, Collection.empty)
                .orFail(s"Failed to send token to $smartContractName1")
            val dealId2 = wallet.createId.orFail("failed to create deal id")
            wallet
                .sendTokenToSmartContract(destinationAddress1, dealId2, tokens2, Bytes.empty, Collection.empty)
                .orFail(s"Failed to send token to $smartContractName2")

    }

    When("""{string} sends all his tokens to {string} smart contract""") {
        (clientName: String, smartContractName: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            logger.info(s"$clientName sends all his tokens to $smartContractName with id $destinationAddress")
            val tokens = wallet.listTokens.orFail("Unable to list tokens").map(_.id)
            val dealId = wallet.createId.orFail("failed to create deal id")
            wallet
                .sendTokenToSmartContract(destinationAddress, dealId, tokens, Bytes.empty, Collection.empty)
                .orFail(s"Failed to send token to $smartContractName")
    }

    When("""{string} can not send all his tokens to {string} smart contract""") {
        (clientName: String, smartContractName: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            logger.info(s"$clientName sends all his tokens to $smartContractName with id $destinationAddress")
            val tokens = wallet.listTokens.orFail("Unable to list tokens").map(_.id)
            val dealId = wallet.createId.orFail("failed to create deal id")
            wallet
                .sendTokenToSmartContract(destinationAddress, dealId, tokens, Bytes.empty, Collection.empty)
                .expectFail(1)
    }

    When("""{string} sends all his tokens concurrently to {string} smart contract and {string} submit value {string} to {string} datafeed""") {
        (clientName: String, smartContractName: String, feedClient: String, feedValue: String, feedName: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            logger.info(s"$clientName sends all his tokens to $smartContractName with id $destinationAddress")
            val tokens = wallet.listTokens.orFail("Unable to list tokens").map(_.id)
            val totalTokens = tokens.length
            logger.info(s"number of tokens: $totalTokens")
            val walletFeed = scenarioContext.wallets.getOrElse(feedClient, Fail("Unknown feed owner"))
            val dataFeed = scenarioContext.registeredDataFeeds.getOrElse(feedName, Fail(s"Unknown datafeed $feedName"))


            ExecuteConcurrently(10, totalTokens + 1) {
                case 0 =>
                    logger.info(s"$feedClient $feedName $feedValue")
                    walletFeed.submitDataFeedValue(
                        Collection(
                            DataFeedValue(
                                id = dataFeed.id,
                                content = Collection(feedValue)
                            )
                        )
                    ) match {
                        case Left(msg) => logger.info(s"Submit data feed value - FAILED: $msg")
                        case Right(_) => logger.info(s"Data feed value submitted")
                    }

                case index =>
                    val i = index - 1
                    val dealId = wallet.createId.orFail("failed to create deal id")
                    logger.info(s"Sending token ($i) ...")
                    wallet
                        .sendTokenToSmartContract(destinationAddress, dealId, Collection(tokens(i)), Bytes.empty, Collection.empty)
                    match {
                        case Left(msg) => logger.info(s"Send token ($i) - FAILED: $msg")
                        case Right(_) => logger.info(s"Send token ($i) COMPLETE")
                    }
            }
            logger.info("All tokens sent")

    }

    When("""{string} sends {int} his tokens concurrently to {string} smart contract""") {
        (clientName: String, tokensQuanitity: Int, smartContractName: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            logger.info(s"$clientName sends all his tokens to $smartContractName with id $destinationAddress")
            val tokens = wallet.listTokens.orFail("Unable to list tokens").map(_.id)

            ExecuteConcurrently(10, tokensQuanitity) { i: Int =>
                val dealId = wallet.createId.orFail("failed to create deal id")
                logger.info(s"Send token ($i) ...")
                wallet
                    .sendTokenToSmartContract(destinationAddress, dealId, Collection(tokens(i)), Bytes.empty, Collection.empty)
                match {
                    case Left(msg) => logger.info(s"Send token ($i) - FAILED: $msg")
                    case Right(_) => logger.info(s"Send token ($i) COMPLETE")
                }
            }
            logger.info("All tokens sent")
    }

    When("""{string} sends his {string} token to {string} to unregulated smart contract""") {
        (clientName: String, tokenName: String, smartContractName: String) =>
            logger.info(s"$clientName sends his $tokenName token to $smartContractName")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val (_, maybeTokenId) = scenarioContext.tokenAddress.getOrElse((clientName, tokenName), Fail(s"No token address for $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            maybeTokenId.map { tokenId =>
                assert(wallet
                    .sendTokenToSmartContract(destinationAddress, UUID.randomUUID().toString, Collection(tokenId), Bytes.empty, Collection.empty)
                    .isLeft, "Should fail as the smart contrct is not yet regulated")
            }
    }

    When("""{string} sends his {string} token to {string}""") {
        (clientName: String, tokenName: String, to: String) =>
            logger.info(s"$clientName sends his $tokenName token to $to")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val walletTo = scenarioContext.wallets.getOrElse(to, Fail(s"Unknown client $to")).getIdentity.orFail(s"Unknown $to ID")
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id for $clientName"))
            val destinationAddress = scenarioContext.clientAddress.getOrElse(to, Fail(s"No token address for $to"))
            val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
                if (events.owner.tokensBurn.nonEmpty)
                    Some(events.owner.tokensBurn)
                else
                    None
            }
            wallet
                .sendTokenToMember(walletTo, destinationAddress, UUID.randomUUID().toString, Collection(tokenId), Collection.empty, Bytes.empty)
                .orFail(s"Failed to send token")
            val deleted = await.get
            logger.info(s"Token has been deleted:${deleted.mkString("\n\t", "\n\t", "")}")
    }

    When("""{string} tries to send his {string} token to {string}""") {
        (clientName: String, tokenName: String, to: String) =>
            logger.info(s"$clientName sends his $tokenName token to $to")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val walletTo = scenarioContext.wallets.getOrElse(to, Fail(s"Unknown client $to"))
            val walletToId = walletTo.getIdentity.orFail(s"Unknown $to ID")
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id for $clientName"))
            val address = walletTo.createAddress.orFail("Can not create address")
            val result =
                wallet
                    .sendTokenToMember(
                        walletToId, address, UUID.randomUUID().toString,
                        Collection(tokenId), Collection.empty, Bytes.empty
                    )

            assert(result.isLeft)
    }

    When("""[Regulation] {string} sends his {string} token to {string} smart contract""") {
        (clientName: String, tokenName: String, smartContractName: String) =>
            logger.info(s"$clientName sends his $tokenName token to $smartContractName")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            //            val waiters = scenarioContext.waiters.getOrElse(clientName, Fail(s"No waiters for $clientName"))
            val (_, maybeTokenId) = scenarioContext.tokenAddress.getOrElse((clientName, tokenName), Fail(s"No token address for $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            //            val await = waiters.awaitRegulationNeeded[Collection[PendingDeal]] { case (w, pendingIssue, pendingDeal, pendingBurn) =>
            //                if (pendingDeal.nonEmpty) Some(pendingDeal) else None
            //            }
            maybeTokenId.map { tokenId =>
                wallet
                    .sendTokenToSmartContract(destinationAddress, UUID.randomUUID().toString, Collection(tokenId), Bytes.empty, Collection.empty)
                    .orFail(s"Failed to send token to $smartContractName")
                //                await.get()
            }
    }

    When("""[Transfer] {string} sends his {string} token to {string}""") {
        (clientName: String, tokenName: String, to: String) =>
            logger.info(s"$clientName sends his $tokenName token to $to")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val walletTo = scenarioContext.wallets.getOrElse(to, Fail(s"Unknown client $to"))
            val walletToId = walletTo.getIdentity.orFail(s"Unknown $to ID")
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id for $clientName"))
            val await = Util.awaitWalletEvents(walletTo) { case (_, events) =>
                if (events.owner.tokensPending.nonEmpty)
                    Some(events.owner.tokensPending)
                else
                    None
            }
            val address = scenarioContext.clientAddress.getOrElse(to, Fail(s"$to address not found"))
            wallet
                .sendTokenToMember(walletToId, address, UUID.randomUUID().toString, Collection(tokenId), Collection.empty, Bytes.empty)
                .orFail(s"Failed to send token")
            await.get
    }

    When("""{string} sends his {string} token to {string} smart contract - should fail with no regulation""") {
        (clientName: String, tokenName: String, smartContractName: String) =>
            logger.info(s"$clientName sends his $tokenName token to $smartContractName")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val (_, maybeTokenId) = scenarioContext.tokenAddress.getOrElse((clientName, tokenName), Fail(s"No token address for $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            maybeTokenId.map { tokenId =>
                assert(
                    wallet
                        .sendTokenToSmartContract(
                            destinationAddress, UUID.randomUUID().toString, Collection(tokenId), Bytes.empty, Collection.empty
                        )
                        .isLeft
                )
            }

    }

    When("""[Transfer with accept] {string} sends his {string} token to {string}""") {
        (clientName: String, tokenName: String, to: String) =>
            logger.info(s"$clientName sends his $tokenName token to $to")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val walletTo = scenarioContext.wallets.getOrElse(to, Fail(s"Unknown client $to"))
            val walletToId = walletTo.getIdentity.orFail(s"Unknown $to ID")
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id for $clientName"))
            val destinationAddress = scenarioContext.clientAddress.getOrElse(to, Fail(s"No token address for $to"))
            val await = Util.awaitWalletEvents(walletTo) { case (_, events) =>
                if (events.owner.tokensPending.nonEmpty)
                    Some(events.owner.tokensPending)
                else
                    None
            }
            val tx = wallet
                .sendTokenToMember(walletToId, destinationAddress, UUID.randomUUID().toString, Collection(tokenId), Collection.empty, Bytes.empty)
                .orFail(s"Failed to send token")
            logger.info(s"tx: ${tx}")
            await.get
    }

    When("""[Transfer with accept] {string} sends his {string} token to {string} but gets error as frozen token""") {
        (clientName: String, tokenName: String, to: String) =>
            logger.info(s"$clientName sends his $tokenName token to $to")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val walletTo = scenarioContext.wallets.getOrElse(to, Fail(s"Unknown client $to"))
            val walletToId = walletTo.getIdentity.orFail(s"Unknown $to ID")
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id for $clientName"))
            val destinationAddress = scenarioContext.clientAddress.getOrElse(to, Fail(s"No token address for $to"))
            val result = wallet
                .sendTokenToMember(walletToId, destinationAddress, UUID.randomUUID().toString, Collection(tokenId), Collection.empty, Bytes.empty)
            logger.info(s"Result ${result}")
            assert(result.isLeft)
            logger.info(s"did not transfer")


    }
}
