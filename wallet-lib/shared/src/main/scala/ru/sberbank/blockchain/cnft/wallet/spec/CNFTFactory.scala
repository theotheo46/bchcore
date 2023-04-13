package ru.sberbank.blockchain.cnft.wallet.spec

import ru.sberbank.blockchain.cnft.common.types.{Bytes, Collection}
import ru.sberbank.blockchain.cnft.commons.ROps.summonHasOps
import ru.sberbank.blockchain.cnft.commons.{LogAware, Logger, ROps}
import ru.sberbank.blockchain.cnft.gate.service.{ChainServiceSpec, ChainTxServiceSpec, POWServiceSpec}
import ru.sberbank.blockchain.cnft.model.TokenId
import ru.sberbank.blockchain.cnft.wallet.CNFTWalletInternal
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.executor.{AccessDecoys, AuthenticatedRequestsExecutor, HttpHashcashExecutor}
import ru.sberbank.blockchain.common.cryptography.{DecoyProvider, Hashcash}
import tools.http.service.HttpServiceFactory

import scala.language.higherKinds
import scala.scalajs.js.annotation.JSExport

// keep the imports below for correct serialization
import tools.http.service.HttpService._
import ru.sberbank.blockchain.cnft._
import ru.sberbank.blockchain.BytesAsBase64RW


/**
 * @author Alexey Polubelov
 */
abstract class CNFTFactory[R[+_] : ROps](
    url: String,
)(implicit
    val logger: Logger,
    serviceFactory: HttpServiceFactory[R]
) extends ChainServiceSpec[R] with LogAware {

    @JSExport
    def newWallet(crypto: WalletCrypto[R]): R[CNFTWalletSpec[R]] = {
        val chainTx = createChainTx(crypto, url)
        for {
            info <- getCurrentVersionInfo
            wallet =
                new CNFTWalletInternal[R](
                    crypto.identity,
                    crypto,
                    this,
                    chainTx,
                    info.height
                )
            _ <- wallet.listTokens
        }
            yield wallet
    }

    @JSExport
    def connectWallet(url: String): CNFTWalletSpec[R] =
        serviceFactory.newService[WalletRemote](url)

    @JSExport
    def extractTokenId(tokenId: String): R[TokenId] = TokenId.from(tokenId)

    private[this] trait WalletRemote extends CNFTWalletSpec[R] {
        override def chain: ChainServiceSpec[R] = CNFTFactory.this
    }

    private def createChainTx(crypto: WalletCrypto[R], url: String): ChainTxServiceSpec[R] = {
        val default = serviceFactory.defaultExecutor(url)
        val difficultyProvider = serviceFactory.newService[POWServiceSpec[R]](url)
        val secureRandomGenerator = crypto.randomGenerator

        val decoyProvider = new DecoyProvider[R]()

        val accessDecoys = new AccessDecoys[R] {
            override def formDecoy(myPublic: Bytes, decoyLength: Int): R[(Collection[Bytes], Bytes)] = {
                for {
                    allKeys <- listMembers.map(_.map(_.accessPublic))
                    decoy <- decoyProvider.generateDecoy(myPublic, allKeys, decoyLength, secureRandomGenerator)
                } yield decoy
            }
        }

        val authenticated = new AuthenticatedRequestsExecutor[R](default, crypto.accessOperations, accessDecoys)
        val powExecutor =
            new HttpHashcashExecutor[R](authenticated, new Hashcash[R](crypto.hash), difficultyProvider)

        serviceFactory.newService[ChainTxServiceSpec[R]](powExecutor)
    }

}


