package ru.sberbank.blockchain.cnft.gate.service

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
trait CNFTGate[R[_]] extends ChainServiceSpec[R] with ChainTxServiceSpec[R] with POWServiceSpec[R]