package org.enterprisedlt.fabric.client

import org.enterprisedlt.fabric.client.configuration.PeerConfig
import org.hyperledger.fabric.protos.peer.Chaincode
import org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions
import org.hyperledger.fabric.sdk._
import org.slf4j.LoggerFactory

import java.util
import scala.collection.JavaConverters.asJavaCollection
import scala.util.Try

/**
 * @author Alexey Polubelov
 */
class FabricChannel(
    fabricClient: FabricClient,
    fabricChannel: Channel,
    bootstrapOrderers: java.util.Collection[Orderer]
) {

    private val logger = LoggerFactory.getLogger(this.getClass)

    def getChainCode(name: String, discoveryForEndorsement: Boolean = false, discoveryForOrdering: Boolean = false /* TODO: , endorsementTimeout: Int = */): FabricChainCode =
        new FabricChainCode(
            fabricClient,
            fabricChannel,
            Chaincode.ChaincodeID.newBuilder().setName(name).build(),
            bootstrapOrderers,
            discoveryForEndorsement,
            discoveryForOrdering
        )

    //=========================================================================
    def setupBlockListener(listener: BlockListener): Try[String] = Try {
        fabricChannel.registerBlockListener(listener)
    }

    def getChannelConfigurationBytes: Array[Byte] = fabricChannel.getChannelConfigurationBytes

    def getChannelHeight: Either[String, Long] = {
        Try {
            fabricChannel.queryBlockchainInfo.getHeight
        }.toEither.left.map { err =>
            val msg = s"Error: ${err.getMessage}"
            logger.error(msg, err)
            msg
        }
    }

    def getBlockByNumber(blockNumber: Long): Either[String, BlockInfo] =
        Try(fabricChannel.queryBlockByNumber(blockNumber))
            .toEither.left.map { err =>
            val msg = s"Error: ${err.getMessage}"
            logger.error(msg, err)
            msg
        }


    def addPeer(config: PeerConfig): Either[String, String] = {
        Try {
            if (config.peerRoles.isEmpty) {
                fabricChannel.addPeer(fabricClient.mkPeer(config))
            } else {
                val peerRolesSet = util.EnumSet
                    .copyOf(
                        asJavaCollection(config.peerRoles)
                    )
                val peerOptions = createPeerOptions
                    .setPeerRoles(peerRolesSet)
                fabricChannel.addPeer(fabricClient.mkPeer(config), peerOptions)
            }
        }.toEither match {
            case Right(_) => Right("Success")
            case Left(err) =>
                val msg = s"Error: ${err.getMessage}"
                logger.error(msg, err)
                Left(msg)
        }
    }


}
