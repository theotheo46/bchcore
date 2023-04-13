package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import org.slf4j.LoggerFactory
import ru.sberbank.blockchain.cnft.common.types._
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.model._
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.spec.CNFTWalletSpec
import ru.sberbank.blockchain.cnft.wallet.store.HDPathStore
import ru.sberbank.blockchain.cnft.wallet.test.Util.{mkProfilesString, mkTokensString}
import ru.sberbank.blockchain.cnft.wallet.walletmodel.{CreateProfileInfo, WalletIssueTokenRequest}
import ru.sberbank.blockchain.cnft.wallet.{CNFT, CNFTCrypto}

import java.util.UUID
import scala.collection.mutable


/**
 * @author Alexey Polubelov
 */
class RestoreWallet extends ScalaDsl with EN with LoggingSupport {

    private val log = LoggerFactory.getLogger(classOf[RestoreWallet])

    //

    //    After { _: Scenario =>
    //        log.info("Stopping wallets ...")
    //        scenarioContext.wallets.values.foreach(_.stopListenBlocks())
    //        log.info("All wallets stopped.")
    //    }

    Given("""Create wallet for {string}""") {
        clientName: String =>
            log.info(s"Creating new wallet for $clientName")
            val CNFTGateUrl = sys.env.getOrElse("CNFT_GATE_URL", "http://localhost:8981")
            log.info(s"Using CNFT gateway: $CNFTGateUrl")
            val chain = CNFT.connect(CNFTGateUrl)

            val adminWallet = chain.connectWallet("http://localhost:8983")

            {
                for {
                    // register wallet through platform admin wallet
                    crypto <- CNFTCrypto
                        .newContext(
                            CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                            CNFTCrypto.bouncyCastleEncryption(),
                            CNFTCrypto.bouncyCastleAccessOperations(),
                            CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                            CNFTCrypto.hash(),
                            CNFTCrypto.secureRandomGenerator
                        )
                        .create()

                    member <- crypto.memberInformation()
                    _ <- chain.getMember(member.id) match {
                        case Left(_) =>
                            logger.info("Member does not registered yet")
                            adminWallet.registerMember(member)
                                .map { tx =>
                                    logger.info(s"Registered self [${member.id}] in TX: [${tx.blockNumber} : ${tx.txId}]")
                                }

                        case Right(_) =>
                            logger.info("Member already registered")
                            Result(())
                    }
                    wallet <- chain.newWallet(crypto)

                    //
                    // TODO: await registered?
                    //                _ = {
                    //                    log.info("Awaiting admin to register wallet... ")
                    //                    registered.get()
                    //                    log.info("Wallet registered")
                    //                }

                    walletBytes <- crypto.exportData()
                } yield {
                    scenarioContext.walletInfo.put(clientName, walletBytes)
                    //                scenarioContext.waiters.put(clientName, waiters)
                    scenarioContext.wallets.put(clientName, wallet)
                    scenarioContext.wallets.put("Admin", adminWallet)
                    //                    scenarioContext.adminWallet
                    log.info(s"Created new client $clientName")
                }
            }.orFail("Failed to create wallet")
    }

    When("""Admin creates new address""") {
        () =>
            log.info("creating address via remote wallet")
            for {
                adminWallet <- scenarioContext.wallets.get("Admin").toRight("no admin wallet found")
                _ <- adminWallet.createAddress
            } yield log.info("success")

    }

    When("""{string} registered new token type""") {
        (clientName: String) =>
            val tokenTypeName = "T1"
            log.info(s"Registering token type $tokenTypeName for $clientName ...")
            (for {
                wallet <- scenarioContext.wallets.get(clientName).toRight(s"Unknown client $clientName")
                typeId <- wallet.createId
                _ <- wallet
                    .registerTokenType(typeId,
                        TokenTypeMeta(
                            description = Collection(DescriptionField("Value", FieldType.Text, tokenTypeName)),
                            fields = Collection(FieldMeta("Value", FieldType.Text))
                        ),
                        DNA(
                            emission =
                                Collection(
                                    Gene(
                                        id = GeneID.EmissionControlledByIssuer,
                                        parameters = Collection.empty
                                    )
                                ),
                            Collection.empty,
                            Collection.empty,
                            Collection.empty),
                        Collection.empty,
                        Collection.empty
                    )
            } yield {
                scenarioContext.tokenTypeIdByName.put(tokenTypeName, typeId)
                log.info(s"Registered token type $tokenTypeName for $clientName ($typeId).")
            }).orFail(s"Failed to register token type $tokenTypeName")
    }


