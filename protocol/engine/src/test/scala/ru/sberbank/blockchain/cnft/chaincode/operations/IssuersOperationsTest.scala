//package ru.sberbank.blockchain.cnft.chaincode.operations
//
//import com.google.protobuf.ByteString
//import org.enterprisedlt.fabric.contract.{KeyValueAdapter, OperationContextMock, QueryResultsIteratorFromIterator}
//import org.hyperledger
//import org.hyperledger.fabric.protos.ledger.queryresult.KvQueryResult.KV
//import org.hyperledger.fabric.shim.ledger.CompositeKey
//import org.mockito.ArgumentMatchers
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.{doNothing, verify, when}
//import org.scalatest.funsuite.AnyFunSuite
//import org.slf4j.{Logger, LoggerFactory}
//import ru.sberbank.blockchain.cnft.proto.tokens.{TokenType}
//
//class IssuersOperationsTest extends AnyFunSuite {
//    val logger: Logger = LoggerFactory.getLogger(this.getClass)
//    val tokenType: TokenType = TokenType(typeID = "SBC", tokenMeta = "SBC", issuerPublicKey = ByteString.copyFrom("SBC".getBytes()))
//    val tokenTypeInvalid: TokenType = TokenType(typeID = "SBC", tokenMeta = "SBC",issuerPublicKey = "PUBKEY")
//
//    test("registerTokenType  / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("TokenType" , "SBC").toString))
//          .thenReturn(null)
//        doNothing().when(mockChaincode)
//          .putState(
//              ArgumentMatchers.eq(new CompositeKey("TokenType", "SBC").toString),
//              ArgumentMatchers.eq(contractCodecs.ledgerCodec.encode(tokenType))
//          )
//        val res = new AllOperations {}.registerTokenType(tokenType)
//        verify(mockChaincode).getState(new CompositeKey("TokenType", "SBC").toString)
//        verify(mockChaincode).putState(new CompositeKey("TokenType" ,"SBC").toString, contractCodecs.ledgerCodec.encode(tokenType))
//        logger.debug(res.right.get)
//        assertResult("Success")(res.right.get)
//    }
//
//    test("TestRegisterTokenType  / TokenType already exists") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("TokenType" , "SBC").toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(tokenType))
//        val res =  new AllOperations {}.registerTokenType(tokenType)
//        verify(mockChaincode).getState(new CompositeKey("TokenType", "SBC").toString)
//        logger.debug(res.left.get)
//        assertResult("TokenType already exists")(res.left.get)
//    }
//
//    test("ListTokenTypes") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(mockChaincode)
//        val kvBuild = KV.newBuilder()
//          .setKey(new CompositeKey("TokeTypes").toString)
//          .setValue(ByteString.copyFrom(contractCodecs.parametersDecoder.encode(tokenType)))
//          .build()
//        val keyValImpl: hyperledger.fabric.shim.ledger.KeyValue = new KeyValueAdapter(kvBuild)
//        val it = scala.collection.Iterable(keyValImpl)
//        val neqQuery: hyperledger.fabric.shim.ledger.QueryResultsIterator[hyperledger.fabric.shim.ledger.KeyValue] = new QueryResultsIteratorFromIterator(it)
//        when(mockChaincode.getStateByPartialCompositeKey(any[CompositeKey]))
//          .thenReturn(neqQuery)
//        val res = new AllOperations {}.listTokenTypes
//        logger.debug(s"res right: ${res.right.get.toString}")
//        assertResult(Array(tokenType))(res.right.get)
//    }
//
//    test("GetTokenType / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("TokenType" , "SBC").toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(tokenType))
//        val getTokenType = new AllOperations {}
//        val res = getTokenType.getTokenType("SBC")
//        verify(mockChaincode).getState(new CompositeKey("TokenType", "SBC").toString)
//        logger.debug(res.right.get.toString)
//        assertResult(tokenType)(res.right.get)
//    }
//
//    test("GetTokenType / No such token type id") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("TokenType" , "SBC").toString))
//          .thenReturn(null)
//        val getTokenType = new AllOperations {}
//        val res = getTokenType.getTokenType("SBC")
//        verify(mockChaincode).getState(new CompositeKey("TokenType", "SBC").toString)
//        logger.debug(res.left.get)
//        assertResult(s"No such token type id: ${tokenType.typeID}")(res.left.get)
//    }
//
//    test("RegisterTokenType  / Invalid token type structure") {
//        val registerTokenType = new AllOperations {}
//        val res = registerTokenType.registerTokenType(tokenTypeInvalid)
//        logger.debug("Invalid token type structure")
//        assertResult("Invalid token type structure")(res.left.get)
//    }
//}
