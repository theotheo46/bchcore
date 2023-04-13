//package ru.sberbank.blockchain.cnft.megacuksservice
//
//import org.mockito.ArgumentMatchers.anyString
//import org.mockito.Mockito.{mock, when}
//import org.scalatest.funsuite.AnyFunSuite
//import ru.sberbank.blockchain.cnft.common.types.ResultOps
//import ru.sberbank.blockchain.cnft.commons.Result
//import ru.sberbank.blockchain.cnft.megacuks.{MegaCuksConfiguration, MegaCuksCryptography, MegaCuksKeyService, MegaCuksServiceSpec}
//import ru.sberbank.blockchain.common.cryptography.model.ChallengeSpec
//
//import java.nio.charset.StandardCharsets
//import java.util.Base64
//import scala.xml.Elem
//
//class MegaCuksCryptoTest extends AnyFunSuite {
//
//    private val megaCuksMock = mock(classOf[MegaCuksServiceSpec[Result]])
//
//    private val cryptography =
//        new MegaCuksCryptography(
//            megaCuksService = megaCuksMock,
//            MegaCuksConfiguration(
//                megaCuksVerifyService = "",
//                megaCuksSystemId = "",
//                megaCuksBsnCode = "",
//                defaultKeyService =
//                    MegaCuksKeyService(
//                        id = "",
//                        certificateB64 = "MIIDCjCCArmgAwIBAgITEgBdgOR4rLmKnl4zhwABAF2A5DAIBgYqhQMCAgMwfzEjMCEGCSqGSIb3DQEJARYUc3VwcG9ydEBjcnlwdG9wcm8ucnUxCzAJBgNVBAYTAlJVMQ8wDQYDVQQHEwZNb3Njb3cxFzAVBgNVBAoTDkNSWVBUTy1QUk8gTExDMSEwHwYDVQQDExhDUllQVE8tUFJPIFRlc3QgQ2VudGVyIDIwHhcNMjIwMTI3MTA1NjMyWhcNMjIwNDI3MTEwNjMyWjAPMQ0wCwYDVQQDDAR0ZXN0MGYwHwYIKoUDBwEBAQEwEwYHKoUDAgIkAAYIKoUDBwEBAgIDQwAEQO+KG8XaMBR7nWx76c0hkz+uve/oXtbFgJi++qmxz0qQnvf558PFvCv9mnHinAkGHqhIws+B71zDa0+5bMW8NYWjggF3MIIBczAPBgNVHQ8BAf8EBQMDB/AAMBMGA1UdJQQMMAoGCCsGAQUFBwMCMB0GA1UdDgQWBBRlbGJKjTNT4MOpJxCkQnpjB3D26DAfBgNVHSMEGDAWgBROgz4Uae/sXXqVK18R/jcyFklVKzBcBgNVHR8EVTBTMFGgT6BNhktodHRwOi8vdGVzdGNhLmNyeXB0b3Byby5ydS9DZXJ0RW5yb2xsL0NSWVBUTy1QUk8lMjBUZXN0JTIwQ2VudGVyJTIwMigxKS5jcmwwgawGCCsGAQUFBwEBBIGfMIGcMGQGCCsGAQUFBzAChlhodHRwOi8vdGVzdGNhLmNyeXB0b3Byby5ydS9DZXJ0RW5yb2xsL3Rlc3QtY2EtMjAxNF9DUllQVE8tUFJPJTIwVGVzdCUyMENlbnRlciUyMDIoMSkuY3J0MDQGCCsGAQUFBzABhihodHRwOi8vdGVzdGNhLmNyeXB0b3Byby5ydS9vY3NwL29jc3Auc3JmMAgGBiqFAwICAwNBAD825otHIlQkHCnEBwB5X3+fOTreY9cngp3YGC2espMos6pRfDx6aDxHJnx34gi/1Z7w5FmRFN2T4MxvEUEOEng="
//                    )
//            )
//        )
//
//    private val cryptographyIncorrect =
//        new MegaCuksCryptography(
//            megaCuksService = megaCuksMock,
//            MegaCuksConfiguration(
//                megaCuksVerifyService = "",
//                megaCuksSystemId = "",
//                megaCuksBsnCode = "",
//                defaultKeyService =
//                    MegaCuksKeyService(
//                        id = "",
//                        certificateB64 = "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"
//                    )
//            )
//        )
//
//
////    test("should create request to sign and parse dummy response") {
////        val dummyResponse: Elem =
////            <Response>
////                <RqUID>r</RqUID>
////                <RsTm>f</RsTm>
////                <ServiceName>o</ServiceName>
////                <SystemId>o</SystemId>
////                <Status>
////                    <StatusCode>0</StatusCode>
////                </Status>
////                <FileData>ddd</FileData>
////                <TotalCheckResult>SignatureValid</TotalCheckResult>
////                <CertificateResult>ggg</CertificateResult>
////            </Response>
////
////        when(megaCuksMock.requestVerify(anyString)).thenReturn(Right(dummyResponse.toString()))
////        val verifySigResult: Result[Boolean] =
////            cryptography.verifySignature(Array.empty, "foo".getBytes("UTF-8"), "foo".getBytes("UTF-8"))
////
////        assert(verifySigResult.contains(true))
////
////    }
//
//
////    test("should create signature") {
////        val signatureBytes = "signature".getBytes(StandardCharsets.UTF_8)
////        val signatureB64String = new String(Base64.getEncoder.encode(signatureBytes), StandardCharsets.UTF_8)
////        println(signatureB64String)
////        val dummySignResponse: Elem =
////            <Response>
////                <RqUID>r</RqUID>
////                <ServiceName>o</ServiceName>
////                <SystemId>o</SystemId>
////                <Status>
////                    <StatusCode>0</StatusCode>
////                </Status>
////                <FileData>ddd</FileData>
////                <SignedData>
////                    {signatureB64String}
////                </SignedData>
////            </Response>
////
////        when(megaCuksMock.requestSign(anyString)).thenReturn(Right(dummySignResponse.toString()))
////
////        val receivedBytes: Result[Array[Byte]] = cryptography.createSignature("", "foo".getBytes(StandardCharsets.UTF_8))
////        assert(receivedBytes.map(_.sameElements(signatureBytes)) == Right(true))
////    }
//
//    test("get public key") {
//        for {
//            challengeSpecResult <- cryptography.publicKey("")
//            maybeChallengeSpec <- ResultOps.fromOption(challengeSpecResult, "Certificate can not parse!")
//            publicKey <- Result {
//                val public = ChallengeSpec.parseFrom(maybeChallengeSpec).value
//                val encodedPubKey = Base64.getEncoder.encodeToString(public)
//                println(s"Public key from GOST Certificate: $encodedPubKey")
//                encodedPubKey
//            }
//        } yield assert(publicKey.nonEmpty)
//    }
//
//    test("get public key fail - incorrect Key Usage type") {
//        assert(cryptographyIncorrect.publicKey("").isLeft)
//    }
//}