package ru.sberbank.blockchain.cnft.gate

import org.enterprisedlt.fabric.client.{ContractResult, FabricChannel}
import org.enterprisedlt.spec.BinaryCodec
import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.cnft.gate.service._
import ru.sberbank.blockchain.cnft.spec.CNFTSpec

/**
 * @author Alexey Polubelov
 */
case class CNFTGateImpl(
    cnft: CNFTSpec[ContractResult],
    channel: FabricChannel,
    chainCodeName: String,
    codec: BinaryCodec,
    POWDifficulty: Int
) extends CNFTGate[Result]
    with CNFTService
    with CNFTBlocksService
    with POWService
