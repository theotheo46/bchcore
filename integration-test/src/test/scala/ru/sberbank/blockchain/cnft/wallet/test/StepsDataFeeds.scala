package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.datatable.DataTable
import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.common.types.{Collection, Result}
import ru.sberbank.blockchain.cnft.commons.{LoggingSupport, collectionFromIterable}
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.model.{DataFeedValue, DescriptionField, FieldMeta, FieldType}
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.spec.CNFTWalletSpec
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Chain, Fail, scenarioContext}

import java.time.{Clock, LocalDateTime}
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

class StepsDataFeeds extends ScalaDsl with EN with LoggingSupport {

    When("""{string} registering datafeed {string}""") {
        (clientName: String, feedName: String) =>
            logger.info(s"Registering datafeed $feedName ")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val dataFeed =
                wallet.registerDataFeed(
                    description = Collection(
                        DescriptionField("description", FieldType.Text, s"test feed $feedName")
                    ),
                    fields = Collection(
                        FieldMeta("timestamp", FieldType.Date)
                    )
                ).map(_.value).orFail(s"can not register datafeed $feedName")


            val await = Util.awaitWalletEvents(wallet) { case (_, events) =>
                if (events.dataFeeds.registeredDataFeed.nonEmpty)
                    Some(events.dataFeeds.registeredDataFeed)
                else None
            }

            val added = await.get
            logger.info(s"Data Feed registered: ${added.map(_.toProtoString).mkString}")

            scenarioContext.registeredDataFeeds.put(feedName, dataFeed)
            logger.info(s"datafeed $dataFeed with id ${dataFeed.id} registered")
    }

    When("""list data feeds""") { () =>
        logger.info(s"list data feeds:")
        val feedAddress = Chain.listDataFeeds.orFail("Can't list feeds").map(_.id)
        feedAddress.foreach(id =>
            logger.info(s"$id"))
    }

    private def submitFeedValue(wallet: CNFTWalletSpec[Result], dataFeedName: String, feedValue: String) = {

        val dataFeed = scenarioContext.registeredDataFeeds.getOrElse(dataFeedName, Fail(s"Unknown datafeed $dataFeedName"))
        wallet.submitDataFeedValue(
            Collection(
                DataFeedValue(
                    id = dataFeed.id,
                    content = Collection(feedValue)
                )
            )
        )
    }

    When("""{string} submitted value {string} for datafeed {string}""") {
        (clientName: String, feedValue: String, feedName: String) =>
            logger.info(s"submitting value to datafeed $feedName by $clientName")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            submitFeedValue(wallet, feedName, feedValue).getOrElse(
                Fail(s"Unsuccessful submit value: $feedValue for datafeed $feedName")
            )
            logger.info(s"Feed $feedName submited value $feedValue")
    }


    When("""{string} submitted for datafeed {string} value:""") {
        (clientName: String, feedName: String, dataTable: DataTable) =>
            logger.info(s"submitting value to datafeed $feedName by $clientName")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            wallet.createAddress
            val dataFeed = scenarioContext.registeredDataFeeds.getOrElse(feedName, Fail(s"Unknown datafeed $feedName"))
            val attributes = dataTable.asMaps().asScala
            wallet.submitDataFeedValue(
                Collection(
                    DataFeedValue(
                        id = dataFeed.id,
                        content = collectionFromIterable(
                            attributes.map(_.get("value"))
                                .map {
                                    case "timestamp_now" =>
                                        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS")
                                        formatter.format(LocalDateTime.now(Clock.systemUTC()))
                                    case v =>
                                        v
                                }
                        )
                    )
                )
            )
    }

    When("""{string} registered data feed for {string} with fields:""") {
        (clientName: String, dataFeedType: String, data: DataTable) =>
            logger.info(s"Registering data feed $dataFeedType for $clientName ...")
            val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
            val attributes = data.asMaps().asScala
            val fields = attributes.map { fieldMeta =>
                val id = fieldMeta.get("id")
                val typeId = fieldMeta.get("typeId")
                val description = fieldMeta.get("description")
                logger.info(s"FieldMeta: $id with typeId $typeId and description $description")
                FieldMeta(id, typeId)
            }.toArray
            val TxResult(_, _, dataFeed) =
                wallet
                    .registerDataFeed(
                        description = Collection(
                            DescriptionField(
                                name = "exchangerate",
                                typeId = FieldType.Text,
                                value = dataFeedType,
                            )
                        ),
                        fields = fields
                    )
                    .orFail(s"Failed to register data feed $dataFeedType")
            logger.info(s"Save dataFeedType $dataFeedType")
            scenarioContext.registeredDataFeeds.put(dataFeedType, dataFeed)
            logger.info(s"Registered data feed $dataFeedType for $clientName ($dataFeedType).")
    }

}
