package ru.sberbank.blockchain.cnft.gate

import jakarta.servlet.DispatcherType
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{FilterHolder, ServletContextHandler, ServletHolder}
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.enterprisedlt.fabric.client.configuration.{Network, OSNConfig, PeerConfig, TLSPath}
import org.enterprisedlt.fabric.client.{ContractResult, FabricClient}
import org.enterprisedlt.general.codecs.ScalaPBCodec
import org.hyperledger.fabric.sdk.Peer.PeerRole
import org.slf4j.{Logger, LoggerFactory}
import ru.sberbank.blockchain.cnft.chaincode.CNFTChaincode
import ru.sberbank.blockchain.cnft.commons._
import ru.sberbank.blockchain.cnft.CurrentPlatformVersion
import ru.sberbank.blockchain.cnft.gate.filter.{AuditFilter, AuthenticationFilter, CachedBodyPostRequestsWrappingFilter, HashcashServletFilter}
import ru.sberbank.blockchain.cnft.gate.service.CNFTGate
import ru.sberbank.blockchain.common.cryptography.Hashcash
import ru.sberbank.blockchain.common.cryptography.bouncycastle.BouncyCastleHash
import tools.http.service.HttpServerService._

import java.nio.charset.Charset
import java.util

/**
 * @author Alexey Polubelov
 */
object CNFTGateMain extends App {
    private val logger = LoggerFactory.getLogger(getClass)
    logger.info("Initializing...")
    logger.info(s"Platform version ${CurrentPlatformVersion}")
    logger.info(s"Default encoding is: ${Charset.defaultCharset().name()}")
    //
    val gatePort = Util.environmentMandatory("CNFT_GATE_PORT").toInt
    val logLevel = Util.environmentMandatory("LOG_LEVEL")
    val organizationName = Util.environmentMandatory("ORG")
    val userCRTPath = Util.environmentMandatory("USER_CRT_PATH")
    val userKeyPath = Util.environmentMandatory("USER_KEY_PATH")
    val OSNHost = Util.environmentMandatory("ORDERER_HOST")
    val OSNPort = Util.environmentMandatory("ORDERER_PORT").toInt
    val OSNTLSCertificate = Util.environmentMandatory("ORDERER_KEY_CRT")
    val PeerHost = Util.environmentMandatory("PEER_HOST")
    val PeerPort = Util.environmentMandatory("PEER_PORT").toInt
    val PeerTLSCertificate = Util.environmentMandatory("PEER_KEY_CRT")
    val channelName = Util.environmentMandatory("CHANNEL_NAME")
    val CNFTChainCodeName = Util.environmentMandatory("CNFT_CHAINCODE")
    val peerDiscovery = Util.environmentOptional("PEER_DISCOVERY", "true").toBoolean
    val maxInboundMessageSize = Util.environmentOptional("MAX_INBOUND_MESSAGE_SIZE", "100").toInt * 1024 * 1024
    val hashcashDifficulty = Util.environmentMandatory("HASHCASH_DIFFICULTY").toInt
    val auditRequests = Util.environmentOptional("AUDIT_REQUESTS", "")

    setupLogger()
    //
    val fabricUser = Util.loadFabricUser(organizationName, userCRTPath, userKeyPath)
    val peerConfig =
        PeerConfig(
            name = s"${PeerHost}_$PeerPort",
            host = PeerHost,
            port = PeerPort,
            setting = TLSPath(PeerTLSCertificate),
            peerRoles = Array(
                PeerRole.SERVICE_DISCOVERY,
                PeerRole.LEDGER_QUERY,
                PeerRole.EVENT_SOURCE,
                PeerRole.CHAINCODE_QUERY,
                PeerRole.ENDORSING_PEER
            ),
            maxInboundMessageSize = maxInboundMessageSize
        )
    val fabricClient =
        new FabricClient(
            fabricUser,
            Network(
                ordering = Array(
                    OSNConfig("osn0", OSNHost, OSNPort, TLSPath(OSNTLSCertificate))
                ),
                peers = Array(peerConfig)
            )
        )
    val channel = fabricClient.channel(channelName)

    val pbCodec = new ScalaPBCodec()

    // keep the import below it's used in macro so idea don't see it :(

    import org.enterprisedlt.general.codecs.proto._

    val cnft = channel
        .getChainCode(CNFTChainCodeName, peerDiscovery, discoveryForOrdering = false)
        .as[CNFTChaincode[ContractResult]]

    val gate = CNFTGateImpl(cnft, channel, CNFTChainCodeName, pbCodec, hashcashDifficulty)

    val server = new Server(gatePort)

    val context = new ServletContextHandler()
    context.setContextPath("/")
    val CORSFilter = new FilterHolder(classOf[CrossOriginFilter])
    CORSFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*")
    CORSFilter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "*")
    CORSFilter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "POST, DELETE, PUT, GET, HEAD, OPTIONS")

    context.addServlet(new ServletHolder(gateService(gate)), "/*")
    context.addFilter(CORSFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST))

    val cachedBodyWrapperFilter = new FilterHolder(classOf[CachedBodyPostRequestsWrappingFilter])
    context.addFilter(cachedBodyWrapperFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST))

    // Hashcash proof checking filter
    val hashcashServletFilter = new FilterHolder(
        new HashcashServletFilter(
            new Hashcash(BouncyCastleHash),
            hashcashDifficulty
        )
    )
    context.addFilter(hashcashServletFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST))

    // Sag Authorization filter
    val AuthFilter = new FilterHolder(new AuthenticationFilter(gate))
    context.addFilter(AuthFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST))

    // Audit Filter
    val auditFilter = new FilterHolder(
        new AuditFilter(
            auditRequests.split(",")
        ))
    context.addFilter(auditFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST))

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
        logger.info("Shutting down...")
        server.stop()
        logger.info("Shutdown complete.")
    }

    //private def handlers(handlers: Handler*) = new HandlerList(handlers: _*)

    private def gateService(gateImpl: CNFTGateImpl) =
        serviceHandler[CNFTGate[Result]](gateImpl)

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
            .getLogger("ru.sberbank.blockchain.cnft.gate")
            .asInstanceOf[ch.qos.logback.classic.Logger]
            .setLevel(ch.qos.logback.classic.Level.valueOf(logLevel))

    }
}
