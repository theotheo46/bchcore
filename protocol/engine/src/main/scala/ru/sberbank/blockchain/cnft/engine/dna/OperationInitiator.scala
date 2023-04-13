package ru.sberbank.blockchain.cnft.engine.dna


/**
 * @author Alexey Polubelov
 */
sealed trait OperationInitiator

object OperationInitiator {
    case object Client extends OperationInitiator

    case class SmartContract(id: String) extends OperationInitiator
}