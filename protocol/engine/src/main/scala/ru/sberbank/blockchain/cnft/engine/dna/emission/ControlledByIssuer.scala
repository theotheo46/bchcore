package ru.sberbank.blockchain.cnft.engine.dna.emission

import ru.sberbank.blockchain.cnft.common.types.Result
import ru.sberbank.blockchain.cnft.engine.dna.{GeneExecutionContext, NoAdditionalFields, OperationInitiator}
import ru.sberbank.blockchain.cnft.model.{FieldMeta, FieldType, IssueTokenRequest, MemberSignature, RequestActorType}

/**
 * @author Alexey Polubelov
 */
object ControlledByIssuer extends EmissionGene with NoAdditionalFields {

    private val Issuer = "Issuer"

    override def genInstanceAttributes: Seq[FieldMeta] = Seq(FieldMeta(Issuer, FieldType.Text))

    override def canIssue(context: GeneExecutionContext, request: IssueTokenRequest): Result[Boolean] =
        context.initiator match {
            case OperationInitiator.Client =>
                val typeIssuerId = context.getType.issuerId

                for {
                    _ <-
                        Result.expect(
                            request.actors.length == 1 && request.actors.head.theType == RequestActorType.Member,
                            s"Invalid actors: expected exactly one of type ${RequestActorType.Member}"
                        )
                    issuerActor <- Result(MemberSignature.parseFrom(request.actors.head.value))
                    _ <-
                        Result.expect(
                            issuerActor.memberId == typeIssuerId,
                            s"Invalid actors: require signature from $typeIssuerId"
                        )
                    issuer <- context.getMember(typeIssuerId)
                    signatureIsOk <-
                        context.cryptography.verifySignature(
                            issuer.signingPublic, request.issue.toBytes, issuerActor.value
                        )

                } yield signatureIsOk

            case OperationInitiator.SmartContract(_) =>
                Result.Fail("Emission is controlled by issuer only")
        }

}
