package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.common.types.{Collection, Result}
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.model.{Operation, OperationStatus, TokenContent}
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.spec.CNFTWalletSpec
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Fail, scenarioContext}
import ru.sberbank.blockchain.cnft.wallet.walletmodel.{FreezeInfo, RegulatoryBurnTokenRequest, WalletIssueTokenRequest}

import java.util.UUID

class StepsRegulation extends ScalaDsl with EN with LoggingSupport {

    When("""{string} approves transaction""") { (regulatorName: String) =>
        logger.info(s"$regulatorName approves transaction...")
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val transactions = transactionsForRegulation(wallet)
        transactions.foreach { operationId =>
            wallet.approveTransaction(operationId.operationId).orFail(s"Failed to approve transaction")
        }
        Thread.sleep(2000)
    }

    When("""{string} send regulatory notification {string} for transactions""") {
        (regulatorName: String, notice: String) =>
            logger.info(s"$regulatorName sending regulatory notification $notice...")
            val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
            val transactions = transactionsForRegulation(wallet)
            transactions.foreach { operationId =>
                wallet.addNotice(operationId.operationId, notice, Collection.empty)
                    .orFail(s"Failed to notify transaction")
            }
            Thread.sleep(2000)
    }

    private def transactionsForRegulation(wallet: CNFTWalletSpec[Result]): Array[Operation] = {
        val transactions = wallet.listOperations
            .map(operations =>
                operations.filter { op =>
                    val state = op.history.last.state
                    logger.info(s"current state: $state")
                    state == OperationStatus.DealPendingRegulation ||
                        state == OperationStatus.BurnPendingRegulation ||
                        state == OperationStatus.IssuePendingRegulation
                }
            ).orFail("Unable to get Pending Transactions")
        transactions
    }

    When("""{string} approves deals""") { (regulatorName: String) =>
        logger.info(s"$regulatorName approves transaction...")
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val transactions = transactionsForRegulation(wallet)
        val deals = transactions.filter { op =>
            val state = op.history.last.state
            state == OperationStatus.DealPendingRegulation
        }
        logger.info(s"Going to approve deals:)")
        deals.foreach { deal =>
            logger.info(s"${deal.operationId}")
        }
        deals.foreach { deal =>
            wallet.approveTransaction(deal.operationId).orFail(s"Failed to approve transaction")
        }
        Thread.sleep(2000)
    }

    When("""{string} approves issues""") { (regulatorName: String) =>
        logger.info(s"$regulatorName approves transaction...")
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val transactions = transactionsForRegulation(wallet)
        val issues = transactions.filter { op =>
            val state = op.history.last.state
            state == OperationStatus.IssuePendingRegulation
        }
        logger.info(s"Going to approve issues")
        issues.foreach { issue =>
            logger.info(s"${issue.operationId}")
        }
        issues.foreach { issue =>
            wallet.approveTransaction(issue.operationId).orFail(s"Failed to approve transaction")
        }
        Thread.sleep(2000)
    }

    When("""{string} approves burn transaction""") { (regulatorName: String) =>
        logger.info(s"$regulatorName approves transaction...")
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val transactions = transactionsForRegulation(wallet)
        transactions.filter { op =>
            val state = op.history.last.state
            state == OperationStatus.BurnPendingRegulation
        }
            .foreach { operationId =>
                wallet.approveTransaction(operationId.operationId).orFail(s"Failed to approve transaction")
            }
        Thread.sleep(2000)
    }

    //    When("""{string} approves transfer transaction""") { (regulatorName: String) =>
    //        logger.info(s"$regulatorName approves transaction...")
    //        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
    //        val transactions = wallet.listPendingOperations.orFail("Unable to get Pending Transactions")
    //        transactions.filter(_.operationType == OperationType.Deal).foreach { operationId =>
    //            wallet.approveTransaction(operationId.operationId).orFail(s"Failed to approve transaction")
    //        }
    //        Thread.sleep(2000)
    //    }

