package ru.sberbank.blockchain.cnft.engine.dna.burn

import ru.sberbank.blockchain.cnft.common.types._
import ru.sberbank.blockchain.cnft.engine.dna._
import ru.sberbank.blockchain.cnft.model.{OwnerType, SignedBurnRequest}

/**
 * @author Alexey Polubelov
 */
object BurnControlledBySmartContract extends BurnGene with NoAdditionalFields with NoInstanceAttributes {

    override def canBurn(context: GeneExecutionContext, tokenId: String, request: SignedBurnRequest): Result[Boolean] =
        context.getOwner(tokenId).flatMap { owner =>
            owner.ownerType match {
                case OwnerType.Signatures =>
                    Result.Fail(s"can not burn by owner signature")

                case OwnerType.SmartContractId =>
                    context.initiator match {
                        case OperationInitiator.Client =>
                            Result.Fail("Only SmartContract allowed to burn tokens from SmartContract address")

                        case OperationInitiator.SmartContract(id) =>
                            Result(id == owner.address.toUTF8 && context.geneParameters.contains(id))
                    }
            }
        }

}
