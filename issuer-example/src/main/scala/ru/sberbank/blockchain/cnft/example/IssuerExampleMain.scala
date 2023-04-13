package ru.sberbank.blockchain.cnft.example

import org.slf4j.{Logger, LoggerFactory}
import ru.sberbank.blockchain.cnft.common.types.Result
import ru.sberbank.blockchain.cnft.commons.Collection
import ru.sberbank.blockchain.cnft.CurrentPlatformVersion
import ru.sberbank.blockchain.cnft.model._
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.spec.{CNFTWalletSpec, WalletEvents}
import ru.sberbank.blockchain.cnft.wallet.store.HDPathStore
import ru.sberbank.blockchain.cnft.wallet.{CNFT, CNFTCrypto}

import java.math.BigInteger

object IssuerExampleMain extends App {
    private implicit val logger: Logger = LoggerFactory.getLogger("IssuerExampleMain")
    private val gateUrl = Util.environmentMandatory("GATE_URL")

    logger.info("Starting reference IssuerProcess...")
    logger.info(s"Platform version ${CurrentPlatformVersion}")

    import ru.sberbank.blockchain.cnft.common.types.BytesOps

    //    private val issuerWalletListener = new DefaultNoOpListener {
    //
    //        override def onWalletEvents(wallet: CNFTWalletSpec[Result], events: WalletEvents): Result[Unit] = {
    //            Result {
    //                if (events.owner.tokensReceived.nonEmpty)
    //                    logger.info(s"added: ${events.owner.tokensReceived.mkString(",")}")
    //                if (events.owner.tokensBurn.nonEmpty)
    //                    logger.info(s"deleted: ${events.owner.tokensBurn.mkString(",")}")
    //                if (events.issuer.tokensBurn.nonEmpty)
    //                    logger.info(s"burned: ${events.issuer.tokensBurn.mkString(",")}")
    //            }
    //        }
    //    }

    val cryptoContext =
        CNFTCrypto
            .newContext(
                CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                CNFTCrypto.bouncyCastleEncryption(),
                CNFTCrypto.bouncyCastleAccessOperations(),
                CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                CNFTCrypto.hash(),
                CNFTCrypto.secureRandomGenerator
            )
    val crypto = cryptoContext.create().orFail("Unable to create AccessOperations")
    val chain = CNFT.connect(gateUrl)

    // ================= register member =================
    val adminWallet = chain.connectWallet("http://localhost:8983")
    val member = crypto.memberInformation().orFail("Failed to obtain member information")
    chain.getMember(member.id) match {
        case Left(_) =>
            logger.info("Member does not registered yet")
            val tx = adminWallet.registerMember(member).orFail("Failed to register self in blockchain")
            logger.info(s"Registered self [${member.id}] in TX: [${tx.blockNumber} : ${tx.txId}]")

        case Right(_) =>
            logger.info("Member already registered")
    }

    // ================= create wallet =================
    val result = chain.newWallet(crypto)

    logger.info("client wallet created")
    val wallet = result.orFail("failed to create wallet")

    try {
        val issuerIdentity = wallet.getIdentity.orFail(s"Failed to get wallet identity for")
        logger.info(s"wallet identity: $issuerIdentity")

        val walletAddress = wallet.createAddress.orFail(s"Failed to create wallet address")

        val typeId = walletAddress.toB64

        wallet
            .registerTokenType(
                typeId,
                TokenTypeMeta(
                    description = Collection.empty,
                    fields = Collection(FieldMeta("amount", FieldType.Numeric))
                ),
                DNA.defaultInstance,
                Collection.empty, Collection.empty
            ).orFail("Failed to register token type")
        logger.info(s"token type $typeId created")
    }
    catch {
        case _: Throwable =>
            logger.info(s"failed to start issuer example...")
    }

    logger.info("Start listening blocks ...")

    @volatile private var running = true

    Runtime.getRuntime.addShutdownHook(new Thread("shutdown hook") {
        override def run(): Unit = {
            running = false
        }
    })

    private var lastSeenBlock = 0L
    while (running) {

        wallet.chain.getLatestBlockNumber match {
            case Left(msg) =>
                logger.info(s"Failed to get height: $msg")

            case Right(height) =>
                while (lastSeenBlock <= height) {
                    wallet.events(BigInteger.valueOf(lastSeenBlock), skipSignaturesCheck = false) match {
                        case Left(msg) =>
                            logger.warn(s"Failed to parse block $lastSeenBlock: $msg")

                        case Right(events) =>
                            logger.info(s"Got block $lastSeenBlock")
                            onWalletEvents(wallet, events)
                    }
                    lastSeenBlock += 1
                }

        }
        if (running) Thread.sleep(1000L)
    }


    def onWalletEvents(wallet: CNFTWalletSpec[Result], events: WalletEvents): Result[Unit] = {
        Result {
            if (events.owner.tokensReceived.nonEmpty)
                logger.info(s"added: ${events.owner.tokensReceived.mkString(",")}")
            if (events.owner.tokensBurn.nonEmpty)
                logger.info(s"deleted: ${events.owner.tokensBurn.mkString(",")}")
            if (events.issuer.tokensBurn.nonEmpty)
                logger.info(s"burned: ${events.issuer.tokensBurn.mkString(",")}")
        }
    }
}
