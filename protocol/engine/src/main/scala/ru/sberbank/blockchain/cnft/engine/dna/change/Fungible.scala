package ru.sberbank.blockchain.cnft.engine.dna.change

import ru.sberbank.blockchain.cnft.commons.ROps.IterableR_Ops
import ru.sberbank.blockchain.cnft.commons.Result
import ru.sberbank.blockchain.cnft.engine.dna.{GeneExecutionContext, NoInstanceAttributes, TypeFieldsAccess}
import ru.sberbank.blockchain.cnft.model.{FieldMeta, FieldType, TokenChangeRequest}

import java.math.BigInteger
import scala.util.Try

/**
 * @author Alexey Polubelov
 */
object Fungible extends ChangeGene with TypeFieldsAccess with NoInstanceAttributes {
    private val Amount = "amount"

    def requireAdditionalFields: Seq[FieldMeta] = Seq(FieldMeta(Amount, FieldType.Numeric))

    override def canChange(context: GeneExecutionContext, changeRequest: TokenChangeRequest): Result[Boolean] =
        for {
            amountIndex <- getFieldIndex(context, Amount)
            content <- context.getTokenContent(changeRequest.tokenId)
            spendString <- content.fields.lift(amountIndex).toRight(s"Missing $Amount field")
            amountSpend <- Try(new BigInteger(spendString)).toOption.toRight(s"Invalid $Amount field value: $spendString")
            totalNewTokensAmount <- changeRequest.amounts.toSeq.mapR { amount =>
                for {
                    newTokenAmount <- Try(new BigInteger(amount)).toOption.toRight(s"Invalid $Amount value: $spendString")
                } yield newTokenAmount
            }
                .map(_.foldLeft(new BigInteger("0"))(_.add(_)))

        } yield amountSpend == totalNewTokensAmount

}