package ru.sberbank.blockchain.cnft.megacuks

import utility.Encoder

import java.time.Instant

/**
 * @author Andrew Pudovikov
 */
case class CheckDocSignDetachReq(
    rqUID: String,
    serviceName: String,
    systemId: String,
    fileData: String,
    signData: String,
    bsnCode: String
)

object CheckDocSignDetachReq {

    implicit val XMLEncoder: Encoder[CheckDocSignDetachReq, String] =
        (value: CheckDocSignDetachReq) => {
            val xml =
            // @formatter:off
                <OcspRq>
                    <RqUID>{value.rqUID}</RqUID>
                    <RqTm>{Instant.now().toString}</RqTm>
                    <ServiceName>{value.serviceName}</ServiceName>
                    <SystemId>{value.systemId}</SystemId>
                    <FileData>
                        <FileData>{value.fileData}</FileData>
                        <SignData>{value.signData}</SignData>
                    </FileData>
                    <BsnCode>{value.bsnCode}</BsnCode>
                </OcspRq>
            // @formatter:on
            xml.toString()
        }
}