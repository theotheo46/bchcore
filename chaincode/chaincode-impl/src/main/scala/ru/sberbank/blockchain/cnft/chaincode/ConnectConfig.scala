package ru.sberbank.blockchain.cnft.chaincode

/**
 * @author Alexey Polubelov
 */
case class ConnectConfig(
    peer_address: String,
    chaincode_id: String,
    client_cert: String,
    client_key: String,
    root_cert: String
)
