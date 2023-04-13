package ru.sberbank.blockchain.cnft.wallet.test

import io.cucumber.scala.{EN, ScalaDsl}
import ru.sberbank.blockchain.cnft.common.types.Bytes
import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import ru.sberbank.blockchain.cnft.wallet.dsl._
import ru.sberbank.blockchain.cnft.wallet.test.SharedContext.{Chain, Fail, scenarioContext}

class StepsMemberInformation extends ScalaDsl with EN with LoggingSupport {
    When("""Admin successfully updates {string} member information""") {
        (clientName: String) =>
            val admin = Chain.connectWallet("http://localhost:8983")
            val client = scenarioContext.wallets.getOrElse(clientName, Fail(s"can not get client wallet"))
            val clientId = client.getIdentity.orFail(s"can not get client identity")
            val memberInfo = admin.chain.getMember(clientId).orFail(s"can not get member information")
            val updatedInfo = memberInfo.withEncryptionPublic(Bytes.empty)
            admin.updateMember(updatedInfo).orFail(s"admin shall be able to update user information")
    }

    When("""{string} is failed to update {string} member information""") {
        (clientName1: String, clientName2: String) =>

            val client1 = scenarioContext.wallets.getOrElse(clientName1, Fail(s"Unknown client $clientName1"))
            val client2 = scenarioContext.wallets.getOrElse(clientName2, Fail(s"can not get client wallet"))
            val clientId = client2.getIdentity.orFail(s"can not get client identity")
            val memberInfo = client2.chain.getMember(clientId).orFail(s"can not get member information")
            val updatedInfo = memberInfo.withAccessPublic(Bytes.empty)
            client1.updateMember(updatedInfo).expectFail
    }

    When("""Member {string} list his member information""") { (clientName: String) =>
        logger.info(s"listing member information for $clientName ...")
        val wallet = scenarioContext.wallets.getOrElse(clientName, Fail(s"Unknown client $clientName"))
        val id = wallet.getIdentity.orFail(s"can not get identity")
        val memberInfo = wallet.chain.getMember(id).orFail(s"can not get member information")
        logger.info(s"member information:")
        logger.info(s"id: ${memberInfo.id}\n" +
            s"signing public:${memberInfo.signingPublic.mkString(" ")}\n" +
            s"access public: ${memberInfo.accessPublic.mkString(" ")}\n" +
            s"ecnryption public: ${memberInfo.encryptionPublic.mkString(" ")}\n" +
            s"is Admin ${memberInfo.isAdmin}")
    }
}
