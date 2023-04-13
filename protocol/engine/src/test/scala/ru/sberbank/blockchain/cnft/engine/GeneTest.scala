package ru.sberbank.blockchain.cnft.engine

import java.util.Base64
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.funsuite.AnyFunSuite
import ru.sberbank.blockchain.cnft.engine.dna.GeneExecutionContextImpl
import ru.sberbank.blockchain.cnft.engine.dna.burn.{BurnLockedAfter, BurnLockedBefore}
import ru.sberbank.blockchain.cnft.engine.dna.transfer.{TransferLockedAfter, TransferLockedBefore}
import ru.sberbank.blockchain.cnft.model.DataFeedValue

class GeneTest extends AnyFunSuite {


    test("burn before") {

        val current = "2020-12-03T10:15:30+01:00"
        val pastThreshold = "2020-12-01T10:15:30+01:00"
        val futureThreshold = "2021-12-01T10:15:30+01:00"

        val storeMock = Mockito.mock(classOf[CNFTStore])
        val feedName = "timeFeed"

        val feedAddressAsB64String = new String(Base64.getEncoder.encode(feedName.getBytes()))

        def context(threshold: String) = GeneExecutionContextImpl(storeMock, null, null, null, Array(feedAddressAsB64String, threshold))

        when(storeMock.getDataFeedValue(feedName.getBytes())).thenReturn(Some(DataFeedValue(feedName.getBytes(), Array(current))))

        //Burn Locked Before some threshold in the past , so we can burn AFTER

        for {
            canBurnAfterThreshold <- BurnLockedBefore.canBurn(context(pastThreshold), null, null)
        } yield assert(canBurnAfterThreshold)

        //Burn Locked Before some threshold in the future , so we can NOT burn AFTER

        for {
            canBurnAfterThreshold <- BurnLockedBefore.canBurn(context(futureThreshold), null, null)
        } yield assert(!canBurnAfterThreshold)

        //Transfer Locked Before some threshold in the past, so we can transfer AFTER
        for {
            canTransferAfterThreshold <- TransferLockedBefore.canTransfer(context(pastThreshold), null, null)
        } yield assert(canTransferAfterThreshold)

        //Transfer Locked Before some threshold in the future, so we can NOT transfer AFTER
        for {
            canTransferAfterThreshold <- TransferLockedBefore.canTransfer(context(pastThreshold), null, null)
        } yield assert(canTransferAfterThreshold)


        //Burn Locked After some threshold in the past, so we can NOT transfer AFTER
        for {
            canBurnAfterThreshold <- BurnLockedAfter.canBurn(context(pastThreshold), null, null)
        } yield assert(!canBurnAfterThreshold)

        //Burn Locked After some threshold in the future, so we can transfer AFTER
        for {
            canBurnAfterThreshold <- BurnLockedAfter.canBurn(context(futureThreshold), null, null)
        } yield assert(canBurnAfterThreshold)

        //Transfer Locked After some threshold in the past, so we can NOT transfer AFTER
        for {
            canTransferAfterThreshold <- TransferLockedAfter.canTransfer(context(pastThreshold), null, null)
        } yield assert(!canTransferAfterThreshold)

        //Transfer Locked After some threshold in the past, so we can transfer AFTER
        for {
            canTransferAfterThreshold <- TransferLockedAfter.canTransfer(context(futureThreshold), null, null)
        } yield assert(canTransferAfterThreshold)

    }


}
