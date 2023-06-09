package org.enterprisedlt.fabric.client

import java.util
import java.util.Properties
import org.enterprisedlt.fabric.client.configuration._
import org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions
import org.hyperledger.fabric.sdk._
import org.hyperledger.fabric.sdk.security.CryptoSuite

import scala.collection.JavaConverters._

/**
 * @author Alexey Polubelov
 */
class FabricClient(
    user: User,
    network: Network
) {
    private val cryptoSuite = CryptoSuite.Factory.getCryptoSuite()
    private val fabricClient: HFClient = getHFClient(user)

    private def getHFClient(user: User): HFClient = {
        val client = HFClient.createNewInstance()
        client.setCryptoSuite(cryptoSuite)
        client.setUserContext(user)
        client
    }

    def channel(name: String): FabricChannel = {
        val channel = fabricClient.newChannel(name)
        network.ordering.foreach { config =>
            channel.addOrderer(mkOSN(config))
        }
        network.peers.foreach { config =>
            if (config.peerRoles.isEmpty) {
                channel.addPeer(mkPeer(config))
            } else {
                val peerRolesSet = util.EnumSet
                    .copyOf(
                        asJavaCollection(config.peerRoles)
                    )
                val peerOptions = createPeerOptions
                    .setPeerRoles(peerRolesSet)
                channel.addPeer(mkPeer(config), peerOptions)
            }
        }
        val bootstrapOrderers = channel.getOrderers
        channel.initialize()
        new FabricChannel(this, channel, bootstrapOrderers)
    }


    private[client] def mkPeer(config: PeerConfig): Peer = {
        val properties = new Properties()
        properties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", config.maxInboundMessageSize)
        config.setting match {
            case Plain =>
                fabricClient.newPeer(config.name, s"grpc://${config.host}:${config.port}", properties)
            case TLSPath(path, hostnameOverride) =>
                properties.put("pemFile", path)
                hostnameOverride.foreach(hostnameOverride => properties.put("hostnameOverride", hostnameOverride))
                fabricClient.newPeer(config.name, s"grpcs://${config.host}:${config.port}", properties)
            case TLSPem(certificatePem, clientKeyPem, clientCertPem, hostnameOverride) =>
                certificatePem.foreach(certificatePem => properties.put("pemBytes", certificatePem))
                clientKeyPem.foreach(clientKeyPem => properties.put("clientKeyBytes", clientKeyPem))
                clientCertPem.foreach(clientCertPem => properties.put("clientCertBytes", clientCertPem))
                hostnameOverride.foreach(hostnameOverride => properties.put("hostnameOverride", hostnameOverride))
                fabricClient.newPeer(config.name, s"grpcs://${config.host}:${config.port}", properties)


        }
    }

    private[client] def mkOSN(config: OSNConfig): Orderer = {
        config.setting match {
            case Plain =>
                fabricClient.newOrderer(config.name, s"grpc://${config.host}:${config.port}")
            case TLSPath(path, hostnameOverride) =>
                val properties = new Properties()
                properties.put("pemFile", path)
                hostnameOverride.foreach(hostnameOverride => properties.put("hostnameOverride", hostnameOverride))
                fabricClient.newOrderer(config.name, s"grpcs://${config.host}:${config.port}", properties)
            case TLSPem(certificatePem, clientKeyPem, clientCertPem, hostnameOverride) =>
                val properties = new Properties()
                certificatePem.foreach(certificatePem => properties.put("pemBytes", certificatePem))
                clientKeyPem.foreach(clientKeyPem => properties.put("clientKeyBytes", clientKeyPem))
                clientCertPem.foreach(clientCertPem => properties.put("clientCertBytes", clientCertPem))
                hostnameOverride.foreach(hostnameOverride => properties.put("hostnameOverride", hostnameOverride))
                fabricClient.newOrderer(config.name, s"grpcs://${config.host}:${config.port}", properties)

        }
    }

    private[client] def newQueryProposalRequest(): QueryByChaincodeRequest = fabricClient.newQueryProposalRequest()

    private[client] def newQueryProposalRequestDefiniton(): QueryLifecycleQueryChaincodeDefinitionRequest = fabricClient.newQueryLifecycleQueryChaincodeDefinitionRequest()

    private[client] def newTransactionProposalRequest(): TransactionProposalRequest = fabricClient.newTransactionProposalRequest()

    private[client] def getUserContext: User = fabricClient.getUserContext

}
