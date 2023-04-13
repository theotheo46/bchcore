package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.commons.{ROps, collectionFromArray}
import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import ru.sberbank.blockchain.cnft.wallet.spec.DataFeedEvents
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity

import scala.language.higherKinds

class DataFeedEventsExtractor[R[+_]](
    val id: WalletIdentity,
)(implicit val R: ROps[R]) {
    def extract(block: BlockEvents): R[DataFeedEvents] = R {
        DataFeedEvents(
            registeredDataFeed = collectionFromArray(block.dataFeedRegistered.map(_.event))
        )
    }
}