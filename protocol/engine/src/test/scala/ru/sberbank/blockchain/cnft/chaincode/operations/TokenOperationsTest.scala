//package ru.sberbank.blockchain.cnft.chaincode.operations
//
//import java.nio.charset.StandardCharsets
//
//import com.google.protobuf.ByteString
//import org.enterprisedlt.fabric.contract.OperationContextMock
//import org.hyperledger.fabric.shim.ledger.CompositeKey
//import org.mockito.ArgumentMatchers
//import org.mockito.ArgumentMatchers.anyString
//import org.mockito.Mockito.{doNothing, verify, when}
//import org.scalatest.funsuite.AnyFunSuite
//import ru.sberbank.blockchain.cnft.proto.tokens.OwnershipType.OwnershipEnum.MULTI
//import ru.sberbank.blockchain.cnft.proto.tokens._
//import ru.sberbank.blockchain.common.cryptography.{BCCryptography, CryptographySettings}
//
//
//class TokenOperationsTest extends AnyFunSuite {
//    private val tokenBody = TokenBody(
//        id = "tokenId",
//        tokenType = "SBC"
//    )
//
//    private val signedToken = SignedToken(tokenBody = Option(tokenBody))
//    private val token = Token(
//        tokenId = "token1",
//        owner = ByteString.copyFrom("".getBytes(StandardCharsets.UTF_8)),
//        signedToken = Some(signedToken),
//        issuerCommitment = ByteString.copyFrom("".getBytes(StandardCharsets.UTF_8)),
//        regulation = ByteString.copyFrom("".getBytes(StandardCharsets.UTF_8)),
//    )
//
//
//    test("testGetToken / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs = OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("Token", "SBC").toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(contractCodecs.ledgerCodec.encode(token)))
//        val res = new AllOperations {}.getToken("SBC")
//        res.map { result =>
//            assertResult(token)(result)
//        }
//    }
//    test("testStoreToken / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs = OperationContextMock.getContractCodecs
//        val cryptography = new BCCryptography(CryptographySettings.EC256_SHA256)
//        val encodedPair = cryptography.createKey()
//        val signature = cryptography.createSignature(encodedPair.thePrivate, tokenBody.toByteArray)
//        val issuerPublicKey = ByteString.copyFrom(encodedPair.thePublic)
//        val owner = TokenOwner(
//            ownershipType = MULTI,
//            content = MultiKey(
//                require = 1,
//                keys = Array(ByteString.copyFrom(issuerPublicKey.toByteArray))
//            ).toByteString
//        )
//
//        val ownerByteString = ByteString.copyFrom(owner.toByteArray)
//
//        val tokenType: TokenType = TokenType(typeID = "SBC", tokenMeta = "SBC", issuerPublicKey = issuerPublicKey)
//        val tokenId = "token1"
//        val reservedTokenId = Token(
//            tokenId = tokenId,
//            owner = ownerByteString
//        )
//        val signedToken1 = SignedToken(
//            tokenBody = Option(tokenBody),
//            signature = ByteString.copyFrom(signature)
//        )
//        val idAccessToken = cryptography.createSignature(encodedPair.thePrivate, s"${tokenId}_Issue".getBytes(StandardCharsets.UTF_8))
//        val request = IssueTokenRequest(
//            tokenId = tokenId,
//            signedToken = Some(signedToken1),
//            idAccessToken = ByteString.copyFrom(idAccessToken),
//            issuerCommitment = ByteString.copyFrom(signature),
//            regulation = "regulation",
//        )
//        val token = Token(
//            tokenId = tokenId,
//            owner = ownerByteString,
//            signedToken = Some(signedToken1),
//            issuerCommitment = ByteString.copyFrom(signature),
//            regulation = ByteString.copyFrom("regulation".getBytes(StandardCharsets.UTF_8)),
//        )
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(anyString()))
//          .thenReturn(contractCodecs.ledgerCodec.encode(tokenType))
//        when(mockChaincode.getState(new CompositeKey("Token", tokenId).toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(contractCodecs.ledgerCodec.encode(reservedTokenId)))
//        doNothing().when(mockChaincode)
//          .putState(
//              ArgumentMatchers.eq(new CompositeKey("Token", token.tokenId).toString),
//              ArgumentMatchers.eq(contractCodecs.ledgerCodec.encode(token))
//          )
//        val res = new AllOperations {}.issueTokens(IssueTokensRequest(requests = Array(request)))
//        verify(mockChaincode)
//          .putState(
//              ArgumentMatchers.eq(new CompositeKey("Token", token.tokenId).toString),
//              ArgumentMatchers.eq(contractCodecs.ledgerCodec.encode(token))
//          )
//        res.map { result =>
//            assertResult(IssueTokensResponse(Seq(IssueTokenResponse(tokenId, processed = true))))(result)
//        }
//
//    }
//}
