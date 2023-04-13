package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl, Scenario}
import ru.sberbank.blockchain.cnft.common.types.{Bytes, Collection, Result}
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.model.{OperationStatus, PendingIssue}
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.store.HDPathStore
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Fail, _}
import ru.sberbank.blockchain.cnft.wallet.test.Util.mkTokensString
import ru.sberbank.blockchain.cnft.wallet.walletmodel.ClientTestData
import ru.sberbank.blockchain.cnft.wallet.{CNFT, CNFTCrypto}

import java.io.{File, FileOutputStream}
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class StepsWalletOperations extends ScalaDsl with EN with LoggingSupport {

    Given("""There is a client {string}""") {
        clientName: String =>
            logger.info(s"Creating new wallet for $clientName")

            {
                val CNFTGateUrl = sys.env.getOrElse("CNFT_GATE_URL", "http://localhost:8981")
                logger.info(s"Using CNFT gateway: $CNFTGateUrl")
                val chain = CNFT.connect(CNFTGateUrl)
                val admin = chain.connectWallet("http://localhost:8983")

                for {
                    // register wallet through platform admin wallet

                    crypto <- CNFTCrypto
                        .newContext(
                            CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                            CNFTCrypto.bouncyCastleEncryption(),
                            CNFTCrypto.bouncyCastleAccessOperations(),
                            CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                            CNFTCrypto.hash(),
                            CNFTCrypto.secureRandomGenerator()
                        )
                        .create()

                    member <- crypto.memberInformation()
                    _ <- chain.getMember(member.id) match {
                        case Left(_) =>
                            logger.info("Member does not registered yet")
                            admin
                                .registerMember(member)
                                .map { tx =>
                                    logger.info(s"Registered self [${member.id}] in TX: [${tx.blockNumber} : ${tx.txId}]")
                                }

                        case Right(_) =>
                            logger.info("Member already registered")
                            Result(())
                    }

                    wallet <- chain.newWallet(crypto)
                    walletIdentity <- wallet.getIdentity
                } yield {
                    scenarioContext.walletIds.put(clientName, walletIdentity)
                    scenarioContext.wallets.put(clientName, wallet)
                    logger.info(s"Created new client $clientName \n id: ${member.id}")
                }

            }.orFail("Failed to create wallet")
    }

    When("""{string} create owner address""") {
        (clientName: String) =>
            logger.info(s"Create token address for $clientName...")
            val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val owner = clientWallet.createSingleOwnerAddress
                .orFail(s"Failed to reserve id for $clientName")
            scenarioContext.tokenOwnerAddress.put(clientName, owner)
            logger.info(s"owner address created ${owner.ownerType} , ${owner.address.mkString(" ")}")
    }

    When("""{string} sees four tokens in his list""") {
        (clientName: String) =>
            logger.info(s"Checks tokens for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val tokens = wallet
                .listTokens
                .orFail(s"Failed to check tokens for $clientName")
            logger.info(mkTokensString(tokens))
            assert(tokens.length == 4)
    }

    When("""{string} sees two tokens in his list""") {
        (clientName: String) =>
            logger.info(s"Checks tokens for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val tokens = wallet
                .listTokens
                .orFail(s"Failed to check tokens for $clientName")
            logger.info(mkTokensString(tokens))
            assert(tokens.length == 2)
    }

    When("""{string} sees one token in his list""") {
        (clientName: String) =>
            logger.info(s"Checks tokens for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val tokens = wallet
                .listTokens
                .orFail(s"Failed to check tokens for $clientName")
            logger.info(s"list of $clientName tokens:\n${mkTokensString(tokens)}")

            Util.printOperations(wallet)
            assert(tokens.length == 1)
    }

    When("""{string} see token {string} has restrictions and ongoing operation""") {
        (clientName: String, tokenName: String) =>
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id with name $tokenName"))
            logger.info(s"Checks wallet token $tokenName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val token = wallet
                .chain
                .getToken(tokenId)
                .orFail(s"Failed to get token $tokenName")
            logger.info(s"Token $tokenName:\nID:${token.id}\nContent:${token.content.mkString(", ")}\nRestrictions:${token.restrictions.mkString(", ")}\nOperations:${token.operations.mkString(", ")}")
            assert(token.id.nonEmpty && token.content.nonEmpty && token.operations.nonEmpty && token.restrictions.nonEmpty)
    }

    When("""{string} sees {int} tokens in his list""") {
        (clientName: String, numberOfTokens: Int) =>
            logger.info(s"Checks tokens for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val tokens = wallet
                .listTokens
                .orFail(s"Failed to check tokens for $clientName")
            logger.info(s"list of $clientName tokens:\n${mkTokensString(tokens)}")

            Util.printOperations(wallet)
            assert(tokens.length == numberOfTokens)
    }

    When("""{string} list his tokens""") {
        (clientName: String) =>
            logger.info(s"Checks tokens for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val tokens = wallet
                .listTokens
                .orFail(s"Failed to check tokens for $clientName")
            logger.info(s"list of $clientName tokens:\n${mkTokensString(tokens)}")

    }

    When("""{string} awaits {int} minutes""") {
        (_: String, minutes: Int) =>
            logger.info(s"Awaiting $minutes minutes...")
            TimeUnit.MINUTES.sleep(minutes.toLong)
    }

    When("""{string} awaits {int} seconds""") {
        (_: String, seconds: Int) =>
            logger.info(s"Awaiting $seconds seconds...")
            TimeUnit.SECONDS.sleep(seconds.toLong)
    }

    When("""{string} checks operations list""") {
        (clientName: String) =>
            logger.info(s"Operations for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            Util.printOperations(wallet)
    }

    When("""{string} sees no tokens in his list""") {
        (clientName: String) =>
            logger.info(s"Checks tokens for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val tokens = wallet
                .listTokens
                .orFail(s"Failed to check tokens for $clientName")
            logger.info(mkTokensString(tokens))

            assert(tokens.isEmpty)
    }

    When("""{string} checks his burnt issuer tokens""") {
        (clientName: String) =>
            logger.info(s"Checks tokens for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val burntTokens = wallet
                .listBurntIssuedTokens
                .orFail(s"Failed to issue token for $clientName")
            logger.info(s"[$clientName] Burnt tokens are: ${burntTokens.map(token => s"ID: ${token.id} Restrictions: ${token.restrictions.mkString(", ")} Content: ${token.content.mkString(", ")} Operations: ${token.operations.mkString(", ")}").mkString}")
    }

    When("""{string} registered address""") {
        (clientName: String) =>
            logger.info(s"Registering address for ")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val address =
                wallet
                    .createAddress
                    .orFail(s"Failed to create address")
            scenarioContext.clientAddress.put(clientName, address)
            logger.info(s"Created address for $clientName.")
    }

    When("""{string} merged all his tokens""") { (clientName: String) =>
        logger.info(s"$clientName tries to merge tokens")

        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val tokens = wallet.listTokens.orFail("Unable to list tokens").map(_.id)

        logger.info(s"client tokens:")
        tokens.foreach { t =>
            logger.info(s"id: $t")
        }
        val result = wallet.mergeTokens(tokens)
            .orFail(s"can not merge tokens")
        logger.info(s"$result")
    }

    When("""{string} merged all his tokens but gets error as some tokens are frozen""") { (clientName: String) =>
        logger.info(s"$clientName tries to merge tokens")

        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val tokens = wallet.listTokens.orFail("Unable to list tokens").map(_.id)

        logger.info(s"client tokens:")
        tokens.foreach { t =>
            logger.info(s"id: $t")
        }
        val result = wallet.mergeTokens(tokens)
        logger.info(s"$result")
        assert(result.isLeft)
        logger.info("did not merge all tokens")
    }

    When("""{string} can not merge all his tokens""") { (clientName: String) =>
        logger.info(s"$clientName tries to merge tokens")

        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val tokens = wallet.listTokens.orFail("Unable to list tokens").map(_.id)

        val result = wallet.mergeTokens(tokens)
            .expectFail(0)
        logger.info(s"$result")
    }

    When("""{string} accepts token""") { (clientName: String) =>
        logger.info(s"$clientName accepts token...")
        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val operations =
            wallet
                .listOperations.orFail("Unable to get operations list")
                .filter(_.history.lastOption.exists(_.state == OperationStatus.AcceptPending))
                .map(_.operationId)

        operations.foreach { operation =>
            wallet.acceptToken(operation).orFail(s"Failed to accept transaction")
        }
        Thread.sleep(2000)
    }

    When("""{string} checks his pending transactions""") { (regulatorName: String) =>
        logger.info(s"$regulatorName checks his pending transactions...")
        Thread.sleep(2000)
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val transactions = wallet.listOperations
            .map(operations =>
                operations.filter { op =>
                    val currentState = op.history.last.state
                    currentState == OperationStatus.IssuePendingRegulation ||
                        currentState == OperationStatus.DealPendingRegulation ||
                        currentState == OperationStatus.BurnPendingRegulation
                }
            ).orFail("Unable to get Pending Transactions")
        logger.info(s"Transactions are:")
        transactions.foreach { tr =>
            logger.info(s"${tr.operationId}, ${tr.history.last}")
        }
    }

    When("""{string} checks his pending transactions with issue extradata""") { (regulatorName: String) =>
        logger.info(s"$regulatorName checks his pending transactions...")
        Thread.sleep(2000)
        val wallet = scenarioContext.wallets.getOrElse(regulatorName, Fail(s"Unknown client $regulatorName"))
        val transactions = wallet.listOperations
            .map(operations =>
                operations.filter { op =>
                    val currentState = op.history.last.state
                    currentState == OperationStatus.IssuePendingRegulation ||
                        currentState == OperationStatus.DealPendingRegulation ||
                        currentState == OperationStatus.BurnPendingRegulation
                }
            ).orFail("Unable to get Pending Transactions")
        transactions.foreach { operation =>
            if (operation.history.last.state == OperationStatus.IssuePendingRegulation) {
                PendingIssue.parseFrom(operation.history.last.data).request.issue.tokens.map { token =>
                    val data = wallet.extractIssueTokenExtraData(token)
                    data.map { d => println(s"issue ${token.tokenId} extradata member ID: ${d.memberID}") }
                }
            }
        }
        logger.info(s"Transactions are: ${transactions.mkString(", ")}")
    }


    When("""{string} send generic message {string} to {string}""") {
        (clientName: String, messageData: String, to: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val walletTo = scenarioContext.wallets.getOrElse(to, Fail(s"Unknown client $to"))
            val toMember = walletTo.getIdentity.orFail(s"Fail to get walletIdentity for $to")
            val await = Util.awaitWalletEvents(walletTo) { case (_, events) =>
                if (events.member.genericMessages.nonEmpty)
                    Some(events.member.genericMessages)
                else
                    None
            }
            val r = wallet.sendGenericMessage(toMember, 1010, 100, messageData.getBytes(StandardCharsets.UTF_8))
            logger.info(s"message send result $r")

            val gr = await.get
            gr.foreach { g =>
                val genericMessage = wallet.extractGenericMessage(g.message.event).getOrElse(Fail(s"can not extract generic message"))
                logger.info(s"received message:\nfrom: ${g.from}\ntimestamp: ${g.message.event.timestamp}\n${new String(genericMessage.data, StandardCharsets.UTF_8)}")
            }

    }

    When("""{string} list his private messages""") {
        (clientName: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val r = wallet.listMessages.orFail(s"can not list messages")
            logger.info(s"Messages: ")
            r.foreach { m =>
                logger.info(s"$m")
            }


    }


    When("""{string} proposes token {string} to {string}""") {
        (clientName: String, _: String, to: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val walletTo = scenarioContext.wallets.getOrElse(to, Fail(s"Unknown client $to"))
            val toMember = walletTo.getIdentity.orFail(s"Fail to get walletIdentity for $to")
            val await = Util.awaitWalletEvents(walletTo) { case (_, events) =>
                if (events.owner.transfersProposed.nonEmpty)
                    Some(events.owner.transfersProposed)
                else
                    None
            }
            wallet.proposeToken(toMember, "", Collection.empty, Bytes.empty)
            val r = await.get
            scenarioContext.operationIds.put(clientName,
                Array(r.headOption.getOrElse(throw new Exception("No transfer proposals received"))
                    .message.event.operationId))
    }

    When("""{string} accepts proposal from {string}""") {
        (clientName: String, from: String) =>
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val walletTo = scenarioContext.wallets.getOrElse(from, Fail(s"Unknown client $from"))
            val await = Util.awaitWalletEvents(walletTo) { case (_, events) =>
                if (events.owner.tokensRequested.nonEmpty)
                    Some(events.owner.tokensRequested)
                else
                    None
            }
            val operationId = scenarioContext.operationIds.getOrElse(from, Fail(s"Unknown operation for $from"))

            scenarioContext.operationIds.put(from, Collection.empty)
            logger.info(s"operation id ${operationId.head}")
            wallet.acceptTransferProposal(operationId.head, Bytes.empty)
            val r = await.get

            scenarioContext.operationIds.put(clientName,
                Array(r.headOption.getOrElse(throw new Exception("No transfer proposals received"))
                    .message.event.operationId))
    }

    When("""{string} requests token {string} from {string}""") { (clientName: String, _: String, from: String) =>
        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val walletFrom = scenarioContext.wallets.getOrElse(from, Fail(s"Unknown client $from"))
        val fromMember = walletFrom.getIdentity.orFail(s"Fail to get walletIdentity for $from")
        val await = Util.awaitWalletEvents(walletFrom) { case (_, events) =>
            if (events.owner.tokensRequested.nonEmpty)
                Some(events.owner.tokensRequested)
            else
                None
        }
        wallet.requestToken(fromMember, "", Collection.empty, Bytes.empty)
        val r = await.get

        scenarioContext.operationIds.put(clientName,
            Array(r.headOption.getOrElse(throw new Exception("No token requests received"))
                .message.event.operationId))

        val address = wallet.createAddress.orFail("Cannot create address")

        scenarioContext.clientAddress.put(clientName, address)

    }

    When("""{string} accepts request for token {string} from {string} accept require""") {
        (clientName: String, tokenName: String, to: String) =>
            logger.info(s"$clientName accepts request for token $tokenName from $to")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val walletTo = scenarioContext.wallets.getOrElse(to, Fail(s"Unknown client $to"))
            val tokenId = scenarioContext.tokenIdByName.getOrElse(tokenName, Fail(s"No token id for $clientName"))
            val await = Util.awaitWalletEvents(walletTo) { case (_, events) =>
                if (events.owner.tokensPending.nonEmpty)
                    Some(events.owner.tokensPending)
                else
                    None
            }

            val operationId = scenarioContext.operationIds.getOrElse(to, Fail(s"Unknown operation for $to"))

            scenarioContext.operationIds.put(to, Collection.empty)

            wallet
                .acceptTokenRequest(operationId.head, Collection(tokenId), Bytes.empty)
                .orFail(s"Failed to accept token request")

            await.get
    }

    When("""{string} checks all wallet events""") {
        (clientName: String) =>
            logger.info(s"Checks tokens for $clientName...")
            val lastBlockNumber = Chain.getLatestBlockNumber.getOrElse(Fail(s""))
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            for (block <- 0L to lastBlockNumber) {
                val events = wallet.events(BigInteger.valueOf(block), skipSignaturesCheck = true)
                logger.debug(s"client events: $events")
                val e = events.getOrElse(Fail(s"can not get events"))
                logger.debug(s"events: $e")
            }


    }

    //    Before { _: Scenario =>
    //        log.info("Before...")
    //    }
    //

    After { scenario: Scenario =>
        logger.info("step after ...")

        val exportTestData = sys.env.getOrElse("EXPORT_TEST_DATA", "false").toBoolean

        if (exportTestData) {
            logger.debug("exporting scenario test data....")
            scenarioContext.wallets.foreach { case (clientName, _) =>
                logger.debug(s"client $clientName")
                exportClientData(clientName, scenario.getName, scenario.getUri.toString, scenario.getStatus.toString)
            }
        }

        //    scenarioContext.wallets.values.foreach(_.stopListenBlocks())
        //    log.info("All wallets stopped.")
    }

    private def exportClientData(clientName: String, scenarioName: String, scenarioId: String, scenarioStatus: String) = {
        logger.info(s"exporting client $clientName data from $scenarioName status: $scenarioStatus.")
        val walletCrypto = scenarioContext.walletCrypto.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

        //            val exportListMembers = wallet.listMembers.getOrElse(Fail(s"can not list members"))
        //            val exportListSmartContracts = wallet.listSmartContracts.getOrElse(Fail(s"can not list smart contracts"))

        val clientTestData =
            ClientTestData(
                clientName = clientName,
                walletId = walletCrypto.identity.id,
                scenarioName = scenarioName,
                scenarioId = scenarioId,
                scenarioStatus = scenarioStatus,
                cryptoData = walletCrypto.exportData().orFail(s"can not export $clientName crypto data"),
                operations = wallet.listOperations.orFail(s"can not list operations"),
                walletTokens = wallet.listTokens.orFail(s"can not list tokens"),
                burntIssuedTokens = wallet.listBurntIssuedTokens.orFail(s"can not list burnt issued tokens"),
                burntTokens = wallet.listBurntTokens.orFail(s"ca not get burnt tokens"),
                issuedTokens = wallet.listIssuedTokens.orFail(s"can not get issued tokens"),
                messages = wallet.listMessages.orFail(s"can not get messages"),
                ownedTokenTypes = wallet.listOwnedTokenTypes.orFail(s"can not get owned token types"),
                profiles = wallet.listProfiles.orFail(s"can not get profiles"),
                signedEndorsments = wallet.listEndorsements.getOrElse(Fail(s"can not list endorsments"))
            )


        saveToFile(s"test_data_${walletCrypto.identity.id}.testbin", clientTestData.toByteArray)

    }

    private def saveToFile(fileName: String, data: Array[Byte]) = Result {
        val dirName = "ExportedTestData"

        val directory = new File(dirName)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        logger.debug(s"writing data into file $fileName")

        val out = new FileOutputStream(s"$dirName/$fileName")
        out.write(data)
        out.close()
        logger.debug(s"file $fileName ready.")

    }

}
