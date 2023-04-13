//package ru.sberbank.blockchain.cnft.chaincode.operations
//
//import com.google.protobuf.ByteString
//import org._
//import org.enterprisedlt.fabric.contract.{KeyValueAdapter, OperationContextMock, QueryResultsIteratorFromIterator}
//import org.hyperledger.fabric.protos.ledger.queryresult.KvQueryResult._
//import org.hyperledger.fabric.shim.ledger.CompositeKey
//import org.mockito.ArgumentMatchers
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.{doNothing, verify, when}
//import org.scalatest.funsuite.AnyFunSuite
//import org.slf4j.{Logger, LoggerFactory}
//import ru.sberbank.blockchain.cnft.proto.tokens._
//
//class ExchangeOperationsTest extends  AnyFunSuite   {
//    val logger: Logger = LoggerFactory.getLogger(this.getClass)
//    private val putOfferRequest = PutOfferRequest(
//        tokenId = "1",
//        tokenType = "SBC",
//        tokenContent = "content",
//        price = 13,
//        sellerPubKey = "selPubKey",
//    )
//
//    private val offerExampleExceptDealRequest = Offer(
//        tokenId = putOfferRequest.tokenId,
//        tokenType = putOfferRequest.tokenType,
//        tokenContent = putOfferRequest.tokenContent,
//        price = putOfferRequest.price,
//        sellerPubKey = putOfferRequest.sellerPubKey,
//        offerId = putOfferRequest.offerId
//    )
//
//    private val offerExampleExceptDealRequestInvalidStruct = Offer(
//        tokenId = putOfferRequest.tokenId,
//        tokenType = putOfferRequest.tokenType,
////        tokenContent = putOfferRequest.tokenContent,
//        price = putOfferRequest.price,
//        sellerPubKey = putOfferRequest.sellerPubKey,
//        offerId = putOfferRequest.offerId
//    )
//    private val dealRequest = DealRequest(
//        deal = Option(Deal(dealId = ByteString.copyFrom("dealId".getBytes()), changes = collection.immutable.Map[String, Owner]())),
//    )
//    private val acceptOfferRequest = AcceptOfferRequest(
//        offerId = "offerId", dealRequest = Option(dealRequest)
//    )
//
//    private val finalizeOfferRequest = FinalizeOfferRequest(
//        offerId = "offerId", dealRequest = Option(dealRequest)
//    )
//
//    private val offerExampleWithDealRequest = Offer(
//        tokenId = putOfferRequest.tokenId,
//        tokenType = putOfferRequest.tokenType,
//        tokenContent = putOfferRequest.tokenContent,
//        price = putOfferRequest.price,
//        sellerPubKey = putOfferRequest.sellerPubKey,
//        offerId = putOfferRequest.offerId,
//        dealRequest = acceptOfferRequest.dealRequest
//    )
//
//
//    test("PutOffer / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(mockChaincode)
//        val tokenType = TokenType(typeID = "SBC", tokenMeta = "SBC", issuerPublicKey = ByteString.copyFrom("SBC".getBytes()))
//        val exOper = new AllOperations {}
//        when(mockChaincode.getState(new CompositeKey("TokenType", "SBC").toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(tokenType))
//        doNothing().when(mockChaincode)
//          .putState(ArgumentMatchers.eq(new CompositeKey("TokenType", "SBC").toString), any[Array[Byte]])
//        val res = exOper.putOffer(putOfferRequest)
//        logger.debug(res.right.get.toString)
//        assert(res.isRight)
//        assertResult(offerExampleExceptDealRequest)(res.right.get)
//        verify(mockChaincode).getState(new CompositeKey("TokenType", "SBC").toString)
//        verify(mockChaincode)
//          .putState(
//              ArgumentMatchers.eq(new CompositeKey("Offer", "offerId").toString),
//              ArgumentMatchers.eq(contractCodecs.ledgerCodec.encode(offerExampleExceptDealRequest))
//          )
//    }
//
//    test("listOffers / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(mockChaincode)
//        val kvBuild = KV.newBuilder()
//          .setKey(new CompositeKey("Offer").toString)
//          .setValue(ByteString.copyFrom(contractCodecs.parametersDecoder.encode(offerExampleWithDealRequest)))
//          .build()
//        val keyValImpl: hyperledger.fabric.shim.ledger.KeyValue = new KeyValueAdapter(kvBuild)
//        val it = scala.collection.Iterable(keyValImpl)
//        val neqQuery: hyperledger.fabric.shim.ledger.QueryResultsIterator[hyperledger.fabric.shim.ledger.KeyValue] = new QueryResultsIteratorFromIterator(it)
//        when(mockChaincode.getStateByPartialCompositeKey(any[CompositeKey]))
//          .thenReturn(neqQuery)
//        val res = new AllOperations {}.listOffers
//        logger.debug(s"res right: ${res.right.get.toString}")
//    }
//    test("acceptOffer / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        val acceptOffer = new AllOperations {}
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("Offer", "offerId").toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(offerExampleExceptDealRequest))
//        doNothing().when(mockChaincode)
//          .putState(
//              ArgumentMatchers.eq(new CompositeKey("Offer", "offerId").toString),
//              ArgumentMatchers.eq(contractCodecs.ledgerCodec.encode(offerExampleWithDealRequest))
//          )
//        val res =  acceptOffer.acceptOffer(acceptOfferRequest)
//        logger.debug(res.right.get.toString)
//        verify(mockChaincode).getState(new CompositeKey("Offer", "offerId").toString)
//        verify(mockChaincode)
//          .putState(
//            ArgumentMatchers.eq(new CompositeKey("Offer", "offerId").toString),
//            ArgumentMatchers.eq(contractCodecs.ledgerCodec.encode(offerExampleWithDealRequest))
//          )
//        assertResult(offerExampleExceptDealRequest)(res.right.get)
//    }
//
//    test("acceptOffer / Invalid token type structure ") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        val acceptOffer = new AllOperations {}
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("Offer", "offerId").toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(offerExampleExceptDealRequestInvalidStruct))
//        val res =  acceptOffer.acceptOffer(acceptOfferRequest)
//        logger.debug(res.left.get)
//        verify(mockChaincode).getState(new CompositeKey("Offer", "offerId").toString)
//        assertResult("Invalid token type structure")(res.left.get)
//    }
//
//    test("finalizeOffer / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        val finalizeOffer = new AllOperations {}
//        OperationContextMock.setMockContext(mockChaincode)
//        when(mockChaincode.getState(new CompositeKey("Offer", "offerId").toString))
//          .thenReturn(contractCodecs.ledgerCodec.encode(offerExampleWithDealRequest))
//        doNothing().when(mockChaincode).delState(ArgumentMatchers.eq(new CompositeKey("Nothing$", "offerId").toString))
//        val res = finalizeOffer.finalizeOffer(finalizeOfferRequest)
//        logger.debug(res.right.get.toString)
//        verify(mockChaincode).getState(new CompositeKey("Offer", "offerId").toString)
//        verify(mockChaincode).delState(new CompositeKey( "Nothing$","offerId").toString)
//        assertResult(PublishDealResponse(dealId = "dealId", processed = true))(res.right.get)
//    }
//}
