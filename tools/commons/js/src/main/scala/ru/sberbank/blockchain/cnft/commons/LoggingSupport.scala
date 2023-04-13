package ru.sberbank.blockchain.cnft.commons

/**
 * @author Maxim Fedin
 */
trait LoggingSupport extends LogAware {
    implicit val logger: Logger = new ConsoleLogger
}
