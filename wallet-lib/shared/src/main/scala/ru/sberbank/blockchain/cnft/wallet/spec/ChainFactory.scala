package ru.sberbank.blockchain.cnft.wallet.spec

import ru.sberbank.blockchain.cnft.commons.LogAware

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
trait ChainFactory[R[+_]] extends LogAware{

    def connect(url: String): CNFTFactory[R]

}