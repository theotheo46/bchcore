package ru.sberbank.blockchain.cnft.megacuks

import ru.sberbank.blockchain.cnft.commons.LoggingSupport
import utility.Decoder

import scala.xml.XML

/**
 * @author Andrew Pudovikov
 */
case class CheckDocSignDetachRes(
    rqUID: String,
    rsTm: String,
    serviceName: String,
    systemId: String,
    statusCode: Int,
    fileData: String,
    signData: String,
    totalCheckResult: String,
    certStatus: String
)

object CheckDocSignDetachRes extends LoggingSupport {

    implicit val XMLDecoder: Decoder[CheckDocSignDetachRes, String] =
        (encoded: String) => {
            logger.trace(s"MegaCuks check sign response: $encoded")
            val node = XML.loadString(encoded)
            CheckDocSignDetachRes(
                rqUID = (node \ "RqUID").text,
                rsTm = (node \ "RsTm").text,
                serviceName = (node \ "ServiceName").text,
                systemId = (node \ "SystemId").text,
                statusCode = (node \ "Status" \ "StatusCode").text.toInt,
                fileData = (node \ "FileData" \ "FileData").text,
                signData = (node \ "FileData" \ "SignData").text,
                totalCheckResult = (node \ "TotalCheckResult").text,
                certStatus = (node \ "CertificateResult" \ "CertStatus").text
            )
        }
}