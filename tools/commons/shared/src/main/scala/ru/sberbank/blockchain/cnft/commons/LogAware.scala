package ru.sberbank.blockchain.cnft.commons

/**
 * @author Maxim Fedin
 */
trait LogAware {

    implicit def logger: Logger
}
