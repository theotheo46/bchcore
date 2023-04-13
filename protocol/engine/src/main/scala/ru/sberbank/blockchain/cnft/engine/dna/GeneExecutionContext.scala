package ru.sberbank.blockchain.cnft.engine.dna

import ru.sberbank.blockchain.cnft.common.types._
import ru.sberbank.blockchain.cnft.model.{DataFeedValue, MemberInformation, TokenContent, TokenOwner, TokenType}
import ru.sberbank.blockchain.common.cryptography.SignatureOperations

/**
 * @author Alexey Polubelov
 */
trait GeneExecutionContext {

    def geneParameters: Collection[String]

    def getType: TokenType

    def getMember(id: String): Result[MemberInformation]

    def getTokenContent(tokenId: String): Result[TokenContent]

    def getOwner(tokenId: String): Result[TokenOwner]

    def cryptography: SignatureOperations[Result]

    def initiator: OperationInitiator

    def getFeedValue(feedId: String): Result[DataFeedValue]
}



