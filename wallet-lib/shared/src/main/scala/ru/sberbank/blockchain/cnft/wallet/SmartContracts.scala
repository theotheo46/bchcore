package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.model.{RegulatorCapabilities, SCRegulationRequest, SignedSCRegulationRequest, SignedSmartContract, SmartContract, SmartContractRegulation}

import scala.language.higherKinds

/**
 * @author Maxim Fedin
 * */
class SmartContracts[R[+_]](wallet: CNFTWalletInternal[R]) {

    import wallet._

    // Smart Contract types
    //    def registerSmartContractTemplate(
    //        feeds: Collection[FeedType],
    //        description: Collection[DescriptionField],
    //        attributes: Collection[FieldMeta],
    //        stateModel: Collection[FieldMeta],
    //        classImplementation: String
    //    ): R[TxResult[SmartContractTemplate]] =
    //        for {
    //            address <- createAddress
    //            smartContractTemplate = SmartContractTemplate(
    //                address = address,
    //                feeds = feeds,
    //                description = description,
    //                attributes = attributes :+ FieldMeta("tokensType", "string", "token type"),
    //                stateModel = stateModel,
    //                classImplementation = classImplementation
    //            )
    //            result <- chain.registerSmartContractTemplate(smartContractTemplate)
    //        } yield result

    // Smart Contracts
    def createSmartContract(
        id: String, templateId: String, attributes: Collection[String],
        dataFeeds: Collection[String], regulators: Collection[RegulatorCapabilities],
    ): R[TxResult[SmartContract]] =
        for {
            myId <- myWalletIdentity
            endorsements <- listEndorsements
            smartContract =
                SmartContract(
                    id = id,
                    issuerId = myId.id,
                    templateId = templateId,
                    feeds = dataFeeds,
                    regulators = regulators,
                    attributes = attributes,
                    endorsements = endorsements
                )
            signature <- crypto.identityOperations.createSignature(myId.signingKey, smartContract.toBytes)
            signedSmartContract = SignedSmartContract(
                smartContract,
                signature
            )
            result <- chainTx.createSmartContract(signedSmartContract)
            _ <- R {
                logger.info(s"SGN smart contract created")
            }
        } yield result.copy(value = smartContract)

    def getSmartContractRegulation(id: String): R[SmartContractRegulation] =
        for {
            contract <- chain.getSmartContract(id)
            regulation <- chain.getSmartContractRegulation(id)
            regulationUpdated <- {
                val i = regulation.approves.indexWhere(_.reason.nonEmpty)
                if (contract.issuerId == wallet.id.id && i != -1) {
                    val encrypted = regulation.approves(i).reason
                    wallet.decryptText(encrypted).map { reason =>
                        regulation.approves(i) = regulation.approves(i).copy(reason = reason)
                        regulation
                    }
                } else R(regulation)
            }
        } yield regulationUpdated


    def approveSmartContract(id: String): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            scRegulationRequest = SCRegulationRequest(
                contractId = id,
                regulatorId = myId.id,
                reason = ""
            )
            signature <- crypto.identityOperations.createSignature(myId.signingKey, scRegulationRequest.toBytes)
            signedSCRegulationRequest = SignedSCRegulationRequest(
                request = scRegulationRequest,
                signature = signature
            )
            result <- chainTx.approveSmartContract(signedSCRegulationRequest)
        } yield result.copy(value = ())


    def rejectSmartContract(id: String, reason: String): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            contract <- chain.getSmartContract(id)
            reasonEncrypted <- encryptText(reason, Collection(contract.issuerId))
            scRegulationRequest = SCRegulationRequest(
                contractId = id,
                regulatorId = myId.id,
                reason = reasonEncrypted
            )
            signature <- crypto.identityOperations.createSignature(myId.signingKey, scRegulationRequest.toBytes)
            signedSCRegulationRequest = SignedSCRegulationRequest(
                request = scRegulationRequest,
                signature = signature
            )
            result <- chainTx.rejectSmartContract(signedSCRegulationRequest)
        } yield result.copy(value = ())

}
