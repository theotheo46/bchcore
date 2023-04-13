package ru.sberbank.blockchain.cnft.engine.dna.transfer

import ru.sberbank.blockchain.cnft.common.types._
import ru.sberbank.blockchain.cnft.engine.dna._
import ru.sberbank.blockchain.cnft.model.{DealLeg, DealRequest, OwnerType, Signatures}

/**
 * @author Alexey Polubelov
 */
object RequireOwnerSignatureForTransfer extends TransferGene with SignatureVerify with NoAdditionalFields with NoInstanceAttributes {

    override def canTransfer(context: GeneExecutionContext, leg: DealLeg, dealRequest: DealRequest): Result[Boolean] =
        context.getOwner(leg.tokenId).flatMap { owner =>
            owner.ownerType match {
                case OwnerType.Signatures =>
                    val tokenId = leg.tokenId
                    for {
                        address <- Result(Signatures.parseFrom(owner.address))
                        isValid =
                            verifySignatures(context,
                                dealRequest.deal.toBytes,
                                address,
                                dealRequest.ownerSignatures
                                    .filter(_.tokenId == tokenId)
                                    .map(_.signature)
                            )
                        _ <- if (isValid) Result(()) else Result.Fail(s"No signatures for $tokenId")
                    } yield true

                case OwnerType.SmartContractId =>
                    context.initiator match {
                        case OperationInitiator.Client =>
                            Result.Fail("Only SmartContract allowed to move tokens from SmartContract address")

                        case OperationInitiator.SmartContract(id) =>
                            Result(id == owner.address.toUTF8)
                    }
            }
        }

}