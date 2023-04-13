package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.datatable.DataTable
import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.common.types.{Collection, _}
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.commons.ROps.IterableR_Ops
import ru.sberbank.blockchain.cnft.model.{DNA, FeedType, FieldMeta, FieldType, Gene, GeneID, RegulatorCapabilities, RegulatorOperation, TokenContent, TokenTypeMeta}
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Chain, Fail, scenarioContext}
import ru.sberbank.blockchain.cnft.wallet.test.Util.{mkRegulationString, toB64}
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIssueTokenRequest

import java.util.UUID
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

class StepsSmartContracts extends ScalaDsl with EN with LoggingSupport {

    When("""smart contract template for {string} attributes are:""") {
        (smartContractTemplateName: String, data: DataTable) =>
            val fields = data.asMaps().asScala.map { fieldMeta =>
                val id = fieldMeta.get("id")
                val typeId = fieldMeta.get("typeId")
                val description = fieldMeta.get("description")
                logger.info(s"FieldMeta: $id with typeId $typeId and description $description")
                FieldMeta(id, typeId)
            }.toArray
            scenarioContext.smartContractTemplateAttributes.put(smartContractTemplateName, fields)
            logger.info(s"Stored smart contract template $smartContractTemplateName attributes")
    }

    And("""smart contract template for {string} data feeds are:""") {
        (smartContractTemplateName: String, data: DataTable) =>
            logger.info(s"Storing data feed ids for smartContractTemplate $smartContractTemplateName")
            val attributes = data.asMaps().asScala.map { dataFeed =>
                val id = dataFeed.get("id")
                logger.info(scenarioContext.dataFeeds.mkString(", "))
                val feeds = scenarioContext.dataFeeds.getOrElse(id, Fail(s"Unknown dataFeed $id")).fields
                FeedType(feeds)
            }.toArray
            if (attributes.nonEmpty) {
                scenarioContext.smartContractTemplateFeeds.put(smartContractTemplateName, attributes)
                logger.info(s"Stored smart contract template $smartContractTemplateName attributes")
            }
    }

    And("""smart contract template for {string} state model is:""") {
        (smartContractTemplateName: String, data: DataTable) =>
            logger.info(s"Storing state model for smartContractTemplate $smartContractTemplateName")
            val attributes = data.asMaps().asScala.map { fieldMeta =>
                val id = fieldMeta.get("id")
                val typeId = fieldMeta.get("typeId")
                val description = fieldMeta.get("description")
                logger.info(s"FieldMeta: $id with typeId $typeId and description $description")
                FieldMeta(id, typeId)
            }.toArray
            scenarioContext.smartContractTemplateStateModel.put(smartContractTemplateName, attributes)
            logger.info(s"Stored smart contract template $smartContractTemplateName state model")
    }

    And("""{string} registered smart contract template for {string}""") {
        (clientName: String, smartContractTemplateName: String) =>
            logger.info(s"Registering smart contract template $smartContractTemplateName for $clientName...")
            //            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            //            val feeds = scenarioContext.smartContractTemplateFeeds
            //                .getOrElse(smartContractTemplateName, Collection())
            //            val attributes = scenarioContext.smartContractTemplateAttributes
            //                .getOrElse(smartContractTemplateName, Collection())
            //            val stateModel = scenarioContext.smartContractTemplateStateModel
            //                .getOrElse(smartContractTemplateName, Collection())
            //            val TxResult(_, _, template) =
            //                wallet
            //                    .registerSmartContractTemplate(
            //                        feeds = feeds,
            //                        description = Collection(
            //                            DescriptionField(
            //                                name = "Smart contract template name",
            //                                typeId = FieldType.Text,
            //                                value = smartContractTemplateName,
            //                            )
            //                        ),
            //                        attributes = attributes,
            //                        stateModel = stateModel,
            //                        classImplementation = smartContractTemplateName
            //                    )
            //                    .orFail(s"Failed to register smart contract template $smartContractTemplateName")
            //            scenarioContext.smartContractTemplates.put(smartContractTemplateName, template)
            logger.info(s"Registered smart contract template $smartContractTemplateName for $clientName .")
    }