    When("""{string} freezes token {string}""") { (regulatorName: String, tokenName: String) =>
        logger.info(s"$regulatorName freezes  token $tokenName...")
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"Unknown tokenName $tokenName"))
        val restrictionId = UUID.randomUUID().toString
        scenarioContext.restrictionsIds.put(regulatorName, Collection(restrictionId))
        val tx = wallet.freezeToken(
            Collection(
                FreezeInfo(
                    restrictionId = restrictionId,
                    tokenIds = Collection(tokenId)
                )
            )
        ).orFail(s"Failed to freeze token $tokenName")
        println(tx)
        Thread.sleep(2000)
    }

    When("""{string} tries to regulatory burn first frozen token by this regulator, in {string} tokens list""") {
        (regulator: String, client: String) =>
            val clientWallet = scenarioContext.wallets.getOrElse(client, Fail(s"Unknown client $client"))
            val regulatorWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown regulator $regulator"))
            val tokenToBurn = clientWallet.listTokens.orFail(s"Failed to list tokens").head
            val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
                if (events.owner.tokensBurn.nonEmpty && events.owner.tokensBurn.contains(tokenToBurn.id))
                    Some(events.owner.tokensBurn)
                else
                    None
            }
            val result = regulatorWallet.regulatoryBurnToken(RegulatoryBurnTokenRequest(
                tokens = Collection(tokenToBurn.id),
                extraData = Collection.empty
            ))
            logger.info(s"Result: $result")
            assert(result.isRight)
            await.get
            logger.info(s"Token burnt")
    }

    When("""{string} tries to regulatory burn first frozen token by other regulator, in {string} tokens list""") {
        (regulator: String, client: String) =>
            val clientWallet = scenarioContext.wallets.getOrElse(client, Fail(s"Unknown client $client"))
            val regulatorWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown regulator $regulator"))
            val tokenToBurn = clientWallet.listTokens.orFail(s"Failed to list tokens").head
            val result = regulatorWallet.regulatoryBurnToken(RegulatoryBurnTokenRequest(
                tokens = Collection(tokenToBurn.id),
                extraData = Collection.empty
            ))
            logger.info(s"Result: $result")
            assert(result.isLeft)
            logger.info(s"Token didn't burn")
    }

    When("""{string} tries to unfreeze token {string} frozen token by other regulator""") {
        (regulator: String, tokenName: String) =>
            val regulatorWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown regulator $regulator"))
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"Unknown tokenName $tokenName"))
            val restrictionId = scenarioContext.restrictionsIds.getOrElse("Regulator1", Fail(s"Unknown restrictionsIds of regulator $regulator"))
            val result = regulatorWallet.unfreezeToken(
                Collection(
                    FreezeInfo(
                        restrictionId = restrictionId.head,
                        tokenIds = Collection(tokenId)
                    )
                ))
            logger.info(s"Result: $result")
            assert(result.isLeft)
            logger.info(s"Token didn't unfreeze")
    }

    When("""{string} tries to freeze token {string} regulated by other regulator but gets error as is not permitted""") {
        (regulator: String, tokenName: String) =>
            val regulatorWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown regulator $regulator"))
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"Unknown tokenName $tokenName"))
            val restrictionId = UUID.randomUUID().toString
            scenarioContext.restrictionsIds.put(regulator, Collection(restrictionId))
            val result = regulatorWallet.freezeToken(
                Collection(
                    FreezeInfo(
                        restrictionId = restrictionId,
                        tokenIds = Collection(tokenId)
                    )
                ))
            logger.info(s"Result: $result")
            assert(result.isLeft)
            logger.info(s"Token didn't freeze")
    }

    When("""{string} unfreezes token {string}""") { (regulatorName: String, tokenName: String) =>
        logger.info(s"$regulatorName unfreezes token $tokenName...")
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"Unknown tokenName $tokenName"))
        val restrictionId = scenarioContext.restrictionsIds.getOrElse(regulatorName, Fail(s"$regulatorName restriction not found"))
        val tx = wallet.unfreezeToken(
            Collection(
                FreezeInfo(
                    restrictionId = restrictionId.head,
                    tokenIds = Collection(tokenId)
                )
            )
        ).orFail(s"Failed to unfreeze token $tokenName")
        println(tx)
        Thread.sleep(2000)
    }

    When("""{string} rejects first transaction in list""") { (regulatorName: String) =>
        logger.info(s"$regulatorName rejects first transaction in list...")
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val transactions = transactionsForRegulation(wallet)
        transactions.headOption.foreach { operationId =>
            wallet.rejectTransaction(operationId.operationId, "reason", Collection.empty[String]).orFail(s"Failed to reject transaction")
        }
        Thread.sleep(2000)
    }

    When("""{string} rejects deal transaction""") { (regulatorName: String) =>
        logger.info(s"$regulatorName rejects first transaction in list...")
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val transactions = transactionsForRegulation(wallet)
        transactions.filter { op =>
            val state = op.history.last.state
            state == OperationStatus.DealPendingRegulation
        }
            .foreach { operationId =>
                wallet.rejectTransaction(operationId.operationId, "reason", Collection.empty[String]).orFail(s"Failed to reject transaction")
            }
        Thread.sleep(2000)
    }

    When("""{string} rejects burn transaction""") { (regulatorName: String) =>
        logger.info(s"$regulatorName rejects first transaction in list...")
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val transactions = transactionsForRegulation(wallet)
        transactions.filter { op =>
            val state = op.history.last.state
            state == OperationStatus.BurnPendingRegulation
        }
            .foreach { operationId =>
                wallet.rejectTransaction(operationId.operationId, "reason", Collection.empty[String]).orFail(s"Failed to reject transaction")
            }
        Thread.sleep(2000)
    }

    When("""{string} approves {string} smart contract""") { (
    clientName: String, smartContractName: String) =>
        logger.info(s"$clientName approves $smartContractName smart contract...")
        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val smartContract = scenarioContext.smartContract.getOrElse(smartContractName, Fail(s"Unknown client $clientName"))
        val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
            if (events.smartContracts.regulationUpdatedSmartContracts.nonEmpty)
                Some(events.smartContracts.regulationUpdatedSmartContracts)
            else
                None
        }
        wallet
            .approveSmartContract(smartContract.id)
            .orFail(s"Fail to approve smart contract")
        await.get
    }

    When("""{string} rejects smart contract {string} by {string}""") {
        (clientName: String, smartContractName: String, scCreator: String) =>
            logger.info(s"$clientName rejects $smartContractName smart contract...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val scCreatorWallet = scenarioContext.wallets.getOrElse(scCreator, Fail(s"Unknown client $scCreator"))

            val smartContract = scenarioContext.smartContract.getOrElse(smartContractName, Fail(s"Unknown smart contract $smartContractName"))
            val await = Util.awaitWalletEvents(scCreatorWallet) { case (_, events) =>
                if (events.smartContracts.rejectedSmartContracts.nonEmpty)
                    Some(events.smartContracts.rejectedSmartContracts)
                else
                    None
            }
            wallet
                .rejectSmartContract(smartContract.id, "Not enough information")
                .orFail(s"Fail to approve smart contract")
            await.get
    }

    When("""{string} performs regulatory transfer {string} to {string}""") {
        (clientName: String, tokenName: String, to: String) =>
            logger.info(s"$clientName performs regulatory transfer of $tokenName token to $to")
            val regulatoryWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val toWallet = scenarioContext.wallets.getOrElse(to, Fail(s"Unknown client $to"))
            val toId = toWallet.getIdentity.orFail(s"Unknown $to ID")
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id for $clientName"))
            val await = Util.awaitWalletEvents(toWallet) { case (_, events) =>
                if (events.owner.tokensReceived.nonEmpty)
                    Some(events.owner.tokensReceived)
                else
                    None
            }
            val destinationAddress = {
                scenarioContext.wallets.getOrElse(to, Fail(s"Unknown client $to"))
                toWallet.createAddress.orFail("Cannot create address")
            }
            val result = regulatoryWallet
                .regulatoryTransfer(toId, UUID.randomUUID().toString, Collection(tokenId), destinationAddress)
                .orFail(s"Failed to perform regulatory transfer token")
            logger.info(s"Result: $result")
            val added = await.get
            logger.info(s"Token has been added: ${added.mkString}")
    }

    When("""{string} tries to regulatory transfer frozen token {string} by this regulator to {string}""") {
        (regulator: String, tokenName: String, ToClientName: String) =>
            logger.info(s"$regulator tries to regulatory transfer frozen token $tokenName by this regulator to $ToClientName")
            val regulatoryWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown regulator $regulator"))
            val toWallet = scenarioContext.wallets.getOrElse(ToClientName, Fail(s"Unknown client $ToClientName"))
            val toId = toWallet.getIdentity.orFail(s"Unknown $ToClientName ID")
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id for $regulator"))
            val await = Util.awaitWalletEvents(toWallet) { case (_, events) =>
                if (events.owner.tokensReceived.nonEmpty)
                    Some(events.owner.tokensReceived)
                else
                    None
            }
            val destinationAddress = {
                scenarioContext.wallets.getOrElse(ToClientName, Fail(s"Unknown client $ToClientName"))
                toWallet.createAddress.orFail("Cannot create address")
            }
            val result = regulatoryWallet
                .regulatoryTransfer(toId, UUID.randomUUID().toString, Collection(tokenId), destinationAddress)
                .orFail(s"Failed to perform regulatory transfer token")
            logger.info(s"Result: $result")
            val added = await.get
            logger.info(s"Token has been added: ${added.mkString}")
    }

    When("""{string} tries to regulatory transfer frozen token {string} by other regulator to {string}""") {
        (regulator: String, tokenName: String, ToClientName: String) =>
            logger.info(s"$regulator tries to regulatory transfer frozen token $tokenName by other regulator to $ToClientName")
            val regulatoryWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown regulator $regulator"))
            val toWallet = scenarioContext.wallets.getOrElse(ToClientName, Fail(s"Unknown client $ToClientName"))
            val toId = toWallet.getIdentity.orFail(s"Unknown $ToClientName ID")
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id for $regulator"))
            val destinationAddress = {
                scenarioContext.wallets.getOrElse(ToClientName, Fail(s"Unknown client $ToClientName"))
                toWallet.createAddress.orFail("Cannot create address")
            }
            val result = regulatoryWallet
                .regulatoryTransfer(toId, UUID.randomUUID().toString, Collection(tokenId), destinationAddress)
            logger.info(s"Result: $result")
            assert(result.isLeft)
            logger.info(s"Token didn't transfer")
    }

    When("""{string} performs regulatory burn {string} of {string}""") {
        (regulator: String, tokenName: String, client: String) =>
            logger.info(s"$regulator performs regulatory burn of $tokenName token")
            val regulatoryWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown client $regulator"))
            val clientWallet = scenarioContext.wallets.getOrElse(client, Fail(s"Unknown client $client"))
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id for $regulator"))

            val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
                if (events.owner.tokensBurn.nonEmpty && events.owner.tokensBurn.contains(tokenId))
                    Some(events.owner.tokensBurn)
                else
                    None
            }
            val result =
                regulatoryWallet
                    .regulatoryBurnToken(
                        RegulatoryBurnTokenRequest(
                            tokens = Collection(tokenId),
                            extraData = Collection.empty
                        )
                    )
                    .orFail(s"Failed to perform regulatory transfer token")
            logger.info(s"Result: $result")
            await.get
            logger.info(s"Token burnt")
    }

    When("""[ICO-Regulation] {string} issues tokens {string} in quantity {int} for {string} of type {string} with value {string}""") {
        (issuerName: String, tokenNamePrefix: String, tokenQuantity: Int, clientName: String, tokenTypeName: String, tokenValue: String) =>

            for (tokenNumber <- 0 until tokenQuantity) {

                val tokenName = s"${tokenNamePrefix}_$tokenNumber"
                logger.info(s"Issuing token $tokenName of type $tokenTypeName for $clientName ...")
                val issuerWallet = scenarioContext.wallets.getOrElse(issuerName, Fail(s"Unknown client $issuerName"))
                val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
                val (_, maybeTokenId) = scenarioContext.tokenAddress.getOrElse((clientName, tokenName), Fail(s"No address for $clientName"))
                val Regulator = scenarioContext.wallets.getOrElse("Regulator", Fail(s"No waiters for $clientName"))
                val owner = clientWallet.createSingleOwnerAddress
                    .orFail(s"Failed to reserve id for $clientName")

                val await = Util.awaitWalletEvents(Regulator) { case (_, events) =>
                    if (events.regulator.pendingIssues.nonEmpty) Some(events.regulator.pendingIssues) else None
                }
                maybeTokenId.foreach { tokenId =>
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
    }
}
