package ru.sberbank.blockchain.cnft.megacuks

import utility.Encoder

import java.time.Instant

/**
 * @author Andrew Pudovikov
 */
case class SignByRequest(
    rqUID: String,
    serviceName: String,
    systemId: String,
    fileData: String
)

object SignByRequest {

    implicit val XMLEncoder: Encoder[SignByRequest, String] =
        (value: SignByRequest) => {
            val xml =
            // @formatter:off
                    <SignRq>
                    <RqUID>{value.rqUID}</RqUID>
                    <RqTm>{Instant.now().toString}</RqTm>
                    <ServiceName>{value.serviceName}</ServiceName>
                    <SystemId>{value.systemId}</SystemId>
                    <FileData>
                        <FileData>{value.fileData}</FileData>
                    </FileData>
                </SignRq>
                // @formatter:on
            xml.toString()
        }

}