    When("""{string} registered address for smart contract {string}""") {
        (clientName: String, smartContractName: String) =>
            logger.info(s"Registering smart contract address for  $smartContractName for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val smartContractId =
                wallet
                    .createId
                    .orFail(s"Failed to create smart contract address for $smartContractName")
            scenarioContext.smartContractAddress.put(smartContractName, smartContractId)
            logger.info(s"Created smart contract id $smartContractId for $smartContractName for $clientName.")
    }

    And("""smart contract {string} data feeds are:""") {
        (smartContractName: String, data: DataTable) =>
            logger.info(s"Storing data feed ids for smartContract $smartContractName")
            val feedAddresses = data.asMaps().asScala.map { dataFeed =>
                val id = dataFeed.get("id")
                //                scenarioContext.dataFeeds
                val feed = scenarioContext.dataFeeds.getOrElse(id, Fail(s"Unknown dataFeed $id"))
                feed.id
            }.toArray
            if (feedAddresses.nonEmpty) {
                scenarioContext.smartContractFeeds.put(smartContractName, feedAddresses)
                logger.info(s"Stored smart contract template $smartContractName attributes")
            }
    }

    And("""smart contract {string} regulators are:""") {
        (smartContractName: String, data: DataTable) =>
            logger.info(s"Storing regulators for $smartContractName")
            val regulatorCapabilities = data.asMaps().asScala.map { row =>
                val name = row.get("name")
                val walletIdentity = scenarioContext.walletIds.getOrElse(name, Fail(s"Fail to get walletIdentity for $name"))
                var capabilities = row.get("capabilities").split(" ")
                if (capabilities.head.toLowerCase == "all")
                    capabilities = RegulatorOperation.All
                RegulatorCapabilities(
                    regulatorId = walletIdentity,
                    capabilities = capabilities
                )
            }.toArray
            if (regulatorCapabilities.nonEmpty) {
                scenarioContext.smartContractRegulators.put(smartContractName, regulatorCapabilities)
                logger.info(s"Stored smart contract $smartContractName regulatorCapabilities")
            }
    }

    And("""smart contract {string} burn extra data:""") {
        (smartContractTemplateName: String, data: DataTable) =>
            logger.info(s"Storing burn extra data for smartContractTemplate $smartContractTemplateName")
            val burnExtraData = data.asMaps().asScala.map { fieldMeta =>
                val id = fieldMeta.get("id")
                val typeId = fieldMeta.get("typeId")
                val description = fieldMeta.get("description")
                logger.info(s"FieldMeta: $id with typeId $typeId and description $description")
                FieldMeta(id, typeId)
            }.toArray
            if (burnExtraData.nonEmpty) {
                scenarioContext.smartContractExtraData.put(smartContractTemplateName, burnExtraData)
                logger.info(s"Stored smart contract template $smartContractTemplateName burn extra data")
            }
    }

    And("""{string} registered {string} smart contract with smart contract template {string} and attributes:""") {
        (clientName: String, smartContractName: String, smartContractTemplateName: String, data: DataTable) =>
            logger.info(s"Registering smart contract template $smartContractTemplateName for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val smartContractId = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"Unknown smart contract name $smartContractName"))

            val feeds = Chain.listDataFeeds.orFail("Can't list feeds").find(df => df.description.exists(a => a.value == "Gate timestamping feed"))
                .map(_.id).toArray
            val burnExtraData = scenarioContext.smartContractExtraData.getOrElse(smartContractName, Collection.empty)
            val regulators = scenarioContext.smartContractRegulators.getOrElse(smartContractName, Collection.empty)
            val attrs = data.asMaps().asScala.map(_.get("value")).toArray
            val attributes =
                smartContractTemplateName match {
                    case "ICO" =>
                        val issuerAddress = wallet.createSingleOwnerAddress.orFail("Can not create issuer address for SC")
                        val identityBase64 = toB64(issuerAddress.toByteArray)
                        val investmentTokenType = scenarioContext.investmentTokenType.getOrElse("SBC", Fail("Investment token type not found!"))
                        logger.info(s"Issuer address for smart contract $smartContractName: $identityBase64")
                        val date = attrs(5)
                        val subscriptionEndDate = attrs(6)
                        val ReleaseDate = attrs(7)
                        attrs
                            .updated(0, investmentTokenType)
                            .updated(1, identityBase64)
                            .updated(5, date)
                            .updated(6, subscriptionEndDate)
                            .updated(7, ReleaseDate)
                    case _ =>
                        Fail(s"Unknown smart contract template name $smartContractTemplateName")
                }

            val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
                if (events.smartContracts.addedSmartContracts.nonEmpty)
                    Some(events.smartContracts.addedSmartContracts)
                else None
            }

            wallet.registerTokenType(
                tokenTypeId = smartContractId,
                meta = TokenTypeMeta(
                    description = Collection.empty,
                    fields =
                        Collection(
                            FieldMeta("amount", FieldType.Numeric)
                        )
                ),
                dna =
                    DNA(
                        emission = Collection(
                            Gene(
                                id = GeneID.EmissionControlledBySmartContract,
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
                regulation = regulators,
                burnExtraData = burnExtraData
            ).orFail("Unable to register token type for SC")

            val smartContract =
                wallet
                    .createSmartContract(
                        id = smartContractId,
                        templateId = smartContractTemplateName,
                        attributes = attributes,
                        dataFeeds = feeds,
                        regulators = regulators
                    )
                    .orFail(s"Failed to register smart contract template $smartContractTemplateName").value

            val added = await.get
            logger.info(s"Smart contract has been added: ${added.map(_.toProtoString).mkString}")
            scenarioContext.smartContract.put(smartContractName, smartContract)
            logger.info(s"Smart contract address: $smartContractId")
            logger.info(s"Registered smart contract template $smartContractTemplateName for $clientName ($smartContract).")
            (for {
                walletIdentity <- wallet.getIdentity
                mySmartContracts <- Chain
                    .listSmartContracts
                    .map(_
                        .filter(_.issuerId == walletIdentity)
                    )
            } yield {
                logger.info(s"Smart contracts are: ${mySmartContracts.mkString(" ")}")
            })
                .orFail(s"Unable to list smart contracts")
    }

    When("""{string} registering wrong IndexTrade token type for smartcontract {string}""") {
        (clientName: String, smartContractName: String) =>

            logger.info(s"Registering token type for smartcontract $smartContractName for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val smartContractId = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"Unknown smart contract name $smartContractName"))
            val burnExtraData = scenarioContext.smartContractExtraData.getOrElse(smartContractName, Collection.empty)
            val regulators = scenarioContext.smartContractRegulators.getOrElse(smartContractName, Collection.empty)

            wallet.registerTokenType(
                tokenTypeId = smartContractId,
                meta = TokenTypeMeta(
                    description = Collection.empty,
                    fields =
                        Collection(
                            FieldMeta("amount", FieldType.Numeric),
                            FieldMeta("prices", FieldType.Numeric),
                            FieldMeta("symbol", FieldType.Text),
                            FieldMeta("tenorValue", FieldType.Text),
                            FieldMeta("valueDate", FieldType.Text),
                            FieldMeta("bandPrice", FieldType.Numeric),
                            FieldMeta("tolerancePrice", FieldType.Numeric),
                            FieldMeta("maxBandVolume", FieldType.Numeric)
                        )
                ),
                dna =
                    DNA(
                        emission = Collection(
                            Gene(
                                id = GeneID.EmissionControlledBySmartContract,
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
                regulation = regulators,
                burnExtraData = burnExtraData
            ).orFail("Unable to register token type for SC")
    }

    When("""{string} registering IndexTrade token type for smartcontract {string} with regulator {string}""") {
        (clientName: String, smartContractName: String, regulatorName: String) =>

            logger.info(s"Registering token type for smartcontract $smartContractName for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val smartContractId = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"Unknown smart contract name $smartContractName"))
            val burnExtraData = scenarioContext.smartContractExtraData.getOrElse(smartContractName, Collection.empty)
            val regulator = scenarioContext.walletIds.getOrElse(regulatorName, Fail(s"Unknown regulator $regulatorName"))

            wallet.registerTokenType(
                tokenTypeId = smartContractId,
                meta = TokenTypeMeta(
                    description = Collection.empty,
                    fields =
                        Collection(
                            FieldMeta("amount", FieldType.Numeric),
                            FieldMeta("price", FieldType.Numeric),
                            FieldMeta("symbol", FieldType.Text),
                            FieldMeta("tenorValue", FieldType.Text),
                            FieldMeta("valueDate", FieldType.Text),
                            FieldMeta("bandPrice", FieldType.Numeric),
                            FieldMeta("tolerancePrice", FieldType.Numeric),
                            FieldMeta("maxBandVolume", FieldType.Numeric)
                        )
                ),
                dna =
                    DNA(
                        emission = Collection(
                            Gene(
                                id = GeneID.EmissionControlledBySmartContract,
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
                ),
                burnExtraData = burnExtraData
            ).orFail("Unable to register token type for SC")

            scenarioContext.investmentTokenType.put("IndexTrade", smartContractId)
    }

    When("""{string} registering IndexTrade token type for smartcontract {string}""") {
        (clientName: String, smartContractName: String) =>

            logger.info(s"Registering token type for smartcontract $smartContractName for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val smartContractId = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"Unknown smart contract name $smartContractName"))
            val burnExtraData = scenarioContext.smartContractExtraData.getOrElse(smartContractName, Collection.empty)
            val regulators = scenarioContext.smartContractRegulators.getOrElse(smartContractName, Collection.empty)

            wallet.registerTokenType(
                tokenTypeId = smartContractId,
                meta = TokenTypeMeta(
                    description = Collection.empty,
                    fields =
                        Collection(
                            FieldMeta("amount", FieldType.Numeric),
                            FieldMeta("price", FieldType.Numeric),
                            FieldMeta("symbol", FieldType.Text),
                            FieldMeta("tenorValue", FieldType.Text),
                            FieldMeta("valueDate", FieldType.Text),
                            FieldMeta("bandPrice", FieldType.Numeric),
                            FieldMeta("tolerancePrice", FieldType.Numeric),
                            FieldMeta("maxBandVolume", FieldType.Numeric)
                        )
                ),
                dna =
                    DNA(
                        emission = Collection(
                            Gene(
                                id = GeneID.EmissionControlledBySmartContract,
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
                regulation = regulators,
                burnExtraData = burnExtraData
            ).orFail("Unable to register token type for SC")

            scenarioContext.investmentTokenType.put("IndexTrade", smartContractId)
    }

    When("""{string} registering IndexTrade token type for redeem smartcontract {string}""") {
        (clientName: String, smartContractName: String) =>

            logger.info(s"Registering token type for smartcontract $smartContractName for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val smartContractId = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"Unknown smart contract name $smartContractName"))
            val burnExtraData = scenarioContext.smartContractExtraData.getOrElse(smartContractName, Collection.empty)
            val regulators = scenarioContext.smartContractRegulators.getOrElse(smartContractName, Collection.empty)

            wallet.registerTokenType(
                tokenTypeId = s"${smartContractId}_indextrade",
                meta = TokenTypeMeta(
                    description = Collection.empty,
                    fields =
                        Collection(
                            FieldMeta("amount", FieldType.Numeric),
                            FieldMeta("price", FieldType.Numeric),
                            FieldMeta("symbol", FieldType.Text),
                            FieldMeta("tenorValue", FieldType.Text),
                            FieldMeta("valueDate", FieldType.Text),
                            FieldMeta("bandPrice", FieldType.Numeric),
                            FieldMeta("tolerancePrice", FieldType.Numeric),
                            FieldMeta("maxBandVolume", FieldType.Numeric)
                        )
                ),
                dna =
                    DNA(
                        emission = Collection(
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
                                    id = GeneID.BurnControlledBySmartContract,
                                    parameters = Collection(smartContractId)
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
                regulation = regulators,
                burnExtraData = burnExtraData
            ).orFail("Unable to register token type for SC")

            scenarioContext.investmentTokenType.put("IndexTrade", s"${smartContractId}_indextrade")
    }

    When("""{string} registering IndexTrade token type for redeem smartcontract {string} with regulator {string}""") {
        (clientName: String, smartContractName: String, regulatorName: String) =>

            logger.info(s"Registering token type for smartcontract $smartContractName for $clientName with regulator $regulatorName ...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val smartContractId = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"Unknown smart contract name $smartContractName"))
            val burnExtraData = scenarioContext.smartContractExtraData.getOrElse(smartContractName, Collection.empty)
            //  val regulators = scenarioContext.smartContractRegulators.getOrElse(smartContractName, Collection.empty)
            val regulator = scenarioContext.walletIds.getOrElse(regulatorName, Fail(s"Unknown regulator $regulatorName"))

            wallet.registerTokenType(
                tokenTypeId = s"${smartContractId}_indextrade",
                meta = TokenTypeMeta(
                    description = Collection.empty,
                    fields =
                        Collection(
                            FieldMeta("amount", FieldType.Numeric),
                            FieldMeta("price", FieldType.Numeric),
                            FieldMeta("symbol", FieldType.Text),
                            FieldMeta("tenorValue", FieldType.Text),
                            FieldMeta("valueDate", FieldType.Text),
                            FieldMeta("bandPrice", FieldType.Numeric),
                            FieldMeta("tolerancePrice", FieldType.Numeric),
                            FieldMeta("maxBandVolume", FieldType.Numeric)
                        )
                ),
                dna =
                    DNA(
                        emission = Collection(
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
                                    id = GeneID.BurnControlledBySmartContract,
                                    parameters = Collection(smartContractId)
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
                burnExtraData = burnExtraData
            ).orFail("Unable to register token type for SC")

            scenarioContext.investmentTokenType.put("IndexTrade", s"${smartContractId}_indextrade")
    }

    //

    When("""{string} registering IndexTrade token type for {string} smartcontract""") {
        (clientName: String, smartContractName: String) =>

            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val burnExtraData = Collection.empty[FieldMeta] //scenarioContext.smartContractExtraData.getOrElse(smartContractName, Collection.empty)
            val regulators = Collection.empty[RegulatorCapabilities] //scenarioContext.smartContractRegulators.getOrElse(smartContractName, Collection.empty)

            val typeId = wallet.createId.orFail("Failed to create type id")
            scenarioContext.investmentTokenType.put("IndexTrade", typeId)
            wallet.registerTokenType(
                tokenTypeId = typeId,
                meta = TokenTypeMeta(
                    description = Collection.empty,
                    fields =
                        Collection(
                            FieldMeta("amount", FieldType.Numeric),
                            FieldMeta("price", FieldType.Numeric),
                            FieldMeta("symbol", FieldType.Text),
                            FieldMeta("tenorValue", FieldType.Text),
                            FieldMeta("valueDate", FieldType.Text),
                            FieldMeta("bandPrice", FieldType.Numeric),
                            FieldMeta("tolerancePrice", FieldType.Numeric),
                            FieldMeta("maxBandVolume", FieldType.Numeric)
                        )
                ),
                dna =
                    DNA(
                        emission = Collection(
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
                regulation = regulators,
                burnExtraData = burnExtraData
            ).orFail("Unable to register token type for SC")
    }

//    When("""{string} issued IndexTrade token {string} for {string} with regulation and content:""") {
//        (issuerName: String, tokenName: String, clientName: String, regulatorName: String, dataTable: DataTable) =>
//
//
//            val tokenTypeName = "IndexTrade"
//            logger.info(s"Issuing token of type $tokenTypeName for $clientName ...")
//            val issuerWallet = scenarioContext.wallets.getOrElse(issuerName, Fail(s"Unknown client $issuerName"))
//            val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
//
//            val tokenContentData = collectionFromIterable(dataTable.asMaps().asScala.map(_.get("value")))
//            logger.info(s"token content data:")
//
//            val maybeTokenType = scenarioContext.investmentTokenType.get(tokenTypeName)
//            val addr =
//                clientWallet
//                    .createAddress
//                    .orFail(s"Failed to create  address for token $tokenName")
//
//            val maybeTokenId = maybeTokenType.map { tt =>
//                clientWallet.createTokenId(tt).orFail("failed to create TokenId")
//            }
//
//            logger.info(s"token id ${maybeTokenId.getOrElse("")} token type ${maybeTokenType.getOrElse("")}")
//            scenarioContext.tokenAddress.put((clientName, tokenName), (addr, maybeTokenId))
//
//            val owner = clientWallet.createSingleOwnerAddress
//                .orFail(s"Failed to reserve id for $clientName")
//            val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
//                if (events.owner.tokensReceived.nonEmpty)
//                    Some(events.owner.tokensReceived)
//                else
//                    None
//            }
//            maybeTokenId.map { tokenId =>
//                issuerWallet
//                    .issue(
//                        Collection(
//                            WalletIssueTokenRequest(
//                                tokenId,
//                                owner,
//                                TokenContent(
//                                    tokenContentData
//                                ),
//                                clientWallet.getIdentity.orFail("no identity")
//                            )
//                        )
//                    )
//                    .orFail(s"Failed to issue token for $clientName")
//                scenarioContext.tokenIdByName.put(tokenName, tokenId)
//                val added = await.get
//                added.foreach { tokenId =>
//                    logger.info(s"Issued token of type $tokenTypeName for $clientName (${tokenId}).")
//                }
//            }
//    }

    When("""{string} issued IndexTrade token {string} for {string} with content:""") {
        (issuerName: String, tokenName: String, clientName: String, dataTable: DataTable) =>


            val tokenTypeName = "IndexTrade"
            logger.info(s"Issuing token of type $tokenTypeName for $clientName ...")
            val issuerWallet = scenarioContext.wallets.getOrElse(issuerName, Fail(s"Unknown client $issuerName"))
            val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val tokenContentData = collectionFromIterable(dataTable.asMaps().asScala.map(_.get("value")))
            logger.info(s"token content data:")

            val maybeTokenType = scenarioContext.investmentTokenType.get(tokenTypeName)
            val addr =
                clientWallet
                    .createAddress
                    .orFail(s"Failed to create  address for token $tokenName")

            val maybeTokenId = maybeTokenType.map { tt =>
                clientWallet.createTokenId(tt).orFail("failed to create TokenId")
            }

            logger.info(s"token id ${maybeTokenId.getOrElse("")} token type ${maybeTokenType.getOrElse("")}")
            scenarioContext.tokenAddress.put((clientName, tokenName), (addr, maybeTokenId))

            val owner = clientWallet.createSingleOwnerAddress
                .orFail(s"Failed to reserve id for $clientName")
            val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
                if (events.owner.tokensReceived.nonEmpty)
                    Some(events.owner.tokensReceived)
                else
                    None
            }
            maybeTokenId.map { tokenId =>
                issuerWallet
                    .issue(
                        Collection(
                            WalletIssueTokenRequest(
                                tokenId,
                                owner,
                                TokenContent(
                                    tokenContentData
                                ),
                                clientWallet.getIdentity.orFail("no identity")
                            )
                        )
                    )
                    .orFail(s"Failed to issue token for $clientName")
                scenarioContext.tokenIdByName.put(tokenName, tokenId)
                val added = await.get
                added.foreach { tokenId =>
                    logger.info(s"Issued token of type $tokenTypeName for $clientName (${tokenId}).")
                }
            }
    }

    When("""{string} issued IndexTrade token {string} for {string} with regulation and content:""") {
        (issuerName: String, tokenName: String, clientName: String, dataTable: DataTable) =>


            val tokenTypeName = "IndexTrade"
            logger.info(s"Issuing token of type $tokenTypeName for $clientName ...")
            val issuerWallet = scenarioContext.wallets.getOrElse(issuerName, Fail(s"Unknown client $issuerName"))
            val clientWallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))

            val tokenContentData = collectionFromIterable(dataTable.asMaps().asScala.map(_.get("value")))
            logger.info(s"token content data:")

            val maybeTokenType = scenarioContext.investmentTokenType.get(tokenTypeName)
            val addr =
                clientWallet
                    .createAddress
                    .orFail(s"Failed to create  address for token $tokenName")

            val maybeTokenId = maybeTokenType.map { tt =>
                clientWallet.createTokenId(tt).orFail("failed to create TokenId")
            }

            logger.info(s"token id ${maybeTokenId.getOrElse("")} token type ${maybeTokenType.getOrElse("")}")
            scenarioContext.tokenAddress.put((clientName, tokenName), (addr, maybeTokenId))

            val owner = clientWallet.createSingleOwnerAddress
                .orFail(s"Failed to reserve id for $clientName")

            maybeTokenId.map { tokenId =>
                issuerWallet
                    .issue(
                        Collection(
                            WalletIssueTokenRequest(
                                tokenId,
                                owner,
                                TokenContent(
                                    tokenContentData
                                ),
                                clientWallet.getIdentity.orFail("no identity")
                            )
                        )
                    )
                    .orFail(s"Failed to issue token for $clientName")
                scenarioContext.tokenIdByName.put(tokenName, tokenId)

            }
    }

    When("""{string} looking on metadata:""") {
        (clientName: String, data: DataTable) =>
            logger.debug(s"client name $clientName")
            println(data)
            val dataTbl = data.asLists().asScala.map { v => v.get(0) -> v.get(1) }.toMap
            println(dataTbl)


    }

    And("""{string} registered {string} smart contract with smart contract template {string} and feed {string} and attributes:""") {
        (clientName: String, smartContractName: String, smartContractTemplateName: String, feedName: String, data: DataTable) =>
            logger.info(s"Registering smart contract template $smartContractTemplateName for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val smartContractId = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"Unknown smart contract name $smartContractName"))
            val feed = scenarioContext.registeredDataFeeds.getOrElse(feedName, Fail(s"unknown datafeed $feedName"))
            val feeds = Collection(feed.id)
            val burnExtraData = scenarioContext.smartContractExtraData.getOrElse(smartContractName, Collection.empty)
            val regulators = scenarioContext.smartContractRegulators.getOrElse(smartContractName, Collection.empty)

            val attrs = data.asLists().asScala.map { v => v.get(0) -> v.get(1) }.toMap
            val attributes =
                smartContractTemplateName match {
                    case "ICO" =>
                        val issuerAddress = wallet.createSingleOwnerAddress.orFail("Can not create issuer address for SC")
                        val identityBase64 = toB64(issuerAddress.toByteArray)
                        val investmentTokenType = scenarioContext.investmentTokenType.getOrElse(attrs("investmentTokenType"), Fail("Investment token type not found!"))
                        logger.info(s"Issuer address for smart contract $smartContractName: $identityBase64")

                        wallet.registerTokenType(
                            tokenTypeId = smartContractId,
                            meta = TokenTypeMeta(
                                description = Collection.empty,
                                fields =
                                    Collection(
                                        FieldMeta("amount", FieldType.Numeric)
                                    )
                            ),
                            dna =
                                DNA(
                                    emission = Collection(
                                        Gene(
                                            id = GeneID.EmissionControlledBySmartContract,
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
                            regulation = regulators,
                            burnExtraData = burnExtraData
                        ).orFail("Unable to register token type for SC")

                        attrs
                            .updated("investmentTokenType", investmentTokenType)
                            .updated("issuerAddress", identityBase64)

                    case "IndexTrade" =>
                        val ownerAddress = wallet.createSingleOwnerAddress.orFail("Can not create issuer address for SC")
                        val identityBase64 =
                            if (attrs("issuerAddress") == "undefined")
                                toB64(ownerAddress.toByteArray)
                            else attrs("issuerAddress")
                        attrs
                            .updated("issuerAddress", identityBase64)
                            .updated("investmentTokenType", scenarioContext.investmentTokenType(attrs("investmentTokenType")))

                    case "IndexTradeRedeem" =>
                        val ownerAddress = scenarioContext.tokenOwnerAddress(attrs("issuerAddress"))
                        val identityBase64 = toB64(ownerAddress.toByteArray)
                        logger.info(s"investment token type: ${scenarioContext.investmentTokenType(attrs("redeemedTokenType"))}")
                        logger.info(s"tech token type: ${scenarioContext.investmentTokenType(attrs("techTokenType"))}")
                        attrs
                            .updated("issuerAddress", identityBase64)
                            .updated("redeemedTokenType", scenarioContext.investmentTokenType(attrs("redeemedTokenType")))
                            .updated("techTokenType", scenarioContext.investmentTokenType(attrs("techTokenType")))
                    case _ =>
                        Fail(s"Unknown smart contract template name $smartContractTemplateName")
                }


            val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
                if (events.smartContracts.addedSmartContracts.nonEmpty)
                    Some(events.smartContracts.addedSmartContracts.nonEmpty)
                else
                    None
            }

            val template = Chain.getSmartContractTemplate(smartContractTemplateName).orFail(s"can not get smart contract template")

            val smartContract =
                wallet
                    .createSmartContract(
                        id = smartContractId,
                        templateId = smartContractTemplateName,
                        attributes = template.attributes
                            .map { f => attributes.getOrElse(f.id, Fail(s"attribute ${f.id} does not exists")) },
                        dataFeeds = feeds,
                        regulators = regulators
                    )
                    .orFail(s"Failed to register smart contract template $smartContractTemplateName").value

            val added = await.get
            logger.info(s"Smart contract has been added: $added")
            scenarioContext.smartContract.put(smartContractName, smartContract)
            logger.info(s"Smart contract add result: $added address: ${smartContractId}")
            logger.info(s"Registered smart contract template $smartContractTemplateName for $clientName ($smartContract).")
        //     val sc = wallet.listSmartContracts.orFail(s"Unable to list smart contracts")
        //     log.info(s"Smart contracts are: ${sc.mkString(" ")}")
    }

    When("""list smart contract {string} accepted deals""") {
        (smartContractName: String) =>
            logger.info(s"list accepted deals for smartcontract: $smartContractName ...")
            (for {
                smartContractAddress <- scenarioContext.smartContractAddress
                    .get(smartContractName)
                    .toRight(s"Unknown smart contract name $smartContractName")

                acceptedDeals <- Chain.listSmartContractAcceptedDeals(smartContractAddress)

                _ = logger.info(s"accepted deals:")
                _ = acceptedDeals.foreach { deal =>
                    logger.info(s"request:\n ${deal.deal}")
                    deal.tokens.foreach { token =>
                        logger.info(s"$token")
                    }
                }
            } yield ())
                .orFail("Failed to list smart contract accepted deals")
    }

    When("""{string} checks {string} smart contract state""") {
        (clientName: String, tokenName: String) =>
            logger.info(s"Checks smart contract state for $tokenName...")
            (for {
                smartContractAddress <- scenarioContext.smartContractAddress
                    .get(tokenName)
                    .toRight(s"Unknown smart contract name $tokenName")
                state <- Chain.getSmartContractState(smartContractAddress)
                _ = logger.info(s"Smart contract state for $tokenName: ${state.state.mkString(" ")}")

                deals <- Chain.listSmartContractAcceptedDeals(smartContractAddress)
                dealsInfo <- deals
                    .toSeq
                    .mapR { deal =>
                        for {
                            wallet <- scenarioContext.wallets
                                .get(clientName)
                                .toRight(s"Unknown client $clientName")

                            data <- wallet.extractDealExtraData(deal.deal.deal)

                            dealTokens =
                                deal.tokens
                                    .toSeq
                                    .map { token =>
                                        //                                        for {
                                        ////                                            walletToken <- Chain.getToken(token.id)
                                        //                                            fromIndex = data.legs(token.leg).from
                                        //                                        } yield (fromIndex, walletToken)
                                        val fromIndex = data.legs(token.leg).from
                                        (fromIndex, token.id)
                                    }
                                    .groupBy(_._1) // fromIndex
                                    .map { case (fromIndex, tokens) =>
                                        //                                            val tokensString = Util.mkTokensString(tokens.map(_._2), 2)
                                        val member = data.members(fromIndex)
                                        s"FROM: ${member.id} got tokens: [${tokens.mkString(", ")} ]"
                                    }
                                    .mkString("\n\t", "\n\t", "")

                        } yield s"In deal ${deal.deal.deal.dealId}, received:\n$dealTokens"
                    }
                    .map(_.mkString("\n"))
                _ = logger.info(s"Accepted deals are:\n$dealsInfo")
            } yield ())
                .orFail("Failed to check smart contract state")
    }

    When("""{string} checks {string} smart contract regulation""") {
        (clientName: String, tokenName: String) =>
            logger.info(s"Checks smart contract regulation for $tokenName...")

            val smartContractAddress =
                scenarioContext.smartContractAddress
                    .getOrElse(tokenName, Fail(s"Unknown smart contract address $tokenName"))

            val regulation = Chain
                .getSmartContractRegulation(smartContractAddress)
                .orFail(s"Failed to issue token for $clientName")
            logger.info(s"[$clientName] Smart contract regulation for $tokenName: ${mkRegulationString(regulation)}")
    }
    And("""{string} can not register {string} smart contract with smart contract template {string} and feed {string} and attributes:""") {
        (clientName: String, smartContractName: String, smartContractTemplateName: String, feedName: String, data: DataTable) =>
            logger.info(s"Registering smart contract template $smartContractTemplateName for $clientName...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val smartContractId = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"Unknown smart contract name $smartContractName"))
            val feed = scenarioContext.registeredDataFeeds.getOrElse(feedName, Fail(s"unknown datafeed $feedName"))
            val feeds = Collection(feed.id)
            val regulators = scenarioContext.smartContractRegulators.getOrElse(smartContractName, Collection.empty)

            val attrs = data.asMaps().asScala.map(_.get("value")).toArray
            val attributes =
                smartContractTemplateName match {
                    case "IndexTrade" =>
                        val ownerAddress = wallet.createSingleOwnerAddress.orFail("Can not create issuer address for SC")
                        val identityBase64 =
                            if (attrs(0) == "undefined")
                                toB64(ownerAddress.toByteArray)
                            else attrs(0)

                        attrs
                            .updated(0, identityBase64)
                            .updated(1, scenarioContext.investmentTokenType(attrs(1)))
                    case _ =>
                        Fail(s"Unknown smart contract template name $smartContractTemplateName")
                }

            val result =
                wallet
                    .createSmartContract(
                        id = smartContractId,
                        templateId = smartContractTemplateName,
                        attributes = attributes,
                        dataFeeds = feeds,
                        regulators = regulators
                    )
            logger.debug(s"registration result $result")
            result
                .expectFail(0)

            logger.info(s"Registered smart contract template $smartContractTemplateName failed as expected.")
    }

    When("""{string} sends his {string} token to {string} smart contract and deal metadata:""") {
        (clientName: String, tokenName: String, smartContractName: String, dealDataTable: DataTable) =>
            logger.info(s"$clientName sends his $tokenName token to $smartContractName")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val (_, maybeTokenId) = scenarioContext.tokenAddress.getOrElse((clientName, tokenName), Fail(s"No token address for $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            //      val waiters = scenarioContext.waiters.getOrElse(clientName, Fail(s"No waiters for $clientName"))

            logger.info(s"token id $maybeTokenId")
            //            val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
            //                if (events.owner.tokensBurn.nonEmpty)
            //                    Some(events.owner.tokensBurn)
            //                else
            //                    None
            //            }

            val dealData = dealDataTable.asMaps().asScala.map(_.get("value"))
            logger.info(s"deal data:")
            dealData.foreach { v => logger.info(s"value: $v") }
            maybeTokenId.map { tokenId =>
                logger.info(s"sending token id $tokenId")
                wallet
                    .sendTokenToSmartContract(destinationAddress, UUID.randomUUID().toString, Collection(tokenId), Bytes.empty, collectionFromIterable(dealData))
                    .orFail(s"Failed to send token to $smartContractName")
                logger.info(s"token send out...")
                //                val deleted = await.get
                //                logger.info(s"Token has been deleted:${deleted.mkString("\n\t", "\n\t", "")}")
            }
    }

    When("""{string} can not sends his {string} token to {string} smart contract and deal metadata:""") {
        (clientName: String, tokenName: String, smartContractName: String, dealDataTable: DataTable) =>
            logger.info(s"$clientName sends his $tokenName token to $smartContractName")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val (_, maybeTokenId) = scenarioContext.tokenAddress.getOrElse((clientName, tokenName), Fail(s"No token address for $clientName"))
            val destinationAddress = scenarioContext.smartContractAddress.getOrElse(smartContractName, Fail(s"No smart contract address for $smartContractName"))
            //      val waiters = scenarioContext.waiters.getOrElse(clientName, Fail(s"No waiters for $clientName"))

            logger.info(s"token id $maybeTokenId")


            val dealData = dealDataTable.asMaps().asScala.map(_.get("value"))
            logger.info(s"deal data:")
            dealData.foreach { v => logger.info(s"value: $v") }
            maybeTokenId.map { tokenId =>
                logger.info(s"sending token id $tokenId")
                val r =
                    wallet
                        .sendTokenToSmartContract(destinationAddress, UUID.randomUUID().toString, Collection(tokenId), Bytes.empty, collectionFromIterable(dealData))
                logger.info(s"send result: $r")
                r.expectFail(0)
                logger.info(s"tokens not  send out...")
            }
    }

}
