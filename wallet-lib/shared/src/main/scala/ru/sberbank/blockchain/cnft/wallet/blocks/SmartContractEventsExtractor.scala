package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.common.types.collectionFromIterable
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.{ROps, collectionFromArray}
import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.wallet.WalletCommonOps
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec.SmartContractEvents
import ru.sberbank.blockchain.cnft.wallet.walletmodel.{SCRejectedResult, WalletIdentity}

import scala.language.higherKinds

class SmartContractEventsExtractor[R[+_]](
    val id: WalletIdentity,
    val crypto: WalletCrypto[R],
    val chain: ChainServiceSpec[R],
)(implicit
    val R: ROps[R]
) extends WalletCommonOps[R] {

    def extract(block: BlockEvents): R[SmartContractEvents] =
        for {
            smartContractRejected <-
                block.smartContractsRejected.toSeq
                    .filterR(blockevent => chain.getSmartContract(blockevent.event.contractId).map(_.issuerId == id.id))
                    .flatMap {
                        _.mapR { blockevent =>
                            decryptText(blockevent.event.reason)
                                .map { reasonDecrypted =>
                                    SCRejectedResult(
                                        contractId = blockevent.event.contractId,
                                        reason = reasonDecrypted
                                    )
                                }
                        }.map(collectionFromIterable)
                    }

            smartContractsAdded = block.smartContractsAdded.map(_.event)

            smartContractStateUpdated = collectionFromArray(block.smartContractsStateUpdated.map(_.event))

            smartContractRegulationUpdated = collectionFromArray(block.smartContractsRegulationUpdated.map(_.event))

            /*_ = block.smartContractsAdded.foreach { smartContractEvent =>
                val smartContract = smartContractEvent.event
                R(TokenOwner.parseFrom(asByteArray(smartContract.address)).address).map { publicKey =>
                    configuration.store.findTokenKeyByPublic(Collection(publicKey)).map { keys =>
                        if (keys.nonEmpty) {
                            configuration.store.saveSmartContractAddress(smartContract.address)
                        }
                    }
                }

            }*/


            result = SmartContractEvents(
                rejectedSmartContracts = smartContractRejected,
                addedSmartContracts = collectionFromArray(smartContractsAdded),
                stateUpdatedSmartContracts = smartContractStateUpdated,
                regulationUpdatedSmartContracts = smartContractRegulationUpdated,
            )
        } yield result
}