package ru.sberbank.blockchain.cnft.migration.tests

import org.slf4j.{Logger, LoggerFactory}
import ru.sberbank.blockchain.cnft.commons.{Collection, Result}
import ru.sberbank.blockchain.cnft.migration.tests.model._
import ru.sberbank.blockchain.cnft.model.TokenId
import ru.sberbank.blockchain.cnft.wallet.store.HDPathStore
import ru.sberbank.blockchain.cnft.wallet.{CNFT, CNFTCrypto}

import java.io.{BufferedInputStream, File, FileInputStream}

object MigrationTests extends App {
    private implicit val logger: Logger = LoggerFactory.getLogger("MigrationTests")

    logger.info("Starting migration tests")

    private val GateUrl = sys.env.getOrElse("CNFT_GATE_URL", "http://localhost:8981")
    private val Chain = CNFT.connect(GateUrl)
    private val testFilesDirPath = "/repo/dm/v49rctest/data/ExportedTestData"

    logger.debug(s"listing tokens:")
    val allTokens = Chain.listTokens.getOrElse(Fail(s"can not list tokens"))
    allTokens.foreach { t =>
        logger.debug(s"token: $t")
    }

    val clientDataFileList = getListOfFiles(testFilesDirPath, Collection("test_data_"), Collection(".testbin"))
    val smartContractFileList = getListOfFiles(testFilesDirPath, Collection("test_smartcontract_"), Collection(".testbin"))

    logger.debug(s"client test file list:")
    val clientTestData =
        clientDataFileList.map { f =>
            logger.debug(s"$f")
            ImportClientTestData.parseFrom(
                loadFromFile(s"$testFilesDirPath/${f.getName}")
                    .getOrElse(Fail(s"can not load file data"))
            )
        }

    logger.debug(s"smart contract data file list:")
    val smartContractTestData =
        smartContractFileList.map { f =>
            logger.debug(s"$f")
            ImportSmartContractTestData.parseFrom(
                loadFromFile(s"$testFilesDirPath/${f.getName}")
                    .getOrElse(Fail(s"can not load file data"))
            )
        }

    logger.debug(s"loaded ${clientTestData.length} client test data.")
    logger.debug(s"loaded ${smartContractTestData.length} smart contract test data.")

    val walletsTestData =

        clientTestData.map { c =>
            val w =
                CNFTCrypto
                    .newContext(
                        CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                        CNFTCrypto.bouncyCastleEncryption(),
                        CNFTCrypto.bouncyCastleAccessOperations(),
                        CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                        CNFTCrypto.hash(),
                        CNFTCrypto.secureRandomGenerator()
                    )
                    .importFrom(c.cryptoData)
                    .getOrElse(Fail(s"can not import cryptodata"))
            logger.debug(s"processing wallet ${w.identity.id}")
            val wallet =
                Chain.newWallet(w)
                    .getOrElse(Fail(s"can not create wallet"))
            (wallet, c)
        }

    val wallets = walletsTestData.map(_._1)
    logger.debug(s"totally wallets imported ${wallets.length}")

    logger.debug(s">>> smart contracts <<<")
    val smartContractsList = Chain.listSmartContracts.getOrElse(Fail(s"can not get smart contract list"))

    smartContractsList.foreach { sc =>
        logger.debug(s"smart contract ${sc.id}")
        val acceptedDeals = Chain.listSmartContractAcceptedDeals(sc.id).getOrElse(Fail(s"can not get accepted deals"))
        acceptedDeals.foreach { a =>
            logger.debug(s"sc accepted deal id:${a.deal.deal.dealId}, total tokens: ${a.tokens.length}")
        }
    }

    val allPrevVwalletTokens = clientTestData.flatMap(_.walletTokens).toList
    val duplicatedTokens = allPrevVwalletTokens.map(_.id).groupBy(identity).collect { case (x, List(_, _, _*)) => x }
    logger.debug(s"duplicated tokens:")
    duplicatedTokens.foreach { t =>
        logger.debug(s"$t")
    }