    When("""{string} issue new token""") {
        (issuerName: String) =>
            val tokenTypeName = "T1"
            log.info(s"Issuing token of type $tokenTypeName for $issuerName ...")
            (for {
                issuerWallet <- scenarioContext.wallets.get(issuerName).toRight(s"Unknown client $issuerName")
                //waiters <- scenarioContext.waiters.get(issuerName).toRight(s"No waiters for $issuerName")
                typeId <- scenarioContext.tokenTypeIdByName.get(tokenTypeName).toRight(s"Unknown token type $tokenTypeName")
                toId <- issuerWallet.getIdentity
                owner <- issuerWallet.createSingleOwnerAddress
                tokenId <- issuerWallet.createTokenId(typeId)
                await = Util.awaitWalletEvents(issuerWallet) { case (_, events) =>
                    if (events.owner.tokensReceived.nonEmpty)
                        Some(events.owner.tokensReceived)
                    else
                        None
                }
                _ <- issuerWallet
                    .issue(
                        Collection(
                            WalletIssueTokenRequest(
                                tokenId,
                                owner,
                                TokenContent(
                                    Collection("100")
                                ),
                                toId
                            )
                        )
                    )
            } yield {
                log.info("Awaiting tokens to arrive ...")
                val added = await.get
                log.info(s"Got tokens: ${added.mkString(", ")}")

                scenarioContext.tokenIdByName.put("T1", tokenId)
                log.info(s"Issued token of type $tokenTypeName for $issuerName ($tokenId).")
                ()
            }).orFail("Error when issuing new token")
    }

    When("""{string} create new profile""") {
        (issuerName: String) =>
            log.info(s"Creating profile for $issuerName ...")
            (for {
                wallet <- scenarioContext
                    .wallets
                    .get(issuerName)
                    .toRight(s"Unknown client $issuerName")
                profileName = UUID.randomUUID().toString
                _ <- wallet.createProfile(
                    CreateProfileInfo(
                        profileName,
                        "test_description",
                        "test_avatar",
                        "test_background"
                    )
                )
            } yield {
                log.info(s"Profile was successfully created for $issuerName")
            }).orFail(s"Failed to create profile for $issuerName")
    }

    When("""Restore wallet for client {string}""") { (clientName: String) =>

        log.info(s"Creating new wallet for $clientName")
        val CNFTGateUrl = sys.env.getOrElse("CNFT_GATE_URL", "http://localhost:8981")
        log.info(s"Using CNFT gateway: $CNFTGateUrl")

        {
            for {
                walletInfo <- scenarioContext.walletInfo.get(clientName).toRight(s"Unknown client $clientName")
                context =
                    CNFTCrypto
                        .newContext(
                            CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                            CNFTCrypto.bouncyCastleEncryption(),
                            CNFTCrypto.bouncyCastleAccessOperations(),
                            CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                            CNFTCrypto.hash(),
                            CNFTCrypto.secureRandomGenerator
                        )
                crypto <- context.importFrom(walletInfo)

                chain = CNFT.connect(CNFTGateUrl)
                wallet <- chain.newWallet(crypto)
            } yield {
                scenarioContext.wallets.put(clientName, wallet)
                log.info(s"Restore wallet new client $clientName")
            }
        }.orFail(s"Error while restoring wallet for client $clientName")
    }

    When("""Check token list in wallet for client {string}""") {
        (clientName: String) =>
            log.info(s"Checks tokens for $clientName...")
            (for {
                wallet <- scenarioContext.wallets.get(clientName).toRight(s"Unknown client $clientName")
                _ = Thread.sleep(5000)
                tokens <- wallet.listTokens
            } yield log.info(mkTokensString(tokens))).orFail(s"Failed to check tokens for $clientName")
    }

    When("""Check {int} tokens in wallet for client {string}""") {
        (expectedCount: Int, clientName: String) =>
            log.info(s"Checks tokens for $clientName...")
            (for {
                wallet <- scenarioContext.wallets.get(clientName).toRight(s"Unknown client $clientName")
                tokens <- wallet.listTokens
            } yield {
                log.info(mkTokensString(tokens))
                assert(tokens.length == expectedCount)
            }).orFail(s"Failed to check tokens for $clientName")
    }

    When("""Check {int} profiles in wallet for client {string}""") {
        (expectedCount: Int, clientName: String) =>
            log.info(s"Checks profiles for $clientName...")
            (for {
                wallet <- scenarioContext.wallets.get(clientName).toRight(s"Unknown client $clientName")
                profiles <- wallet.listProfiles
            } yield {
                log.info(mkProfilesString(profiles))
                assert(profiles.length == expectedCount)
            }).orFail(s"Failed to check tokens for $clientName")
    }

    private val _context = new ThreadLocal[ScenarioContext]() {
        override def initialValue(): ScenarioContext = ScenarioContext()
    }

    private def scenarioContext: ScenarioContext = _context.get()

    case class ScenarioContext(
        wallets: mutable.Map[String, CNFTWalletSpec[Result]] = mutable.HashMap.empty[String, CNFTWalletSpec[Result]],
        tokenTypeIdByName: mutable.Map[String, String] = mutable.HashMap.empty[String, String],
        tokenIdByName: mutable.Map[String, String] = mutable.HashMap.empty[String, String],
        walletInfo: mutable.Map[String, Bytes] = mutable.HashMap.empty[String, Bytes],
    )

}
