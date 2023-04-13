package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.common.types.collectionFromIterable
import ru.sberbank.blockchain.cnft.commons.ROps
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.model.TokenId
import ru.sberbank.blockchain.cnft.wallet.WalletCommonOps
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec.IssuerEvents
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity

import scala.language.higherKinds

class IssuerEventsExtractor[R[+_]](
    val id: WalletIdentity,
    val crypto: WalletCrypto[R],
    val chain: ChainServiceSpec[R],
)(implicit
    val R: ROps[R]
) extends WalletCommonOps[R] {

    def extract(block: BlockEvents): R[IssuerEvents] = {
        block.tokensBurned.toSeq.filterR {
            _.event.request.request.tokens.toSeq.findR { tokenId =>
                for {
                    token <- TokenId.from(tokenId)
                    tokenType <- chain.getTokenType(token.typeId)
                } yield tokenType.issuerId == id.id
            }.map(_.isDefined)
        }.map(_.map(_.event))
            .map(collectionFromIterable)
            .map { r =>
                IssuerEvents(
                    tokensBurn = r.map(_.request.request)
                )
            }
    }
}