    logger.debug(s"checking consistency:")
    walletsTestData.foreach { case (wallet, testData) =>

        logger.debug(s"client ${testData.clientName} , wallet: ${testData.walletId}, test: ${testData.scenarioName}, ${testData.scenarioStatus}, ${testData.scenarioId}")
        // checking tokens
        val prevVersiontokens = testData.walletTokens
        val currentVersiontokens = wallet.listTokens.getOrElse(Fail(s"can not list client tokens"))
        val prevVersionIds = prevVersiontokens.map(_.id)
        val currentVersionIds = currentVersiontokens.map(_.id)
        val diffLength = currentVersionIds.length - prevVersionIds.length
        if (diffLength == 0) {
            logger.debug(s"client has same amount of tokens")
            if (prevVersionIds sameElements currentVersionIds) {
                logger.debug(s" and same token ids")
            } else {
                logger.debug(s"!!! different token ids")
            }
        } else {
            logger.debug(s"!!! client has different amount of tokens: $diffLength  (currentVersion-prevVersion)")
            logger.debug(s"in test: ${testData.scenarioName}, ${testData.scenarioId}, ${testData.scenarioStatus}\n client ${testData.clientName}, ${testData.walletId}")
            logger.debug(s"currentVersiontokens: ${currentVersiontokens.length} , prevVersiontokens: ${prevVersiontokens.length}")
            logger.debug(s"current version ids:")
            currentVersiontokens.foreach { t =>
                logger.debug(s"${t.id} , type: ${TokenId.from(t.id).getOrElse(Fail(s"can not get id"))}")
                allPrevVwalletTokens.filter(_.id == t.id).foreach { x =>
                    logger.debug(s"${x.id}, ${x.content.mkString(" ")}")
                    logger.debug(s"current version")
                    val content = Chain.getTokenContent(t.id).getOrElse(Fail(s"can not get token content"))
                    logger.debug(s"current version content: ${content.fields.mkString(" ")}")
                }
            }
            logger.debug(s"prev version ids:")
            prevVersiontokens.foreach { t => logger.debug(s"${t.id} , type: ${TokenId.from(t.id).getOrElse(Fail(s"can not get id"))}") }
        }

        val clientPrevVersionOperation = testData.operations
        val clientCurrentVersionOperation = wallet.listOperations.orFail(s"can not get client operations")

        val prevVersionOpIds = clientPrevVersionOperation.map(_.operationId)
        val currentVersionOpIds = clientCurrentVersionOperation.map(_.operationId)

        if (prevVersionOpIds.diff(currentVersionOpIds).isEmpty) {
            logger.debug(s"client has all prev version ids in current version ops list")
        } else {
            logger.debug(s"!!!client has NO all prev version operations ids in current version list")
            logger.debug(s"difference: ${currentVersionOpIds.filterNot(prevVersionOpIds.contains(_)).mkString("; ")}")
        }
        clientPrevVersionOperation.foreach { prevVersionOp =>
            val opId = prevVersionOp.operationId
            logger.debug(s"operation id: $opId")
            clientCurrentVersionOperation.find(_.operationId == opId)
            match {
                case Some(opCurrentVersion) =>
                    val currentVersionHistory = opCurrentVersion.history
                    val prevVersionHistory = prevVersionOp.history
                    logger.debug(">>>current version history:")
                    currentVersionHistory.foreach { h =>
                        logger.debug(s"${h.state} , ${h.timestamp}, ${h.txId}, ${h.block}")
                        val opData = wallet.getOperationDetails(h)
                            .orFail(s"can not get Opertions details")
                        logger.debug(s"operation data $opData")
                    }
                    logger.debug(">>>prev version history:")
                    prevVersionHistory.foreach { h =>
                        logger.debug(s"${h.state} , ${h.timestamp}, ${h.txId}, ${h.block}")
                    }
                    logger.debug(s"--------------------<<<")
                case None =>
                    logger.debug(s"!!! current version does not contain operations for $opId")
                    val prevVersionHistory = prevVersionOp.history
                    logger.debug(">>>prev version history:")
                    prevVersionHistory.foreach { h =>
                        logger.debug(s"${h.state} , ${h.timestamp}, ${h.txId}, ${h.block}")
                    }
            }
        }
    }
    logger.debug(s"************** adjusted history comparision *********")
    val excludedStates = Collection("Issue_Init", "Burn_Init", "Deal_Init", "Transfer_Proposed", "Token_Requested")
    var totalHistoryMatch = 0
    var totalHistoryNotMatch = 0
    var totalNoOperations = 0

