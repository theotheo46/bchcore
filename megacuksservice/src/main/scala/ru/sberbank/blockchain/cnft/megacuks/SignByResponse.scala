package ru.sberbank.blockchain.cnft.megacuks

import ru.sberbank.blockchain.cnft.commons.{LoggingSupport, Result}
import utility.Decoder
import ru.sberbank.blockchain.cnft.wallet.dsl._

import scala.xml.XML

/**
 * @author Andrew Pudovikov
 */
case class SignByResponse(
    rqUID: String,
    serviceName: String,
    systemId: String,
    statusCode: Int,
    fileData: String,
    signedData: String,
)

object SignByResponse extends LoggingSupport {

    implicit val XMLDecoder: Decoder[SignByResponse, String] =
        (encoded: String) => {
            logger.trace(s"MegaCuks sign response: $encoded")
            val node = XML.loadString(encoded)

            {
                for {
                    rqUID <- SignByResponseValidator.rqUID((node \ "RqUID").text)
                    serviceName <- SignByResponseValidator.serviceName((node \ "ServiceName").text)
                    systemId <- SignByResponseValidator.systemId((node \ "SystemId").text)
                    statusCode <- SignByResponseValidator.statusCode((node \ "Status" \ "StatusCode").text).map(_.toInt)
                    fileData <- SignByResponseValidator.fileData((node \ "FileData" \ "FileData").text)
                    signedData <- SignByResponseValidator.signedData((node \ "SignedData").text)
                } yield
                    SignByResponse(rqUID, serviceName, systemId, statusCode, fileData, signedData)
            }
                .orFail(s"input validation failed")
        }

}

object SignByResponseValidator {

    def validate(source: String, input: String, pattern: String, maxLength: Long): Result[String] =
        for {
            _ <- Result.expect(input.length <= maxLength, s"$source data length exceed maximum allowed")
            _ <- Result.expect(input.matches(pattern), s"$source data contains non valid symbols")
        } yield input


    def rqUID(input: String): Result[String] =
        validate("RqUID", input, "[0-f,-]+", 200)

    def serviceName(input: String): Result[String] =
        validate("ServiceName", input, ".*", 36)

    def systemId(input: String): Result[String] =
        validate("SystemId", input, ".*", 36)

    def statusCode(input: String): Result[String] =
        validate("StatusCode", input, "[0-9]+", 42)

    def fileData(input: String): Result[String] =
        validate("FileData", input, "^[-A-Za-z0-9+/]*={0,3}$", 2147483647)

    def signedData(input: String): Result[String] =
        validate("SignedData", input, "^[-A-Za-z0-9+/]*={0,3}$", 2147483647)
}