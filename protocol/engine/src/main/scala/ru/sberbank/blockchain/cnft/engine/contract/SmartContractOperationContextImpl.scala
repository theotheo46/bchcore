package ru.sberbank.blockchain.cnft.engine.contract

import ru.sberbank.blockchain.cnft.common.types._
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.engine.contract.ICOSmartContract.{BigIntegerOps, StringOps}
import ru.sberbank.blockchain.cnft.engine.{CNFTStore, TransactionContext}
import ru.sberbank.blockchain.cnft.model.{AcceptedDeal, DataFeed, DataFeedValue, SmartContract, SmartContractState, TokenChangeRequestSmartContract, TokenContent, TokenId, TokenOwner, TokenToCreate, TokenType}

import java.math.BigInteger

/**
 * @author Alexey Polubelov
 */
case class SmartContractOperationContextImpl(
    contract: SmartContract,
    state_ : SmartContractState,
    store: CNFTStore,
    txContext: TransactionContext,
    currentDeal: Option[AcceptedDeal] = None
) extends SmartContractOperationContext with LoggingSupport {

    // State changes will be buffer

    private var stateUpdate_ : Option[SmartContractState] = None

    override def stateUpdate: Option[SmartContractState] = stateUpdate_

    override def updateStateField(index: Int, value: String): Unit = {
        val current = stateUpdate_.getOrElse(state_)
        stateUpdate_ =
            Some(
                current.copy(
                    state = current.state.updated(index, value)
                )
            )
    }

    override def state: SmartContractState = stateUpdate.getOrElse(state_)

    override def completeContract(): Unit = {
        val current = stateUpdate_.getOrElse(state_)
        stateUpdate_ = Some(current.copy(alive = false))
    }

    //

    override def nextUniqueId: String = txContext.nextUniqueId

    override def timestamp: String = txContext.timestamp

    override def getTokenContent(tokenId: String): Option[TokenContent] = store.getTokenBody(tokenId)

    override def getTokenType(typeId: String): Option[TokenType] = store.getTokenType(typeId)

    override def getTokenOwner(tokenId: String): TokenOwner =
        store.getTokenOwner(tokenId)
            .getOrElse(throw new Exception(s"Invalid token id: ${tokenId}"))

    override def getDataFeed(id: String): Result[DataFeed] =
        store.getDataFeed(id).toRight(s"Feed [${id}] does not exists")

    override def getFeedValue(feedId: String): Result[DataFeedValue] =
        store.getDataFeedValue(feedId).toRight(s"Feed [${feedId}] does not exists")

    override def dealsAccepted: Collection[AcceptedDeal] =
        store.getSmartContractAcceptedDeals(contract.id)

    override def changeTokens(toChangeId: String, neededVals: Collection[BigInteger]): Result[TokenChangeRequestSmartContract] = {

        for {
            changeTokenValue <- getTokenValue(toChangeId)
            tokenTypeId <- TokenId.from(toChangeId).map(_.typeId)
            diff = changeTokenValue.subtract(neededVals.fold(new BigInteger("0"))(_.add(_)))
            _ <- ResultOps.expect(diff === "0".toNumeric, s"token $toChangeId can not be changed to requested values")

            toCreateIds <- neededVals.mapR { amount =>
                TokenId.encode(tokenTypeId, txContext.nextUniqueId)
                    .map(id =>
                        TokenToCreate(id, amount.toString())
                    )
            }

        } yield
            TokenChangeRequestSmartContract(
                toChangeId,
                toCreateIds
            )
    }

    override def getTokenValue(tokenId: String): Result[BigInteger] =

        for {
            tokenContent <- ResultOps.fromOption(store.getTokenBody(tokenId), s"token $tokenId does not exist")
            amount <- Result {
                tokenContent.fields.head
            }
            _ <- Result {
                logger.debug(s"token value $amount")
            }
        } yield amount.toNumeric
}
