//package ru.sberbank.blockchain.cnft.chaincode.operations
//
//import com.google.protobuf.ByteString
//import org.enterprisedlt.fabric.contract.OperationContextMock
//import org.hyperledger.fabric.protos.msp.Identities.SerializedIdentity
//import org.hyperledger.fabric.shim.ledger.CompositeKey
//import org.mockito.ArgumentMatchers
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.{doNothing, times, verify, when}
//import org.scalatest.funsuite.AnyFunSuite
//import org.slf4j.{Logger, LoggerFactory}
//import ru.sberbank.blockchain.cnft.proto.tokens.OwnershipType.OwnershipEnum.MULTI
//import ru.sberbank.blockchain.cnft.proto.tokens._
//import ru.sberbank.blockchain.common.cryptography.{BCCryptography, CryptographySettings}
//
//
//class TokenIDOperationsTest extends AnyFunSuite {
//    private val GateId = "GATE0"
//
//    private val logger: Logger = LoggerFactory.getLogger(this.getClass)
//
//    private val multiKey = MultiKey(require = 1, keys = Seq(ByteString.copyFrom("signature".getBytes())))
//
//    private val owner = TokenOwner(ownershipType = MULTI, content = multiKey.toByteString)
//
//    private val tokenIdRequest = TokenIDRequest(owner = Option(owner), cid = GateId, count = 1)
//
//    private val createIDsRequest = ReserveIDsRequest(requests = Seq(tokenIdRequest))
//
//    private val deal = Deal(dealId = ByteString.copyFrom("dealId".getBytes()), changes = Map.apply[String, TokenOwner]("idOwner" -> owner))
//
//    private val token = Token(
//        owner = ByteString.copyFrom(owner.toByteArray),
//        signedToken = None,
//        issuerCommitment = ByteString.EMPTY,
//        regulation = ByteString.EMPTY
//    )
//
//    private val dealRequestWhitoutSignature = DealRequest(deal = Option(deal), signatures = Map.empty)
//
//    test("testMakeDeal / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs = OperationContextMock.getContractCodecs
//        val cryptography = new BCCryptography(CryptographySettings.EC256_SHA256)
//        val encodedPair = cryptography.createKey()
//        val signature = cryptography.createSignature(encodedPair.thePrivate, deal.toByteArray)
//        val multiSign = MultiSign(multiSign = Array(ByteString.copyFrom(signature)))
//
//        ByteString.copyFrom(signature).toByteArray
//        val dealRequest = DealRequest(
//            deal = Option(deal),
//            signatures = Map.apply[String, MultiSign]("idOwner" -> multiSign)
//        )
//        val oldOwner = TokenOwner(
//            ownershipType = MULTI,
//            content = MultiKey(
//                require = 1,
//                keys = Array(ByteString.copyFrom(encodedPair.thePublic))
//            ).toByteString
//        )
//        val newOwner = TokenOwner(ownershipType = MULTI, content = multiKey.toByteString)
//        val token = Token(
//            owner = ByteString.copyFrom(oldOwner.toByteArray),
//            signedToken = None,
//            issuerCommitment = ByteString.EMPTY,
//            regulation = ByteString.EMPTY
//        )
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("Token", "idOwner").toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(token))
//        val res = new AllOperations {}.makeDeal(dealRequest)
//        verify(mockChaincode, times(2)).getState(new CompositeKey("Token", "idOwner").toString)
//        val updatedTokenOwner = token.copy(owner = ByteString.copyFrom(newOwner.toByteArray))
//        verify(mockChaincode)
//          .putState(
//              ArgumentMatchers.eq(new CompositeKey(s"Token", "idOwner").toString),
//              ArgumentMatchers.eq(contractCodecs.ledgerCodec.encode(updatedTokenOwner))
//          )
//        res.map { result =>
//            assertResult(())(result)
//        }
//    }
//
//    test("testMakeDeal / Not enough signatures") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs = OperationContextMock.getContractCodecs
//        val cryptography = new BCCryptography(CryptographySettings.EC256_SHA256)
//        val encodedPair = cryptography.createKey()
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("Token", "idOwner").toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(token))
//        val res = new AllOperations {}.makeDeal(dealRequestWhitoutSignature)
//        verify(mockChaincode).getState(new CompositeKey("Token", "idOwner").toString)
//        assertResult("Not enough signatures")(res.left.get)
//    }
//
//    test("testMakeDeal / No owner for id") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("byte[]", "noId").toString))
//          .thenReturn(any[Array[Byte]])
//        val res = new AllOperations {}.makeDeal(dealRequestWhitoutSignature)
//        assertResult("No owner for idOwner")(res.left.get)
//    }
//
//    test("testMakeDeal / Missing signature from id") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs = OperationContextMock.getContractCodecs
//        val cryptography = new BCCryptography(CryptographySettings.EC256_SHA256)
//        val encodedPair = cryptography.createKey()
//        val signature = cryptography.createSignature(encodedPair.thePrivate, deal.toByteArray)
//        val multiSign = MultiSign(multiSign = Array(ByteString.copyFrom(signature)))
//        val dealRequest = DealRequest(
//            deal = Option(deal),
//            signatures = Map.apply[String, MultiSign]("noSign" -> multiSign)
//        )
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("Token", "idOwner").toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(token))
//        new AllOperations {}.makeDeal(dealRequest)
//          .map { result =>
//              logger.debug(s"result: $result")
//              assertResult("Missing signature from: idOwner")(result)
//
//          }
//    }
//
//    test("testBurnTokenID / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs = OperationContextMock.getContractCodecs
//        val cryptography = new BCCryptography(CryptographySettings.EC256_SHA256)
//        val encodedPair = cryptography.createKey()
//        val owner = TokenOwner(
//            ownershipType = MULTI,
//            content = MultiKey(
//                require = 1,
//                keys = Array(ByteString.copyFrom(encodedPair.thePublic))
//            ).toByteString
//        )
//        val token = Token(
//            owner = ByteString.copyFrom(owner.toByteArray),
//            signedToken = None,
//            issuerCommitment = ByteString.EMPTY,
//            regulation = ByteString.EMPTY
//        )
//
//        val burnToken = BurnToken(
//            id = "SBC_ID"
//        )
//        val signature = cryptography.createSignature(encodedPair.thePrivate, burnToken.toByteArray)
//
//        val burnTokenIDRequest = BurnTokenIDRequest(
//            burnToken = Some(burnToken),
//            signature = Seq(ByteString.copyFrom(signature))
//        )
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("Token", burnToken.id).toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(token))
//        doNothing().when(mockChaincode)
//          .delState(
//              ArgumentMatchers.eq(new CompositeKey("Token", burnToken.id).toString)
//          )
//        val res = new AllOperations {}.burnTokenID(burnTokenIDRequest)
//        verify(mockChaincode).getState(new CompositeKey("Token", burnToken.id).toString)
//        verify(mockChaincode).delState(new CompositeKey("Token", burnToken.id).toString)
//        assertResult(owner.toByteArray)(res.right.get)
//    }
//
//    test("testCreateTokenIDs / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs = OperationContextMock.getContractCodecs
//        val TokenCounterKey = "MAX_TOKEN"
//        OperationContextMock.setMockContext(mockChaincode)
//        val mspId = "MSP-ID-TEST"
//        val IDsCounterKey = new CompositeKey("SIMPLE", mspId, GateId, TokenCounterKey)
//        when(
//            mockChaincode.getCreator
//        ).thenReturn(
//            contractCodecs.ledgerCodec.encode(
//                SerializedIdentity.newBuilder().setMspid(mspId).build()
//            )
//        )
//        when(
//            mockChaincode.getState(
//                ArgumentMatchers.eq(IDsCounterKey.toString)
//            )
//        ).thenReturn(
//            contractCodecs.ledgerCodec.encode("1")
//        )
//
//        val key = s"${mspId}_${GateId}_1"
//        doNothing().when(mockChaincode)
//          .putState(
//              ArgumentMatchers.eq(new CompositeKey("Owner", key).toString),
//              ArgumentMatchers.eq(contractCodecs.ledgerCodec.encode(owner))
//          )
//        doNothing().when(mockChaincode)
//          .putState(
//              ArgumentMatchers.eq(IDsCounterKey.toString),
//              ArgumentMatchers.eq(contractCodecs.ledgerCodec.encode(2))
//          )
//        val res = new AllOperations {}.reserveTokenIDs(createIDsRequest)
//        logger.debug(s"result: ${res.right.get.toString}")
//        verify(mockChaincode).getCreator
//        verify(mockChaincode).getState(IDsCounterKey.toString)
//    }
//
//    test("testGetIDOwner / should works fine") {
//        val chaincodeMock = OperationContextMock.getMockChaincode
//        val contractCodecs = OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(chaincodeMock)
//
//        val TestKey = "KeyOwnerSBC"
//        val TestCompositeKey = new CompositeKey("Token", TestKey).toString
//        when(chaincodeMock.getState(TestCompositeKey)).thenReturn(contractCodecs.ledgerCodec.encode(token))
//
//        val res = new AllOperations {}.getOwner(TestKey)
//        logger.debug(s"result: $res")
//        verify(chaincodeMock).getState(TestCompositeKey)
//        res.foreach { ownerResult =>
//            assertResult(owner.toByteArray)(ownerResult)
//        }
//    }
//
//    test("testGetIDOwner / no such ID KeyOwnerSBCA") {
//        val chaincodeMock = OperationContextMock.getMockChaincode
//        val contractCodecs = OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(chaincodeMock)
//
//        val TestKey = "KeyOwnerSBCA"
//        val TestCompositeKey = new CompositeKey("Token", TestKey).toString
//        when(chaincodeMock.getState(TestCompositeKey)).thenReturn(null)
//
//        val res = new AllOperations {}.getOwner(TestKey)
//        verify(chaincodeMock).getState(TestCompositeKey)
//        res.foreach { result =>
//            assertResult("No such ID KeyOwnerSBCA")(result)
//        }
//    }
//}