    walletsTestData.foreach { case (wallet, testData) =>

        val clientPrevVersionOperation = testData.operations
        val clientCurrentVersionOperation = wallet.listOperations.orFail(s"can not get client operations")

        clientPrevVersionOperation.foreach { prevVersionOp =>
            val opId = prevVersionOp.operationId
            logger.debug(s"operation id: $opId")
            clientCurrentVersionOperation.find(_.operationId == opId)
            match {
                case Some(currentVersionOp) =>
                    val currentVersionHistory = currentVersionOp.history
                    val prevVersionHistoryFiltered = prevVersionOp.history
                        .filterNot(h => excludedStates.contains(h.state))
                    val currentVersionPackedHistory = currentVersionHistory
                        .map { h => s"${h.state}*${h.timestamp}*${h.txId}*${h.block}" }
                        .sorted
                    val prevVersionPackedHistory = prevVersionHistoryFiltered
                        .map { h => s"${h.state}*${h.timestamp}*${h.txId}*${h.block}" }
                        .sorted
                    if (prevVersionPackedHistory.forall(currentVersionPackedHistory.contains(_))) {
                        totalHistoryMatch = totalHistoryMatch + 1
                        logger.debug("prev == current version history")
                    } else {
                        totalHistoryNotMatch = totalHistoryNotMatch + 1
                        prevVersionPackedHistory.filterNot(currentVersionPackedHistory.contains(_)).foreach { h =>
                            logger.debug(s"!!!history difference: $h")
                        }
                        logger.debug(s"->in test: ${testData.scenarioName}, ${testData.scenarioId}, ${testData.scenarioStatus},\n ${testData.clientName} , ${testData.walletId}")
                    }
                case None =>
                    val prevVersionHistoryFiltered = prevVersionOp.history
                        .filterNot(h => excludedStates.contains(h.state))

                    if (prevVersionHistoryFiltered.nonEmpty) {
                        totalNoOperations = totalNoOperations + 1
                        logger.debug(s"!!! current version does not contain operations for $opId")
                        logger.debug(">>>prev version history:")
                        prevVersionHistoryFiltered.foreach { h =>
                            logger.debug(s"${h.state} , ${h.timestamp}, ${h.txId}, ${h.block}")
                        }
                        logger.debug(s"->in test: ${testData.scenarioName}, ${testData.scenarioId}, ${testData.scenarioStatus},\n ${testData.clientName} , ${testData.walletId}")
                    }
            }
        }
    }
    logger.debug(s"### history matched: $totalHistoryMatch")
    logger.debug(s"### history not matched: $totalHistoryNotMatch")
    logger.debug(s"### no operations: $totalNoOperations")

    logger.debug(s"*** burnt tokens check ****")
    walletsTestData.foreach { case (wallet, testData) =>
        val currentVersionBurntIssued = wallet.listBurntIssuedTokens.orFail(s"failed to get burnt issued")
        //   val currentVersionBurnt = wallet.listBurntTokens.orFail(s"failed to get burnt tokens")

        val prevVersionBurntIssued = testData.burntIssuedTokens

        if (prevVersionBurntIssued.nonEmpty) {
            logger.debug(s"client ${testData.clientName} , ${testData.scenarioName}, ${testData.scenarioId}, ${testData.scenarioStatus},\n ${testData.walletId}")
            if (prevVersionBurntIssued.map(_.id).forall(currentVersionBurntIssued.map(_.id).contains(_))) {
                logger.debug(s"all burnt issued prev match current version")
            } else {
                logger.debug(s"!!! burnt issued prev not match current")
            }
        }


    }


    private def getListOfFiles(dirPath: String, prefix: Collection[String], extensions: Collection[String]): Collection[File] = {
        val dir = new File(dirPath)
        dir.listFiles.filter(_.isFile).filter {
            file =>
                extensions.exists(file.getName.endsWith(_)) && prefix.exists(file.getName.startsWith(_))
        }
    }


    private def loadFromFile(fileName: String) = Result {

        logger.debug(s"loading data from $fileName")

        val in = new FileInputStream(fileName)
        val bis = new BufferedInputStream(in)
        val data =
            Stream.continually(bis.read).takeWhile(_ != -1).map(_.toByte).toArray
        in.close()
        logger.debug(s"file $fileName was successfully read.")
        data
    }

    private def Fail(msg: String) = throw new java.lang.AssertionError(msg)

    implicit class RExt[+T](value: Result[T]) {
        @inline def orFail(msg: String): T =
            value match {
                case Right(value) => value
                case Left(error) => Fail(s"$msg: $error")
            }
    }
}
