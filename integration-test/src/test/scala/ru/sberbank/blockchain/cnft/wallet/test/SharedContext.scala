package ru.sberbank.blockchain.cnft.wallet.test

import ru.sberbank.blockchain.cnft.common.types.{Bytes, Collection, Result}
import ru.sberbank.blockchain.cnft.model.{DataFeed, FeedType, FieldMeta, RegulatorCapabilities, SmartContract, SmartContractTemplate, TokenOwner}
import ru.sberbank.blockchain.cnft.wallet.CNFT
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec.{CNFTFactory, CNFTWalletSpec}

import scala.collection.mutable

object SharedContext {
    private val GateUrl = sys.env.getOrElse("CNFT_GATE_URL", "http://localhost:8981")
    val Chain: CNFTFactory[Result] = CNFT.connect(GateUrl)

    private val _context = new ThreadLocal[ScenarioContext]() {
        override def initialValue(): ScenarioContext = ScenarioContext()
    }

    def scenarioContext: ScenarioContext = _context.get()

    case class ScenarioContext(
        wallets: mutable.Map[String, CNFTWalletSpec[Result]] = mutable.HashMap.empty[String, CNFTWalletSpec[Result]],
        //        waiters: mutable.Map[String, Waiters] = mutable.HashMap.empty[String, Waiters],
        walletIds: mutable.Map[String, String] = mutable.HashMap.empty[String, String],
        walletCrypto: mutable.Map[String, WalletCrypto[Result]] = mutable.HashMap.empty[String, WalletCrypto[Result]],
        dataFeeds: mutable.Map[String, DataFeed] = mutable.HashMap.empty[String, DataFeed],
        walletSeeds: mutable.Map[String, Bytes] = mutable.HashMap.empty[String, Bytes],

        // SC Templates
        smartContractTemplates: mutable.Map[String, SmartContractTemplate] = mutable.HashMap.empty[String, SmartContractTemplate],
        smartContractTemplateAttributes: mutable.Map[String, Collection[FieldMeta]] = mutable.HashMap.empty[String, Collection[FieldMeta]],
        smartContractTemplateFeeds: mutable.Map[String, Collection[FeedType]] = mutable.HashMap.empty[String, Collection[FeedType]],
        smartContractTemplateStateModel: mutable.Map[String, Collection[FieldMeta]] = mutable.HashMap.empty[String, Collection[FieldMeta]],

        // SC
        smartContractRegulators: mutable.Map[String, Collection[RegulatorCapabilities]] = mutable.HashMap.empty[String, Collection[RegulatorCapabilities]],
        smartContractAddress: mutable.Map[String, String] = mutable.HashMap.empty[String, String],
        smartContractFeeds: mutable.Map[String, Collection[String]] = mutable.HashMap.empty[String, Collection[String]],
        smartContractExtraData: mutable.Map[String, Collection[FieldMeta]] = mutable.HashMap.empty[String, Collection[FieldMeta]],
        investmentTokenType: mutable.Map[String, String] = mutable.HashMap.empty[String, String],

        smartContract: mutable.Map[String, SmartContract] = mutable.HashMap.empty[String, SmartContract],
        // Token
        tokenOwnerAddress: mutable.Map[String, TokenOwner] = mutable.HashMap.empty[String, TokenOwner],
        tokenAddress: mutable.Map[(String, String), (Bytes, Option[String])] = mutable.HashMap.empty[(String, String), (Bytes, Option[String])],
        tokenTypeIdByName: mutable.Map[String, String] = mutable.HashMap.empty[String, String],
        tokenIdByName: mutable.Map[String, String] = mutable.HashMap.empty[String, String],
        operationIds: mutable.Map[String, Collection[String]] = mutable.HashMap.empty[String, Collection[String]],

        clientAddress: mutable.Map[String, Bytes] = mutable.HashMap.empty[String, Bytes],
        registeredDataFeeds: mutable.Map[String, DataFeed] = mutable.HashMap.empty[String, DataFeed],
        restrictionsIds: mutable.Map[String, Collection[String]] = mutable.HashMap.empty[String, Collection[String]]

    )

    // TODO: use orFail from ru.sberbank.blockchain.cnft.wallet.dsl
    def Fail(msg: String) = throw new java.lang.AssertionError(msg)

}
