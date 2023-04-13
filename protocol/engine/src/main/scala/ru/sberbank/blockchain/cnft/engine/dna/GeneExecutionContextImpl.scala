package ru.sberbank.blockchain.cnft.engine.dna

import ru.sberbank.blockchain.cnft.common.types.{Collection, Result}
import ru.sberbank.blockchain.cnft.engine.CNFTStore
import ru.sberbank.blockchain.cnft.model.{DataFeedValue, MemberInformation, TokenContent, TokenOwner, TokenType}
import ru.sberbank.blockchain.common.cryptography.SignatureOperations

/**
 * @author Alexey Polubelov
 */
case class GeneExecutionContextImpl(
    store: CNFTStore,
    cryptographicOperations: SignatureOperations[Result],
    tokenType: TokenType,
    initiator: OperationInitiator,
    geneParameters: Collection[String],
    ownersOverride: Map[String, TokenOwner]
) extends GeneExecutionContext {

    override def getType: TokenType = tokenType

    override def getMember(id: String): Result[MemberInformation] =
        store.getMemberInfo(id).toRight(s"Invalid member: $id")

    override def getTokenContent(tokenId: String): Result[TokenContent] =
        store.getTokenBody(tokenId)
            .toRight(s"Invalid token: ${tokenId}")

    override def getOwner(tokenId: String): Result[TokenOwner] =
        ownersOverride.get(tokenId)
            .map { a => Result(a) }
            .getOrElse(
                store.getTokenOwner(tokenId)
            )


    override def cryptography: SignatureOperations[Result] = cryptographicOperations

    override def getFeedValue(feedId: String): Result[DataFeedValue] =
        store.getDataFeedValue(feedId).toRight(s"Feed [$feedId] does not exists")
}

