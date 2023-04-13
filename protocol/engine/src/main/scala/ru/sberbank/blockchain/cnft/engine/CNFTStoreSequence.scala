package ru.sberbank.blockchain.cnft.engine

/**
 * @author Alexey Polubelov
 */
trait CNFTStoreSequence {

    def next: String

    def end(): Unit
}
