//package ru.sberbank.blockchain.cnft.engine
//
//import org.mockito.Mockito.mock
//import ru.sberbank.blockchain.cnft.commons.Result
//import ru.sberbank.blockchain.common.cryptography.Cryptography
//
//object Mocks{
//    val mockStore = mock(classOf[CNFTStore])
//    val mockCryptography = mock(classOf[Cryptography[Result]])
//    def getMockStore= mockStore
//    def getMockCryptography = mockCryptography
//    def getMockCNFTStoreSequence = mock(classOf[CNFTStoreSequence])
//
//    def getMockCNFTEngine = new CNFTEngine {
//        override def store: CNFTStore =  getMockStore
//
//        override def cryptography: Cryptography[Result] = getMockCryptography
//    }
//}
