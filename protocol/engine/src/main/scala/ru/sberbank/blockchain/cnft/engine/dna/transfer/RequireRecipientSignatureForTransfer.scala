package ru.sberbank.blockchain.cnft.engine.dna.transfer

import ru.sberbank.blockchain.cnft.common.types.Result
import ru.sberbank.blockchain.cnft.engine.dna._
import ru.sberbank.blockchain.cnft.model.{DealLeg, DealRequest, OwnerType, Signatures}

/**
 * @author Alexey Polubelov
 */
object RequireRecipientSignatureForTransfer extends TransferGene with SignatureVerify with NoAdditionalFields with NoInstanceAttributes {

    override def canTransfer(context: GeneExecutionContext, leg: DealLeg, dealRequest: DealRequest): Result[Boolean] = {
        val owner = leg.newOwner
        owner.ownerType match {
            case OwnerType.Signatures =>
                for {
                    address <- Result(Signatures.parseFrom(owner.address))
                } yield
                    verifySignatures(context,
                        dealRequest.deal.toBytes, address,
                        dealRequest.recipientSignatures.filter(_.tokenId == leg.tokenId).map(_.signature)
                    ) // if false is returned the operation will go to pending

            case OwnerType.SmartContractId =>
                context.initiator match {
                    case OperationInitiator.Client =>
                        Result(true) // smart will accept or reject the deal according to it's logic

                    case OperationInitiator.SmartContract(_) =>
                        Result.Fail("It's not allowed to transfer from SmartContract to SmartContract")
                }
        }
    }
}
