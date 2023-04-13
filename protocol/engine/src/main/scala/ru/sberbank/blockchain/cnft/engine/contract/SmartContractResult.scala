package ru.sberbank.blockchain.cnft.engine.contract

import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.model.{BurnRequest, DealRequest, IssueTokenRequest, SmartContractState, TokenChangeRequestSmartContract}

/**
 * @author Alexey Polubelov
 */
case class SmartContractResult(
    emission: Collection[IssueTokenRequest],
    transfer: Collection[DealRequest],
    change: Collection[TokenChangeRequestSmartContract],
    burn: Collection[BurnRequest],
    stateUpdate: Option[SmartContractState] = None
) {

    def +(other: SmartContractResult): SmartContractResult =
        this.copy(
            emission = this.emission ++ other.emission,
            transfer = this.transfer ++ other.transfer,
            change = this.change ++ other.change,
            burn = this.burn ++ other.burn
        )

}

object SmartContractResult {
    def empty: SmartContractResult = SmartContractResult(Collection.empty, Collection.empty, Collection.empty, Collection.empty, None)
}