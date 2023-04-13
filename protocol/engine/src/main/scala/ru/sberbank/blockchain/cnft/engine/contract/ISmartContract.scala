package ru.sberbank.blockchain.cnft.engine.contract

import ru.sberbank.blockchain.cnft.common.types.{Collection, Result}
import ru.sberbank.blockchain.cnft.model.{AcceptedToken, DataFeedValue, SmartContractState, SmartContractTemplate}

/**
 * @author Alexey Polubelov
 */
trait ISmartContract {

    def templateInformation: SmartContractTemplate

    def initialize(context: SmartContractOperationContext): Result[SmartContractState]

    def acceptTransfer(
        context: SmartContractOperationContext,
        tokens: Collection[AcceptedToken],
    ): Result[SmartContractResult]

    def processDataFeed(
        context: SmartContractOperationContext,
        dataFeedValue: DataFeedValue
    ): Result[SmartContractResult]

    //    // TODO: move this to Effect
    //    // returns updated smart contract if there were any changes
    //    def getStateUpdate(context: SmartContractOperationContext): Option[SmartContractState]
}
