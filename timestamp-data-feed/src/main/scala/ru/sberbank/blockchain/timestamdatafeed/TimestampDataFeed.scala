package ru.sberbank.blockchain.timestamdatafeed

import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, LoggingSupport, Result}
import ru.sberbank.blockchain.cnft.model.{DataFeedValue, DescriptionField, FieldMeta, FieldType}
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.spec.CNFTWalletSpec
import ru.sberbank.blockchain.cnft.wallet.store.HDPathStore
import ru.sberbank.blockchain.cnft.wallet.{CNFT, CNFTCrypto}

import java.time.Instant

/**
 * @author Maxim Fedin
 */
class TimestampDataFeed(
    adminUrl: String,
    gateUrl: String,
    walletData: Bytes,
    processorTimeout: Long
) extends Thread with LoggingSupport {

    @volatile var running = false
    private[this] val monitor = new Object

    private val context =
        CNFTCrypto
            .newContext(
                CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                CNFTCrypto.bouncyCastleEncryption(),
                CNFTCrypto.bouncyCastleAccessOperations(),
                CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                CNFTCrypto.hash(),
                CNFTCrypto.secureRandomGenerator
            )

    private val crypto = context.importFrom(walletData).orFail("Unable to create AccessOperations")

    private val chain = CNFT.connect(gateUrl)
    private val adminWallet = chain.connectWallet(adminUrl)

    private val wallet: CNFTWalletSpec[Result] = {
        for {
            member <- crypto.memberInformation()
            _ <- chain.getMember(member.id) match {
                case Left(_) =>
                    logger.info("Member does not registered yet")
                    adminWallet
                        .registerMember(member)
                        .map { tx =>
                            logger.info(s"Registered self [${member.id}] in TX: [${tx.blockNumber} : ${tx.txId}]")
                        }

                case Right(_) =>
                    logger.info("Member already registered")
                    Result(())
            }

            wallet <- chain.newWallet(crypto)

        } yield wallet

    }.orFail("Unable to create wallet")

    def startup(): Unit = {
        monitor.synchronized {
            if (!running) {
                logger.debug("Starting processing thread...")
                running = true
                start()
            }
        }
    }

    def shutdown(): Unit = {
        monitor.synchronized {
            if (running) {
                logger.debug("Stopping processing thread...")
                running = false
            }
        }
        logger.debug("Awaiting processing thread to stop...")
        interrupt()
        join()
    }


    override def run(): Unit = {
        logger.debug("Timestamp DataFeed started.")
        chain.listDataFeeds match {
            case Left(msg) =>
                logger.error(s"Failed to get list of feeds: $msg")

            case Right(feeds) =>
                feeds
                    .find(_.owner == crypto.identity.id).map(Right(_))
                    .getOrElse {
                        logger.info("Feed not found registering new")
                        wallet.registerDataFeed(
                            description = Collection(
                                DescriptionField("description", FieldType.Text, "Gate timestamping feed")
                            ),
                            fields = Collection(
                                FieldMeta("timestamp", FieldType.Date)
                            )
                        ).map(_.value)
                    }
                match {
                    case Right(feed) =>
                        logger.debug("Timestamp DataFeed registered.")
                        while (running) {
                            try {
                                val timestamp = Instant.now().toString
                                wallet.submitDataFeedValue(
                                    Collection(
                                        DataFeedValue(
                                            id = feed.id,
                                            content = Collection(timestamp)
                                        )
                                    )
                                ) match {
                                    case Right(v) =>
                                        logger.info(s"Submitted timestamp [$timestamp] in ${v.blockNumber}:${v.txId}")

                                    case Left(msg) =>
                                        logger.warn(s"Failed to submit timestamp: $msg")
                                }
                                //
                                Thread.sleep(processorTimeout)
                            }
                            catch {
                                case e: Throwable =>
                                    e.printStackTrace()
                                    running = false
                            }
                        }

                    case Left(msg) =>
                        running = false
                        throw new Exception(s"Cannot register timestamping data feed: $msg")
                }
        }
    }
}