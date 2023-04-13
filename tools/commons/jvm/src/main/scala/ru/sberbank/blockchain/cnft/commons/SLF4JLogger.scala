package ru.sberbank.blockchain.cnft.commons

import org.slf4j

/**
 * @author Maxim Fedin
 */
class SLF4JLogger(
    logger: slf4j.Logger
) extends Logger {

    override def info(msg: String): Unit = logger.info(msg)

    override def infoR[R[+_] : ROps](msg: String): R[Unit] =
        implicitly[ROps[R]].apply {
            info(msg)
        }

    override def debug(msg: String): Unit = logger.debug(msg)

    override def debugR[R[+_] : ROps](msg: String): R[Unit] =
        implicitly[ROps[R]].apply {
            debug(msg)
        }

    override def error(msg: String): Unit = logger.error(msg)

    override def errorR[R[+_] : ROps](msg: String): R[Unit] =
        implicitly[ROps[R]].apply {
            error(msg)
        }

    override def error(msg: String, error: Throwable): Unit = logger.error(msg, error)

    override def trace(msg: String): Unit = logger.trace(msg)

    override def traceR[R[+_] : ROps](msg: String): R[Unit] =
        implicitly[ROps[R]].apply {
            trace(msg)
        }

    override def warn(msg: String): Unit = logger.warn(msg)

    override def warnR[R[+_] : ROps](msg: String): R[Unit] =
        implicitly[ROps[R]].apply {
            warn(msg)
        }
}


object SLF4JLogger {
    def apply(logger: slf4j.Logger): SLF4JLogger = new SLF4JLogger(logger)
}
