package ru.sberbank.blockchain.cnft.commons

import org.slf4j.LoggerFactory

/**
 * @author Maxim Fedin
 * @author Alexey Polubelov
 */
trait LoggingSupport extends LogAware {
    implicit val logger: Logger = SLF4JLogger(LoggerFactory.getLogger(this.getClass))
}
