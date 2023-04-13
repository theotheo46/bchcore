package ru.sberbank.blockchain.cnft.engine.dna.emission

import ru.sberbank.blockchain.cnft.common.types._
import ru.sberbank.blockchain.cnft.engine.dna.{GeneExecutionContext, NoAdditionalFields, OperationInitiator}
import ru.sberbank.blockchain.cnft.model.{FieldMeta, FieldType, IssueTokenRequest, RequestActorType}

/**
 * @author Alexey Polubelov
 */
object ControlledBySmartContract extends EmissionGene with NoAdditionalFields {

    private val Issuer = "Issuer"

    override def genInstanceAttributes: Seq[FieldMeta] = Seq(FieldMeta(Issuer, FieldType.Text))

    override def canIssue(context: GeneExecutionContext, request: IssueTokenRequest): Result[Boolean] =
        context.initiator match {
            case OperationInitiator.Client =>
                Result.Fail("Emission is controlled by smart contract only")

            case OperationInitiator.SmartContract(id) =>
                for {
                    _ <-
                        Result.expect(
                            request.actors.length == 1 && request.actors.head.theType == RequestActorType.SmartContract,
                            s"Invalid actors: expected exactly one of type ${RequestActorType.SmartContract}"
                        )
                    _ <- Result.expect(
                        id == request.actors.head.value.toUTF8,
                        "Invalid actors: the actor must be id of smart contract"
                    )
                    _ <- Result.expect(
                        context.getType.typeId == id,
                        s"Not allowed, issue must be done by smart contract ${context.getType.typeId}"
                    )

                } yield true
        }

}
