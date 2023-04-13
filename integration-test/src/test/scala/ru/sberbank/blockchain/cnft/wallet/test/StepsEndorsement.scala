package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Chain, Fail, scenarioContext}
import ru.sberbank.blockchain.cnft.wallet.test.Util.{mkEndorsement, mkPublicEndorsement}

import java.nio.charset.StandardCharsets

class StepsEndorsement extends ScalaDsl with EN with LoggingSupport {

    When("""{string} asks {string} for endorsement""") { (client: String, regulator: String) =>
        logger.info(s"$client asks $regulator for endorsement...")
        val clientWallet = scenarioContext.wallets.getOrElse(client, Fail(s"Unknown client $client"))
        val regulatorWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown client $client"))

        val regulatorId = regulatorWallet.getIdentity.orFail(s"Fail to get walletIdentity for $regulator")
        val await = Util.awaitWalletEvents(regulatorWallet) { case (_, events) =>
            if (events.member.endorsementRequested.nonEmpty)
                Some(events.member.endorsementRequested)
            else
                None
        }
        clientWallet.requestEndorsement(regulatorId, "data".getBytes(StandardCharsets.UTF_8))
        await.get
    }

    When("""{string} approves {string} endorsement""") { (regulator: String, client: String) =>
        logger.info(s"$regulator approves $client endorsement...")
        val regulatorWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown client $regulator"))

        val clientWallet = scenarioContext.wallets.getOrElse(client, Fail(s"Unknown client $client"))
        val walletIdentity = clientWallet.getIdentity.orFail(s"Fail to get walletIdentity for $client")

        val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
            if (events.member.endorsed.nonEmpty)
                Some(events.member.endorsed)
            else
                None
        }
        regulatorWallet.endorseMember(walletIdentity, "dummy certificate".getBytes(StandardCharsets.UTF_8))
        val endorsed = await.get
        logger.info(s"Successfully endorsed: ${endorsed.map(_.toProtoString).mkString}")
    }

    When("""{string} public endorse member {string} with endorsement {string} of kind {string}""") {
        (endorser: String, client: String, endorseText: String, kindId: String) =>
            logger.info(s"$endorser endorse $client ")
            val endorserWallet = scenarioContext.wallets.getOrElse(endorser, Fail(s"Unknown client $endorser"))

            val clientWallet = scenarioContext.wallets.getOrElse(client, Fail(s"Unknown client $client"))
            val walletIdentity = clientWallet.getIdentity.orFail(s"Fail to get walletIdentity for $client")

            val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
                if (events.member.endorsedPublic.nonEmpty)
                    Some(events.member.endorsedPublic)
                else
                    None
            }
            endorserWallet.endorseMemberPublic(walletIdentity, kindId, endorseText.getBytes(StandardCharsets.UTF_8))
            val endorsed = await.get
            logger.info(s"Successfully endorsed: ${endorsed.map(_.toProtoString).mkString}")
    }

    When("""{string} revoke endorsement of kind {string} for member {string}""") {
        (endorser: String, kindId: String, client: String) =>
            logger.info(s"$endorser revoke endorsement $client ")
            val endorserWallet = scenarioContext.wallets.getOrElse(endorser, Fail(s"Unknown client $endorser"))

            val clientWallet = scenarioContext.wallets.getOrElse(client, Fail(s"Unknown client $client"))
            val walletIdentity = clientWallet.getIdentity.orFail(s"Fail to get walletIdentity for $client")

            val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
                if (events.member.revokedEndorsements.nonEmpty)
                    Some(events.member.revokedEndorsements)
                else
                    None
            }
            endorserWallet.revokePublicEndorsement(walletIdentity, kindId)
                    val endorsed = await.get
            logger.info(s"Successfully revoke endorsement: ${endorsed.map(_.toProtoString).mkString}")
    }


    When("""{string} rejects {string} endorsement""") { (regulator: String, client: String) =>
        logger.info(s"$regulator rejects $client endorsement...")
        val regulatorWallet = scenarioContext.wallets.getOrElse(regulator, Fail(s"Unknown client $regulator"))
        val clientWallet = scenarioContext.wallets.getOrElse(client, Fail(s"Unknown client $client"))
        val walletIdentity = clientWallet.getIdentity.orFail(s"Fail to get walletIdentity for $client")
        val await = Util.awaitWalletEvents(clientWallet) { case (_, events) =>
            if (events.member.endorsementRejected.nonEmpty)
                Some(events.member.endorsementRejected)
            else
                None
        }
        regulatorWallet.rejectEndorsement(walletIdentity, "No data provided")
        await.get
    }

    When("""{string} checks his endorsements""") { (clientName: String) =>
        logger.info(s"$clientName checks his endorsements...")
        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val endorsements = wallet.listEndorsements.orFail("Unable to get Endorsements")
        logger.info(s"Endorsements are: ${mkEndorsement(endorsements)}")
    }

    When("""{string} checks his public endorsements""") { (clientName: String) =>
        logger.info(s"$clientName checks his endorsements...")
        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val id = wallet.getIdentity.getOrElse(Fail(s"can not get id"))
        val endorsements = Chain.listPublicEndorsements(id).orFail("Unable to get Endorsements")
        logger.info(s"Endorsements are: ${mkPublicEndorsement(endorsements)}")
    }

    When("""{string} checks his endorsements are empty""") { (clientName: String) =>
        logger.info(s"$clientName checks his endorsements  are empty...")
        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val endorsements = wallet.listEndorsements.orFail("Unable to get Endorsements")
        assert(endorsements.isEmpty)
    }
}
