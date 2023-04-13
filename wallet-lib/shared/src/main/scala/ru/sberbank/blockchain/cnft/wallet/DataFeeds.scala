package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{asBytes, collectionToArray}
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.model.{DataFeed, DataFeedValue, DescriptionField, FeedValueRequest, FieldMeta, SignedDataFeed}
import org.enterprisedlt.general.codecs.proto._

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
class DataFeeds[R[+_]](wallet: CNFTWalletInternal[R]) {

    import wallet._

    def registerDataFeed(description: Collection[DescriptionField], fields: Collection[FieldMeta]): R[TxResult[DataFeed]] =
        for {
            myId <- myWalletIdentity
            dataFeed = DataFeed(
                id = generateId,
                owner = wallet.id.id,
                description = description,
                fields = fields
            )
            signature <- crypto.identityOperations.createSignature(myId.signingKey, dataFeed.toBytes)
            signedDataFeed = SignedDataFeed(
                dataFeed,
                signature
            )
            result <- chainTx.registerDataFeed(signedDataFeed)
        } yield result.copy(value = dataFeed)

    def submitDataFeedValue(values: Collection[DataFeedValue]): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            signature <- crypto.identityOperations
                .createSignature(myId.signingKey, asBytes(ArrayEncoder[DataFeedValue].encode(collectionToArray(values))))
            result <- chainTx.submitDataFeedValue(
                FeedValueRequest(
                    feedValues = values,
                    signature = signature
                )
            )
        } yield result.copy(value = ())
}
