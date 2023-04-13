package ru.sberbank.blockchain.cnft

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.slf4j.{Logger, LoggerFactory}
import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.cnft.megacuks.{MegaCuksConfiguration, MegaCuksCryptography, MegaCuksKeyService, MegaCuksService}
import ru.sberbank.blockchain.common.cryptography.SignatureOperations
import tools.http.service.HttpServerService._


object CNFTMegacuks extends App {
    private implicit val logger: Logger = LoggerFactory.getLogger("MegaCuks")
    private val logLevel = Util.environmentOptional("LOG_LEVEL", "debug")
    setupLogger()
    logger.info("Initializing Megacuks...")
    logger.info(s"Platform version ${CurrentPlatformVersion}")

    //

    val serverPort = Util.environmentMandatory("MEGACUKS_SERVER_PORT").toInt
    val server = new Server(serverPort)

    val context = new ServletContextHandler()
    context.setContextPath("/")

    val serviceURL = Util.environmentMandatory("MEGACUKS_SERVICE_URL")

    private val cryptoMCS =
        new MegaCuksCryptography(
            megaCuksService = MegaCuksService.newMegaCuksService(serviceURL),
            MegaCuksConfiguration(
                megaCuksVerifyService = Util.environmentMandatory("MEGACUKS_VERIFY_SERVICE"),
                megaCuksSystemId = Util.environmentMandatory("MEGACUKS_SYSTEM_ID"),
                megaCuksBsnCode = Util.environmentMandatory("MEGACUKS_BSN_CODE"),
                defaultKeyService =
                    MegaCuksKeyService(
                        id = Util.environmentMandatory("MEGACUKS_DEFAULT_KEY_SERVICE_ID"),
                        certificateB64 = Util.environmentMandatory("MEGACUKS_DEFAULT_KEY_SERVICE_CERTIFICATE_B64"),
                    )
            )
        )

    context.addServlet(new ServletHolder(megacuksService(cryptoMCS)), "/*")

    server.setHandler(context)

    //
    Runtime.getRuntime.addShutdownHook(new Thread("SHUTDOWN") {
        override def run(): Unit = shutdown()
    })
    //
    server.start()

    logger.info("Started")


    //=========================================================================
    private def shutdown(): Unit = {
        logger.info("megacuks shutting down...")
        server.stop()
        logger.info("megacuks shutdown complete.")
    }

    private def megacuksService(mcs: SignatureOperations[Result]) =
        serviceHandler[SignatureOperations[Result]](mcs)

    private def setupLogger(): Unit = {
        LoggerFactory
            .getLogger(Logger.ROOT_LOGGER_NAME)
            .asInstanceOf[ch.qos.logback.classic.Logger]
            .setLevel(ch.qos.logback.classic.Level.valueOf(logLevel))
        LoggerFactory
            .getLogger("org.eclipse.jetty")
            .asInstanceOf[ch.qos.logback.classic.Logger]
            .setLevel(ch.qos.logback.classic.Level.valueOf(logLevel))
        LoggerFactory
            .getLogger("ru.sberbank.blockchain.cnft.megacuks")
            .asInstanceOf[ch.qos.logback.classic.Logger]
            .setLevel(ch.qos.logback.classic.Level.valueOf(logLevel))

    }
}

