package ru.sberbank.blockchain.cnft.engine

/**
 * @author Alexey Polubelov
 */
trait TransactionContext {
    def txId: String

    def nextUniqueId: String

    def timestamp: String
}
