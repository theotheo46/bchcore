package ru.sberbank.blockchain.cnft.megacuks

/**
 * @author Alexey Polubelov
 */
case class MegaCuksConfiguration(
    megaCuksVerifyService: String,
    megaCuksSystemId: String,
    megaCuksBsnCode: String,
    defaultKeyService: MegaCuksKeyService,
    keyServiceOverrides: Map[String, MegaCuksKeyService] = Map.empty,
)
