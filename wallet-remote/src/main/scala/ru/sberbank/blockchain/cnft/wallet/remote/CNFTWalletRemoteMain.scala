package ru.sberbank.blockchain.cnft.wallet.remote

import jakarta.servlet.DispatcherType
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{FilterHolder, ServletContextHandler, ServletHolder}
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.slf4j.{Logger, LoggerFactory}
import ru.sberbank.blockchain.cnft.common.types.Result
import ru.sberbank.blockchain.cnft.CurrentPlatformVersion
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.spec.CNFTWalletSpec
import ru.sberbank.blockchain.cnft.wallet.store.HDPathStore
import ru.sberbank.blockchain.cnft.wallet.{CNFT, CNFTCrypto}
import tools.http.service.HttpServerService._
import tools.http.service.ServiceJettyHandler

import java.util
import java.util.Base64

/**
 * @author Alexey Polubelov
 */
object CNFTWalletRemoteMain extends App {
    private val logLevel = Util.environmentMandatory("LOG_LEVEL")
    setupLogger()
    private val logger = LoggerFactory.getLogger(getClass)
    logger.info("Initializing Wallet Remote...")
    logger.info(s"Platform version ${CurrentPlatformVersion}")

    //
    import ru.sberbank.blockchain.BytesAsBase64RW
    private val walletCrypto = Util.environmentMandatory("WALLET_CRYPTO")

    val configWalletRemoteIdentityOperations = "WALLET_REMOTE_IDENTITY_OPERATIONS"
    val configWalletRemoteAddressOperations = "WALLET_REMOTE_ADDRESS_OPERATIONS"

    val gateUrl = Util.environmentMandatory("GATE_URL")
    val walletRemotePort = Util.environmentMandatory("WALLET_REMOTE_PORT").toInt
    val walletRemoteIdentityOperations = Util.environmentOptional(configWalletRemoteIdentityOperations, "hd")
    val walletRemoteAddressOperations = Util.environmentOptional(configWalletRemoteAddressOperations, "hd")

    val adminWalletUrl = Util.environmentOptional("ADMIN_WALLET_URL")

    def opsConfig(param: String, url: String) =
        url match {
            case "hd" =>
                CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory())
            case url if url.startsWith("remote:http://") =>
                CNFTCrypto.remoteSignatureOperations(url.drop(7))
            case _ =>
                throw new IllegalArgumentException(s"unexpected config parameter $url for $param")
        }


    val cryptoContext =
        CNFTCrypto
            .newContext(
                opsConfig(configWalletRemoteIdentityOperations, walletRemoteIdentityOperations),
                CNFTCrypto.bouncyCastleEncryption(),
                CNFTCrypto.bouncyCastleAccessOperations(),
                opsConfig(configWalletRemoteAddressOperations, walletRemoteAddressOperations),
                CNFTCrypto.hash(),
                CNFTCrypto.secureRandomGenerator()
            )

    val walletData = Base64.getDecoder.decode(walletCrypto)
    val crypto = cryptoContext.importFrom(walletData).orFail("Unable to create AccessOperations")

    val chain = CNFT.connect(gateUrl)
    val wallet = chain.newWallet(crypto).orFail("Failed to create wallet")

    val member = crypto.memberInformation().orFail("Can not obtain member information")
    chain.getMember(member.id) match {
        case Left(_) =>
            logger.info("Member does not registered yet")
            val tx = adminWalletUrl match {
                case Some(url) =>
                    logger.info(s"Trying register self using admin wallet $url")
                    val adminWallet = chain.connectWallet(url)
                    adminWallet.registerMember(member).orFail("Failed to register self in blockchain")
                case None =>
                    logger.info(s"Trying register self directly")
                    wallet.registerMember(member).orFail("Failed to register self in blockchain")
            }
            logger.info(s"Registered self [${member.id}] in TX: [${tx.blockNumber} : ${tx.txId}]")

        case Right(_) =>
            logger.info("Member already registered")
    }

    val server = new Server(walletRemotePort)

    val context = new ServletContextHandler()
    context.setContextPath("/")
    val CORSFilter = new FilterHolder(classOf[CrossOriginFilter])
    CORSFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*")
    CORSFilter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "*")
    CORSFilter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "POST, DELETE, PUT, GET, HEAD, OPTIONS")

    context.addServlet(new ServletHolder(walletService(wallet)), "/*")
    context.addFilter(CORSFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST))

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
        logger.info("Wallet Remote shutting down...")
        server.stop()
        logger.info("Wallet Remote shutdown complete.")
    }

    private def walletService(wallet: CNFTWalletSpec[Result]): ServiceJettyHandler[CNFTWalletSpec[Result]] =
        serviceHandler[CNFTWalletSpec[Result]](wallet)


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
            .getLogger("ru.sberbank.blockchain.cnft.wallet.remote")
            .asInstanceOf[ch.qos.logback.classic.Logger]
            .setLevel(ch.qos.logback.classic.Level.valueOf(logLevel))

    }

}
