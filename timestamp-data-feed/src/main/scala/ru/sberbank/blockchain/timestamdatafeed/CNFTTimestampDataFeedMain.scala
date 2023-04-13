package ru.sberbank.blockchain.timestamdatafeed

import org.slf4j.LoggerFactory
import ru.sberbank.blockchain.cnft.CurrentPlatformVersion

import java.util.Base64
import java.util.concurrent.TimeUnit

object CNFTTimestampDataFeedMain extends App {
    private val logger = LoggerFactory.getLogger(getClass)
    logger.info("Initializing Timestamp Data Feed...")
    logger.info(s"Platform version ${CurrentPlatformVersion}")

    private val gateUrl = Util.environmentMandatory("GATE_URL")
    //    private val logLevel = Util.environmentMandatory("LOG_LEVEL")

    private val timestampFeedIntervalMinutes =
        Util.environmentOptional("TIMESTAMP_FEED_INTERVAL",
            "1"
            //TimeUnit.HOURS.toMinutes(1).toString
        ).toLong

    private val adminWalletUrl = Util.environmentMandatory("ADMIN_WALLET_URL")
    private val walletCrypto = Util.environmentMandatory("WALLET_CRYPTO")

    private val walletData = Base64.getDecoder.decode(walletCrypto)
    private val timestampDataFeed = new TimestampDataFeed(
        adminWalletUrl, gateUrl, walletData,
        TimeUnit.MINUTES.toMillis(timestampFeedIntervalMinutes)
    )

    //
    Runtime.getRuntime.addShutdownHook(new Thread("SHUTDOWN") {
        override def run(): Unit = shutdown()
    })
    //

    timestampDataFeed.startup()


    logger.info("Started")


    //=========================================================================
    private def shutdown(): Unit = {
        logger.info("Timestamp Data Feed shutting down...")
        timestampDataFeed.shutdown()
        logger.info("Timestamp Data Feed shutdown complete.")
    }
}
