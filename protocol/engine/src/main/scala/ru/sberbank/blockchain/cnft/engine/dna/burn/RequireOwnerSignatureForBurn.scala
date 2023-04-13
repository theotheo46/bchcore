package ru.sberbank.blockchain.cnft.engine.dna.burn

import ru.sberbank.blockchain.cnft.common.types._
import ru.sberbank.blockchain.cnft.engine.dna._
import ru.sberbank.blockchain.cnft.model.{OwnerType, Signatures, SignedBurnRequest}

/**
 * @author Alexey Polubelov
 */
object RequireOwnerSignatureForBurn extends BurnGene with SignatureVerify with NoAdditionalFields with NoInstanceAttributes {

    override def canBurn(context: GeneExecutionContext, tokenId: String, request: SignedBurnRequest): Result[Boolean] =
        context.getOwner(tokenId).flatMap { owner =>
            owner.ownerType match {
                case OwnerType.Signatures =>
                    for {
                        address <- Result(Signatures.parseFrom(owner.address))
                        isValid =
                            verifySignatures(
                                context, request.request.toBytes, address,
                                request.signatures
                                    .filter(_.tokenId == tokenId)
                                    .map(_.signature)
                            )
                        _ <- if (isValid) Result(()) else Result.Fail(s"Not enough signatures")
                    } yield isValid

                case OwnerType.SmartContractId =>
                    context.initiator match {
                        case OperationInitiator.Client =>
                            Result.Fail("Only SmartContract allowed to burn tokens from SmartContract address")

                        case OperationInitiator.SmartContract(id) =>
                            Result(id == owner.address.toUTF8)
                    }
            }
        }

}
