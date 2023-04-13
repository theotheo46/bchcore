package ru.sberbank.blockchain.cnft.wallet.test

import ru.sberbank.blockchain.cnft.common.types.{Collection, Result}
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.model.{DNA, DescriptionField, FieldMeta, FieldType, Gene, GeneID, TokenTypeMeta}
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.spec.{CNFTFactory, CNFTWalletSpec}
import ru.sberbank.blockchain.cnft.wallet.store.HDPathStore
import ru.sberbank.blockchain.cnft.wallet.{CNFT, CNFTCrypto}

object CNFTUtil extends LoggingSupport {

    lazy val CNFTGateUrl: String = sys.env.getOrElse("CNFT_GATE_URL", "http://localhost:8981")

    val CNFTRemoteWalletUrl: String = "http://localhost:8983"

    val Chain: CNFTFactory[Result] = CNFT.connect(CNFTGateUrl)


    def createWalletCrypto: WalletCrypto[Result] =
        CNFTCrypto
            .newContext(
                identityOpsFactory = CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                encryptionOpsFactory = CNFTCrypto.bouncyCastleEncryption(),
                accessOpsFactory = CNFTCrypto.bouncyCastleAccessOperations(),
                addressOpsFactory = CNFTCrypto.hdSignatureOperations(HDPathStore.inMemory()),
                hashFactory = CNFTCrypto.hash(),
                randomGeneratorFactory = CNFTCrypto.secureRandomGenerator()
            )
            .create()
            .orFail("Unable to create WalletCrypto")

    def registerMemberHook(crypto: WalletCrypto[Result]): Unit = {
        logger.info("Connecting to Admin wallet...")

        val adminWallet = Chain.connectWallet(CNFTRemoteWalletUrl)

        crypto.memberInformation().map { memberInfo =>
            Chain.getMember(memberInfo.id) match {
                case Left(_) =>
                    logger.info("Member does not registered yet")
                    val tx = adminWallet
                        .registerMember(memberInfo)
                        .orFail("Failed to register self in blockchain")
                    logger.info(s"Registered self [${memberInfo.id}] in TX: [${tx.blockNumber} : ${tx.txId}]")

                case Right(_) =>
                    logger.info("Member already registered")
            }
        }.orFail("Failed to register member")
    }

    def registerEmittableTokenType(
        wallet: CNFTWalletSpec[Result],
        typeId: String,
        tokenTypeName: String,
        dnaChange: Collection[Gene]
    ): TxResult[Unit] =
        wallet
            .registerTokenType(
                typeId,
                TokenTypeMeta(
                    description = Collection(DescriptionField("Value", FieldType.Text, tokenTypeName)),
                    fields = Collection(FieldMeta("amount", FieldType.Text))
                ),
                DNA.defaultInstance.copy(
                    emission = Collection(
                        Gene(
                            id = GeneID.EmissionControlledByIssuer,
                            parameters = Collection.empty
                        )
                    ),
                    change = dnaChange
                ),
                Collection.empty,
                Collection.empty
            )
            .orFail(s"Failed to register fungible token type $tokenTypeName")

}
