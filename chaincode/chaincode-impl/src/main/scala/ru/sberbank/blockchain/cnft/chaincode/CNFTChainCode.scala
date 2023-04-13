package ru.sberbank.blockchain.cnft.chaincode

import com.google.gson.Gson
import org.bouncycastle.util.encoders.Hex
import org.enterprisedlt.fabric.contract.{FabricChainCode, OperationContext}
import org.enterprisedlt.spec.ContractResult
import org.hyperledger.fabric.shim.ChaincodeServerProperties
import org.slf4j.{Logger, LoggerFactory}
import ru.sberbank.blockchain.cnft.chaincode.store.ChaincodeCNFTStore
import ru.sberbank.blockchain.cnft.commons.{Base58, LoggingSupport, Result}
import ru.sberbank.blockchain.cnft.engine.{CNFTEngine, CNFTStore, TransactionContext}
import ru.sberbank.blockchain.common.cryptography.{SignatureOperations, VerifyOnlyCryptography}

import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util
import java.util.Base64

/**
 * @author Maxim Fedin
 */
object CNFTChainCode extends App with LoggingSupport {

    private object CNFTChaincodeImpl extends CNFTEngine with CNFTChaincode[ContractResult] {
        val store: CNFTStore = ChaincodeCNFTStore
        val cryptography: SignatureOperations[Result] = new VerifyOnlyCryptography
        override val txContext: TransactionContext = new TransactionContext {

            override def txId: String = OperationContext.transaction.id

            override def nextUniqueId: String = {
                val tx = OperationContext.transaction
                val txBytes = Hex.decode(tx.id)
                if (txBytes.length != 32) throw new Exception(s"Unexpected TX ID length, expected 32 byte, got: ${txBytes.length}")
                val number = tx.counter.next
                if (number > Integer.MAX_VALUE) throw new Exception(s"Tx local counter exceeded : $number")
                val result = util.Arrays.copyOf(txBytes, 36)
                result(32) = (number >>> 0).toByte
                result(33) = (number >>> 8).toByte
                result(34) = (number >>> 16).toByte
                result(35) = (number >>> 24).toByte
                Base58.encode(result)
            }

            override def timestamp: String = OperationContext.transaction.timestamp.toString
        }
    }

    //NOTE: keep the import below

    import org.enterprisedlt.general.codecs.proto._

    private val chaincode = new FabricChainCode[CNFTChaincode[ContractResult]](CNFTChaincodeImpl, Util.ContractCodecs)

    // start SHIM chain code
    (sys.env.get("CHAINCODE_SERVER_PORT").map(_.toInt), sys.env.get("CORE_CHAINCODE_ID_NAME")) match {
        case (Some(serverPort), Some(_)) => // server mode:
            val config = new ChaincodeServerProperties(
                serverPort,
                sys.env.getOrElse("MAX_INBOUND_METADATA_SIZE", "100").toInt * 1024 * 1024,
                sys.env.getOrElse("MAX_INBOUND_MESSAGE_SIZE", "100").toInt * 1024 * 1024,
                sys.env.getOrElse("MAX_CONNECTION_AGE_SECONDS", "5").toInt,
                sys.env.getOrElse("KEEP_ALIVE_TIMEOUT_SECONDS", "20").toInt,
                sys.env.getOrElse("PERMIT_KEEP_ALIVE_TIME_MINUTES", "1").toInt,
                sys.env.getOrElse("KEEP_ALIVE_TIME_MINUTES", "1").toInt,
                sys.env.getOrElse("PERMIT_KEEP_ALIVE_WITHOUT_CALLS", "true").toBoolean
            )
            chaincode.startAsServer(config, args)
        case _ => // legacy mode:
            chaincode.start(mkArgs(args))
    }

    // setup log levels
    LoggerFactory
        .getLogger(Logger.ROOT_LOGGER_NAME)
        .asInstanceOf[ch.qos.logback.classic.Logger]
        .setLevel(ch.qos.logback.classic.Level.TRACE)
    LoggerFactory
        .getLogger(this.getClass.getPackage.getName)
        .asInstanceOf[ch.qos.logback.classic.Logger]
        .setLevel(ch.qos.logback.classic.Level.TRACE)
    LoggerFactory
        .getLogger(classOf[FabricChainCode[_]].getPackage.getName)
        .asInstanceOf[ch.qos.logback.classic.Logger]
        .setLevel(ch.qos.logback.classic.Level.TRACE)


    //
    private lazy val rootCertPath = sys.env.getOrElse("CORE_PEER_TLS_ROOTCERT_FILE", throw new Exception(s"Mandatory variable CORE_PEER_TLS_ROOTCERT_FILE is missing!"))
    private lazy val clientKeyPath = sys.env.getOrElse("CORE_TLS_CLIENT_KEY_PATH", throw new Exception(s"Mandatory variable CORE_TLS_CLIENT_KEY_PATH is missing!"))
    private lazy val clientCertPath = sys.env.getOrElse("CORE_TLS_CLIENT_CERT_PATH", throw new Exception(s"Mandatory variable CORE_TLS_CLIENT_CERT_PATH is missing!"))

    private def mkArgs(args: Array[String]): Array[String] = if (args.length == 1 && args(0).endsWith(".json")) {
        val path = args(0)
        val config = load(path)
        dumpToFile(rootCertPath, config.root_cert)
        dumpToFile(clientKeyPath, Base64.getEncoder.encodeToString(config.client_key.getBytes(StandardCharsets.UTF_8)))
        dumpToFile(clientCertPath, Base64.getEncoder.encodeToString(config.client_cert.getBytes(StandardCharsets.UTF_8)))

        Array("-a", config.peer_address, "-i", config.chaincode_id)
    }
    else args

    private def dumpToFile(fileName: String, content: String): Unit = {
        val fileWriter = new FileWriter(fileName)
        try {
            fileWriter.write(content)
        } finally fileWriter.close()
    }

    private def load(path: String): ConnectConfig = {
        val gson = new Gson
        val reader = Files.newBufferedReader(Paths.get(path))
        gson.fromJson(reader, classOf[ConnectConfig])
    }

}

