package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.common.types.Collection
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.model.{DNA, DescriptionField, FieldMeta, FieldType, Gene, GeneID, RegulatorCapabilities, RegulatorOperation, TokenTypeMeta}
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Fail, scenarioContext}

class StepsRegisterTokenTypes extends ScalaDsl with EN with LoggingSupport {

    When("""{string} registered token type {string}""") {
        (clientName: String, tokenTypeName: String) =>
            logger.info(s"Registering token type $tokenTypeName for $clientName ...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val typeId = wallet.createId.orFail("Failed to create type id")

            wallet
                .registerTokenType(
                    typeId,
                    TokenTypeMeta(
                        description = Collection(DescriptionField("Value", FieldType.Numeric, tokenTypeName)),
                        fields = Collection(FieldMeta("amount", FieldType.Text))
                    ),
                    DNA(
                        emission =
                            Collection(
                                Gene(
                                    id = GeneID.EmissionControlledByIssuer,
                                    parameters = Collection.empty
                                )
                            ),
                        transfer =
                            Collection(
                                Gene(
                                    id = GeneID.RequireOwnerSignatureForTransfer,
                                    parameters = Collection.empty
                                )
                            ),
                        burn =
                            Collection(
                                Gene(
                                    id = GeneID.RequireOwnerSignatureForBurn,
                                    parameters = Collection.empty
                                )
                            ),
                        change =
                            Collection(
                                Gene(
                                    id = GeneID.Fungible,
                                    parameters = Collection.empty
                                )
                            )
                    ),
                    Collection.empty,
                    Collection.empty
                )
                .orFail(s"Failed to register token type $tokenTypeName")
            scenarioContext.tokenTypeIdByName.put(clientName, typeId)
            scenarioContext.investmentTokenType.put(tokenTypeName, typeId)
            logger.info(s"Registered token type $tokenTypeName for $clientName ($typeId).")
    }

    When("""[ICO] {string} registered token type {string} with recipient signature gene""") {
        (clientName: String, tokenTypeName: String) =>
            logger.info(s"Registering token type $tokenTypeName for $clientName ...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val typeId = wallet.createId.orFail("Failed to create type id")
            wallet
                .registerTokenType(
                    typeId,
                    TokenTypeMeta(
                        description = Collection(DescriptionField("Value", FieldType.Numeric, tokenTypeName)),
                        fields = Collection(FieldMeta("amount", FieldType.Text))
                    ),
                    DNA(
                        emission =
                            Collection(
                                Gene(
                                    id = GeneID.EmissionControlledByIssuer,
                                    parameters = Collection.empty
                                )
                            ),
                        transfer =
                            Collection(
                                Gene(
                                    id = GeneID.RequireOwnerSignatureForTransfer,
                                    parameters = Collection.empty
                                ),
                                Gene(
                                    id = GeneID.RequireRecipientSignatureForTransfer,
                                    parameters = Collection.empty
                                )
                            ),
                        burn =
                            Collection(
                                Gene(
                                    id = GeneID.RequireOwnerSignatureForBurn,
                                    parameters = Collection.empty
                                )
                            ),
                        change =
                            Collection(
                                Gene(
                                    id = GeneID.Fungible,
                                    parameters = Collection.empty
                                )
                            )
                    ),
                    regulation = Collection.empty,
                    burnExtraData = Collection.empty)
                .orFail(s"Failed to register token type $tokenTypeName")
            scenarioContext.tokenTypeIdByName.put(clientName, typeId)
            scenarioContext.investmentTokenType.put(tokenTypeName, typeId)
            logger.info(s"Registered token type $tokenTypeName for $clientName ($typeId).")
    }

    When("""[ICO] {string} registered token type {string} with recipient signature gene with regulator {string}""") {
        (clientName: String, tokenTypeName: String, regulatorName: String) =>
            logger.info(s"Registering token type $tokenTypeName for $clientName ...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val typeId = wallet.createId.orFail("Failed to create type id")
            val regulator = scenarioContext.walletIds.getOrElse(regulatorName, Fail(s"Unknown client $clientName"))
            wallet
                .registerTokenType(
                    typeId,
                    TokenTypeMeta(
                        description = Collection(DescriptionField("Value", FieldType.Numeric, tokenTypeName)),
                        fields = Collection(FieldMeta("amount", FieldType.Text))
                    ),
                    DNA(
                        emission =
                            Collection(
                                Gene(
                                    id = GeneID.EmissionControlledByIssuer,
                                    parameters = Collection.empty
                                )
                            ),
                        transfer =
                            Collection(
                                Gene(
                                    id = GeneID.RequireOwnerSignatureForTransfer,
                                    parameters = Collection.empty
                                ),
                                Gene(
                                    id = GeneID.RequireRecipientSignatureForTransfer,
                                    parameters = Collection.empty
                                )
                            ),
                        burn =
                            Collection(
                                Gene(
                                    id = GeneID.RequireOwnerSignatureForBurn,
                                    parameters = Collection.empty
                                )
                            ),
                        change =
                            Collection(
                                Gene(
                                    id = GeneID.Fungible,
                                    parameters = Collection.empty
                                )
                            )
                    ),
                    regulation = Collection(
                        RegulatorCapabilities(
                            regulatorId = regulator,
                            capabilities = RegulatorOperation.All
                        )
                    ), Collection.empty)
                .orFail(s"Failed to register token type $tokenTypeName")
            scenarioContext.tokenTypeIdByName.put(clientName, typeId)
            scenarioContext.investmentTokenType.put(tokenTypeName, typeId)
            logger.info(s"Registered token type $tokenTypeName for $clientName ($typeId).")
    }

    When("""{string} registered token type {string} with regulator {string}""") {
        (clientName: String, tokenTypeName: String, regulatorName: String) =>
            logger.info(s"Registering token type $tokenTypeName for $clientName ...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val typeId = wallet.createId.orFail("Failed to create type id")

            val regulator = scenarioContext.walletIds.getOrElse(regulatorName, Fail(s"Unknown client $clientName"))
            wallet
                .registerTokenType(
                    tokenTypeId = typeId,
                    meta = TokenTypeMeta(
                        description = Collection(DescriptionField("Value", FieldType.Text, tokenTypeName)),
                        fields = Collection(FieldMeta("amount", FieldType.Text))
                    ),
                    dna = DNA(
                        emission =
                            Collection(
                                Gene(
                                    id = GeneID.EmissionControlledByIssuer,
                                    parameters = Collection.empty
                                )
                            ),
                        transfer =
                            Collection(
                                Gene(
                                    id = GeneID.RequireOwnerSignatureForTransfer,
                                    parameters = Collection.empty
                                )
                            ),
                        burn =
                            Collection(
                                Gene(
                                    id = GeneID.RequireOwnerSignatureForBurn,
                                    parameters = Collection.empty
                                )
                            ),
                        change =
                            Collection(
                                Gene(
                                    id = GeneID.Fungible,
                                    parameters = Collection.empty
                                )
                            )
                    ),
                    regulation = Collection(
                        RegulatorCapabilities(
                            regulatorId = regulator,
                            capabilities = RegulatorOperation.All
                        )
                    ),
                    burnExtraData = Collection.empty
                )
                .orFail(s"Failed to register token type $tokenTypeName")

            scenarioContext.tokenTypeIdByName.put(clientName, typeId)
            scenarioContext.investmentTokenType.put(tokenTypeName, typeId)
            logger.info(s"Registered token type $tokenTypeName for $clientName ($typeId).")
    }

    When("""{string} registered token type {string} with burn locked {string} and date {string} with datafeed {string}""") {
        (clientName: String, tokenTypeName: String, burnCondition: String, burnDueDate: String, feedName: String) =>
            logger.info(s"Registering token type $tokenTypeName for $clientName with burn allowed $burnCondition date...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val typeId = wallet.createId.orFail("Failed to create type id")
            val feed = scenarioContext.registeredDataFeeds.getOrElse(feedName, Fail(s"unknown datafeed $feedName"))
            val feedAddress = feed.id

            val burnGeneID = burnCondition match {
                case "before" => GeneID.BurnLockedBefore
                case "after" => GeneID.BurnLockedAfter
                case _ => Fail(s"Condition is neither before nor after")
            }
            wallet
                .registerTokenType(
                    typeId,
                    TokenTypeMeta(
                        description = Collection(DescriptionField("Value", FieldType.Numeric, tokenTypeName)),
                        fields = Collection(FieldMeta("amount", FieldType.Text))
                    ),
                    DNA(
                        emission =
                            Collection(
                                Gene(
                                    id = GeneID.EmissionControlledByIssuer,
                                    parameters = Collection.empty
                                )
                            ),
                        transfer =
                            Collection(
                                Gene(
                                    id = GeneID.RequireOwnerSignatureForTransfer,
                                    parameters = Collection.empty
                                )
                            ),
                        burn =
                            Collection(
                                Gene(
                                    id = burnGeneID,
                                    parameters = Collection(feedAddress, burnDueDate)
                                )
                            ),
                        change =
                            Collection(
                                Gene(
                                    id = GeneID.Fungible,
                                    parameters = Collection.empty
                                )
                            )
                    ),
                    Collection.empty,
                    Collection.empty
                )
                .orFail(s"Failed to register token type $tokenTypeName")
            scenarioContext.tokenTypeIdByName.put(clientName, typeId)
            scenarioContext.investmentTokenType.put(tokenTypeName, typeId)
            logger.info(s"Registered token type $tokenTypeName for $clientName ($typeId).")
    }

    When("""{string} registered token type {string} with burn locked between date {string} and date {string} with datafeed {string}""") {
        (clientName: String, tokenTypeName: String, burnFromDate: String, burnUntilDate: String, feedName: String) =>
            logger.info(s"Registering token type $tokenTypeName for $clientName with burn locked between two dates...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val typeId = wallet.createId.orFail("Failed to create type id")
            val feed = scenarioContext.registeredDataFeeds.getOrElse(feedName, Fail(s"unknown datafeed $feedName"))
            val feedAddress = feed.id

            val burnGeneID = GeneID.BurnLockedBetween
            wallet
                .registerTokenType(
                    typeId,
                    TokenTypeMeta(
                        description = Collection(DescriptionField("Value", FieldType.Numeric, tokenTypeName)),
                        fields = Collection(FieldMeta("amount", FieldType.Text))
                    ),
                    DNA(
                        emission =
                            Collection(
                                Gene(
                                    id = GeneID.EmissionControlledByIssuer,
                                    parameters = Collection.empty
                                )
                            ),
                        transfer =
                            Collection(
                                Gene(
                                    id = GeneID.RequireOwnerSignatureForTransfer,
                                    parameters = Collection.empty
                                )
                            ),
                        burn =
                            Collection(
                                Gene(
                                    id = burnGeneID,
                                    parameters = Collection(feedAddress, burnFromDate, burnUntilDate)
                                )
                            ),
                        change =
                            Collection(
                                Gene(
                                    id = GeneID.Fungible,
                                    parameters = Collection.empty
                                )
                            )
                    ),
                    Collection.empty,
                    Collection.empty
                )
                .orFail(s"Failed to register token type $tokenTypeName")
            scenarioContext.tokenTypeIdByName.put(clientName, typeId)
            scenarioContext.investmentTokenType.put(tokenTypeName, typeId)
            logger.info(s"Registered token type $tokenTypeName for $clientName ($typeId).")
    }
}
