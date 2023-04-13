//package ru.sberbank.blockchain.cnft.chaincode.operations
//
//import com.google.protobuf.ByteString
//import org.enterprisedlt.fabric.contract.OperationContextMock
//import org.mockito.ArgumentMatchers.anyString
//import org.mockito.Mockito.when
//import org.scalatest.funsuite.AnyFunSuite
//import ru.sberbank.blockchain.cnft.proto.tokens.{Message, MessageList, MessageRequest}
//
//class MessagesOperationsTest extends AnyFunSuite {
//
//    test("testPutMessage / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(mockChaincode)
//        val messageTest = Message(
//            content = ByteString.copyFrom(contractCodecs.parametersDecoder.encode("SBC".getBytes))
//        )
//        val messageRequest = MessageRequest(
//            to = "1",
//            from = "2",
//            operationId = "3", registryOperation = Option(messageTest)
//        )
//        when(mockChaincode.getState(anyString()))
//          .thenReturn(null)
//        val res = new AllOperations {}.putMessage(Array.apply(messageRequest))
//        println(res.right.get.toString)
//    }
//
//    test("testGetMessages / should works fine") {
//        val mockChaincode = OperationContextMock.getMockChaincode
//        val contractCodecs =  OperationContextMock.getContractCodecs
//        OperationContextMock.setMockContext(mockChaincode)
//        val messageTest = Message(
//            content = ByteString.copyFrom(contractCodecs.parametersDecoder.encode("SBC".getBytes))
//        )
//        when(mockChaincode.getState(anyString()))
//          .thenReturn(contractCodecs.ledgerCodec.encode(messageTest))
//        val res = new AllOperations {}.getMessages("to", "from")
//        assertResult(MessageList(messageList = Seq.apply(messageTest)))(res.right.get)
//    }
//
//}
