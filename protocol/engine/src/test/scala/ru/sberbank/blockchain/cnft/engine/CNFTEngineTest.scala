//package ru.sberbank.blockchain.cnft.engine
//
//import org.mockito.ArgumentMatchers
//import org.mockito.Mockito.{doNothing, mock, when}
//import org.scalatest.freespec.AnyFreeSpec
//import ru.sberbank.blockchain.cnft.commons.{Bytes, Collection, Result}
//import ru.sberbank.blockchain.cnft.engine.Mocks._
//import ru.sberbank.blockchain.cnft.model._
//import ru.sberbank.blockchain.cnft.spec.CNFTChallenge
//import ru.sberbank.blockchain.common.cryptography.OperationType.TokenOperation
//
//class CNFTEngineTest extends AnyFreeSpec {
//
//    type Result[T] = Either[String, T]
//    val tokenId = "SBC"
//    val tokenType: TokenType = TokenType(typeId = tokenId, Collection.empty, issuerPublicKey = "SBC".getBytes(), issuerId = "I1")
//
//    val owner: TokenOwner = TokenOwner(require = 1, keys = Collection("key".getBytes()))
//    val reservedId: ReservedId = ReservedId(
//        tokenId = tokenId,
//        owner = owner
//    )
//    val tokenBody: TokenBody = TokenBody(tokenId = tokenId, tokenType = tokenId, content = Collection.empty)
//    val signedToken = SignedToken(tokenBody = tokenBody, signature = "theSignature".getBytes()).toByteArray
//    val issuerCommitment: Array[Byte] = "issuerCommitment".getBytes()
//    val issuedToken: IssuedToken = IssuedToken(signedToken = signedToken, issuerCommitment = issuerCommitment, regulations = Collection.empty)
//    val issuedTokenRequest: IssueTokenRequest = IssueTokenRequest(
//        tokenId = tokenId,
//        tokenIdAccess = tokenId.getBytes(),
//        signedToken = signedToken,
//        issuerCommitment = issuerCommitment,
//        frozen = false,
//        regulations = Collection.empty
//    )
//
//    "RegisterTokenType " - {
//
//        "Positive case SBC when Type id is None" in {
//            val registerTokenType: RegisterTokenType = RegisterTokenType(Collection.empty, issuerPublicKey = "SBC".getBytes(), issuerId = "issuerId")
//            val tokenType: TokenType = TokenType(tokenMeta = Collection.empty, issuerPublicKey = "SBC".getBytes(), typeId = "Token_Type_SBC_1", issuerId= "issuerId")
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val mockCNFTStoreSequence = getMockCNFTStoreSequence
//
//
//            when(mockStore.getTokenType(tokenType.typeId)).thenReturn(None)
//            when(mockStore.getSequence[TokenType]("TokenType", "gate1"))
//                .thenReturn(mockCNFTStoreSequence)
//            when(mockCNFTStoreSequence.next)
//                .thenReturn("Token_Type_SBC_1")
//
//            when(mockStore.getTokenType(tokenType.typeId)).thenReturn(None)
//
//            doNothing().when(mockStore).saveTokenType(tokenType)
//            doNothing().when(mockCNFTStoreSequence).end()
//
//            val result = mockCNFTEngine.registerTokenType(gateId = "gate1", Array(registerTokenType))
//            assert(result.isRight)
//        }
//
//        "IssuerPublicKey is missing" in {
//            val registerTokenType: RegisterTokenType = RegisterTokenType(Collection.empty, issuerPublicKey = Array[Byte](), issuerId= "issuerId")
//            val TestResult = Left("issuerPublicKey is missing")
//            val tokenType: TokenType = TokenType(typeId = "SBC", tokenMeta = Collection.empty, issuerPublicKey = Bytes.empty, issuerId= "issuerId")
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val mockCNFTStoreSequence = getMockCNFTStoreSequence
//
//            when(mockStore.getTokenType(tokenType.typeId)).thenReturn(None)
//            when(mockStore.getSequence[TokenType]("TokenType", "gate1"))
//                .thenReturn(mockCNFTStoreSequence)
//            doNothing().when(mockStore).saveTokenType(tokenType)
//            doNothing().when(mockCNFTStoreSequence).end()
//
//            val result = mockCNFTEngine.registerTokenType(gateId = "gate1", Array(registerTokenType))
//            assertResult(result)(TestResult)
//        }
//
//        "TokenType already exists" in {
//            val TestResult = Left("TokenType already exists")
//            val registerTokenType: RegisterTokenType = RegisterTokenType(Collection.empty, issuerPublicKey = "SBC".getBytes(), issuerId= "issuerId")
//
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val mockCNFTStoreSequence = getMockCNFTStoreSequence
//
//            when(mockStore.getTokenType(tokenType.typeId)).thenReturn(Option(tokenType))
//            when(mockStore.getSequence[TokenType]("TokenType", "gate1"))
//                .thenReturn(mockCNFTStoreSequence)
//            doNothing().when(mockStore).saveTokenType(tokenType)
//            doNothing().when(mockCNFTStoreSequence).end()
//
//            val result = mockCNFTEngine.registerTokenType(gateId = "gate1", Array(registerTokenType))
//
//            assertResult(result)(TestResult)
//        }
//    }
//    "GetTokenType" - {
//        "No such token type id" in {
//            val tokenId = "SBC"
//            val TestResult = Left("No such token type id: " + tokenId)
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            when(mockStore.getTokenType(tokenId)).thenReturn(None)
//            val result = mockCNFTEngine.getTokenType(tokenId)
//            assertResult(result)(TestResult)
//        }
//
//        "Positive case" in {
//            val TestResult = Right(tokenType)
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            when(mockStore.getTokenType("SBC")).thenReturn(Option(tokenType))
//            val result = mockCNFTEngine.getTokenType("SBC")
//            assertResult(result)(TestResult)
//
//        }
//    }
//
//    "ListTokenType" - {
//        "Positive case" in {
//            val arrayTokenType = Array(tokenType)
//            val TestResult = Right(arrayTokenType)
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            when(mockStore.listTokenTypes).thenReturn(arrayTokenType)
//            assertResult(mockCNFTEngine.listTokenTypes)(TestResult)
//        }
//    }
//
//    private val TokenIdsSequenceName = "Token"
//    "reserveTokenIDs / positive case" in {
//        val owner = TokenOwner(require = 1, keys = Collection("key".getBytes()))
//        val TestResult = Array(ReservedId(
//            tokenId = "SBC",
//            owner = owner
//        ))
//        val reserveTokenIDsRequest = ReserveTokenIDsRequest(gateId = "SBC", owner = Collection(owner))
//        val mockCNFTStoreSequence = mock(classOf[CNFTStoreSequence])
//        val mockStore = getMockStore
//        val mockCNFTEngine = getMockCNFTEngine
//        when(mockStore.getSequence[Token](TokenIdsSequenceName, reserveTokenIDsRequest.gateId))
//            .thenReturn(mockCNFTStoreSequence)
//        when(mockCNFTStoreSequence.next)
//            .thenReturn("SBC")
//        doNothing().when(mockStore).saveTokenType(tokenType)
//        doNothing().when(mockCNFTStoreSequence).end()
//        val result = mockCNFTEngine.reserveTokenIDs(reserveTokenIDsRequest)
//        result match {
//            case Right(value) =>
//                assertResult(value.head.tokenId)(TestResult.head.tokenId)
//                assertResult(value.head.owner)(TestResult.head.owner)
//            case _ => fail("reserve id failed")
//
//        }
//    }
//    "GetOwner" - {
//        "getOwner / positive case" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val token = Token(id = reservedId, token = Option(issuedToken))
//            val tokenOwner = TokenOwner(require = 1, keys = Collection(owner.keys.head))
//
//            when(mockStore.getToken("SBC")).thenReturn(Option(token))
//            val result = mockCNFTEngine.getOwner("SBC")
//
//            assert(result.isRight)
//            result.foreach { v =>
//                assert(v.require == tokenOwner.require)
//                assert(v.keys sameElements tokenOwner.keys)
//            }
//        }
//
//        "getOwner / no such token" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val tokenId = "SBC"
//            val testResult = Left(s"No such token [$tokenId]")
//            when(mockStore.getToken("SBC")).thenReturn(None)
//            val result = mockCNFTEngine.getOwner("SBC")
//            assertResult(testResult)(result)
//        }
//    }
//    "IssueTokens" - {
//        "issueTokens / positive case" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val token = Token(id = reservedId, token = None)
//            val testResult = IssueTokenResponse(code = 0, token = Option(Token(id = reservedId, token = Option(issuedToken))))
//
//            when(mockStore.getToken(tokenId))
//                .thenReturn(Option(token))
//            doNothing().when(mockStore).saveTokenType(ArgumentMatchers.any())
//            val result = mockCNFTEngine.issueTokens(Array(issuedTokenRequest))
//            result match {
//                case Right(value) =>
//                    assertResult(value.head.token)(testResult.token)
//                    assertResult(value.head.code)(testResult.code)
//                case _ => fail("issue token failed")
//            }
//        }
//
//        "issueTokens / IssueTokenResponse(code = 2)" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val token = Token(id = reservedId, token = Option(issuedToken))
//
//            when(mockStore.getToken("SBC"))
//                .thenReturn(Option(token))
//            doNothing().when(mockStore).saveTokenType(ArgumentMatchers.any())
//            val result = mockCNFTEngine.issueTokens(Array(issuedTokenRequest))
//            result match {
//                case Right(value) => assertResult(value.head)(IssueTokenResponse(code = 2, None))
//                case _ => fail("issue token failed")
//            }
//        }
//
//        "issueTokens / issueTokenResponse(code = 1)" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//
//            when(mockStore.getToken("SBC"))
//                .thenReturn(None)
//            doNothing().when(mockStore).saveTokenType(ArgumentMatchers.any())
//            val result = mockCNFTEngine.issueTokens(Array(issuedTokenRequest))
//            result match {
//                case Right(value) => assertResult(value.head)(IssueTokenResponse(code = 1, None))
//                case _ => fail("issue token failed")
//            }
//        }
//    }
//    "Get token" - {
//        "getToken / positive case" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val token = Token(id = reservedId, token = Option(issuedToken))
//            when(mockStore.getToken(tokenId)).thenReturn(Option(token))
//            assertResult(mockCNFTEngine.getToken(tokenId))(Right(token))
//        }
//
//        "getToken / no such token" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            when(mockStore.getToken(tokenId)).thenReturn(None)
//            assertResult(mockCNFTEngine.getToken(tokenId))(Left(s"No such token [$tokenId]"))
//        }
//    }
//
//    "MakeDeal" - {
//        "makeDeal / positive case" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val mockCryptography = getMockCryptography
//            val token = Token(id = reservedId, token = Option(issuedToken))
//            val deal = Deal(dealId = "dealId", changes = Map.apply[String, TokenChange](tokenId -> TokenChange(tokenOwner = owner, signedToken = signedToken)))
//            val changeSignature = "changeSignature".getBytes()
//            val dealRequest = DealRequest(deal, signatures = Collection(changeSignature))
//            val keyBytes = owner.keys.head
//            val signatureBytes = changeSignature
//            val dealBytes = dealRequest.deal.toByteArray
//            when(mockStore.getToken(tokenId)).thenReturn(Option(token))
//            when(
//                mockCryptography.verifySignature(key = keyBytes, content = dealBytes, signature = signatureBytes)
//            ).thenReturn(Result.Ok(true))
//
//            assertResult(mockCNFTEngine.makeDeal(dealRequest))(Right(()))
//        }
//
//        "makeDeal / no such token" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val deal = Deal(dealId = "dealId", changes = Map.apply[String, TokenChange]("idOwner" -> TokenChange(tokenOwner = owner, signedToken = signedToken)))
//            val changeSignature = "changeSignature".getBytes()
//            val dealRequest = DealRequest(deal, signatures = Collection(changeSignature))
//            when(mockStore.getToken("idOwner")).thenReturn(None)
//            val result = mockCNFTEngine.makeDeal(dealRequest)
//            assertResult(Left("No such token [idOwner]"))(result)
//        }
//
//        "makeDeal / no signatures to fulfill move of idOwner" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val token = Token(id = reservedId, token = Option(issuedToken))
//            val deal = Deal(dealId = "dealId", changes = Map.apply[String, TokenChange]("idOwner" -> TokenChange(tokenOwner = owner, signedToken = signedToken)))
//            val dealRequest = DealRequest(deal, signatures = Collection.empty)
//            when(mockStore.getToken("idOwner"))
//                .thenReturn(Option(token))
//            val result = mockCNFTEngine.makeDeal(dealRequest)
//            assertResult(result)(Left(s"No signatures to fulfill move of idOwner"))
//        }
//    }
//    "BurnToken" - {
//        "burnToken / should work fine" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val mockCryptography = getMockCryptography
//            val burnTokenRequest = SignedBurnRequest(request = BurnRequest(tokenId = tokenId, extra = Bytes.empty), signatures = Collection("theSignature".getBytes()))
//            val token = Token(id = reservedId, token = Option(issuedToken))
//            val contentBytes = CNFTChallenge.forTokenBurn(tokenId)
//            val keyBytes = owner.keys.head
//            val signatureBytes = burnTokenRequest.signatures.head.toByteArray
//            when(mockStore.getToken(tokenId))
//                .thenReturn(Option(token))
//            when(mockStore.deleteToken(tokenId))
//                .thenReturn(true)
//            when(mockCryptography.verifySignature(key = keyBytes, content = contentBytes, signature = signatureBytes))
//                .thenReturn(Result.Ok(true))
//            assertResult(mockCNFTEngine.burnToken(burnTokenRequest))(Right(()))
//        }
//
//        "burnToken / unable to delete token" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val mockCryptography = getMockCryptography
//            val burnTokenRequest = SignedBurnRequest(request = BurnRequest(tokenId = tokenId, extra = Bytes.empty), signatures = Collection("theSignature".getBytes()))
//            val token = Token(id = reservedId, token = Option(issuedToken))
//            val contentBytes = Util.decodeB64(tokenId, "content") match {
//                case Right(value) => value
//                case _ => fail("decode failed")
//            }
//            val keyBytes = owner.keys.head
//            val signatureBytes = burnTokenRequest.signatures.head.toByteArray
//            when(mockStore.getToken(tokenId))
//                .thenReturn(Option(token))
//            when(mockStore.deleteToken(tokenId))
//                .thenReturn(false)
//            when(mockCryptography.verifySignature(key = keyBytes, content = contentBytes, signature = signatureBytes))
//                .thenReturn(Result.Ok(false))
//            assertResult(mockCNFTEngine.burnToken(burnTokenRequest))(Left(s"Unable to delete token: $tokenId"))
//        }
//
//        "burnToken / invalid Burn Request / signatures are missing" in {
//            val mockCNFTEngine = getMockCNFTEngine
//            val burnTokenRequest = SignedBurnRequest(request = BurnRequest(tokenId = tokenId, extra = Bytes.empty), signatures = Collection.empty)
//            assertResult(mockCNFTEngine.burnToken(burnTokenRequest))(Left(s"Invalid Burn Request: signatures are missing"))
//        }
//
//        "burnToken / invalid Burn Request / tokenId is missing" in {
//            val mockCNFTEngine = getMockCNFTEngine
//            val burnTokenRequest = SignedBurnRequest(request = BurnRequest(tokenId = "", extra = Bytes.empty), Collection("theSignature".getBytes()))
//            assertResult(mockCNFTEngine.burnToken(burnTokenRequest))(Left(s"Invalid Burn Request: tokenId is missing"))
//        }
//
//        "burnToken / Signatures invalid" in {
//            val mockStore = getMockStore
//            val mockCNFTEngine = getMockCNFTEngine
//            val mockCryptography = getMockCryptography
//            val burnTokenRequest = SignedBurnRequest(request = BurnRequest(tokenId = tokenId, extra = Bytes.empty), signatures = Collection("theSignature".getBytes()))
//            val token = Token(id = reservedId, token = Option(issuedToken))
//            val contentBytes = CNFTChallenge.forTokenBurn(tokenId)
//            val keyBytes = owner.keys.head
//            val signatureBytes = burnTokenRequest.signatures.head.toByteArray
//            when(mockStore.getToken(tokenId))
//                .thenReturn(Option(token))
//            when(mockStore.deleteToken(tokenId))
//                .thenReturn(true)
//            when(mockCryptography.verifySignature(key = keyBytes, content = contentBytes, signature = signatureBytes))
//                .thenReturn(Result.Ok(false))
//            assertResult(mockCNFTEngine.burnToken(burnTokenRequest))(Left("Signatures invalid"))
//        }
//    }
//}
