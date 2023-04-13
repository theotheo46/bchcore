package ru.sberbank.blockchain.cnft.commons

import org.scalajs.dom.console

/**
 * @author Maxim Fedin
 */
class ConsoleLogger extends Logger {

    override def info(msg: String): Unit = console.info(msg)

    override def infoR[R[+_] : ROps](msg: String): R[Unit] =
        implicitly[ROps[R]].apply {
            info(msg)
        }

    override def debug(msg: String): Unit = console.debug(msg)

    override def error(msg: String): Unit = console.error(msg)

    override def errorR[R[+_] : ROps](msg: String): R[Unit] =
        implicitly[ROps[R]].apply {
            error(msg)
        }

    override def error(msg: String, error: Throwable): Unit = console.error(msg, error)

    override def trace(msg: String): Unit = console.debug(msg)

    override def traceR[R[+_] : ROps](msg: String): R[Unit] =
        implicitly[ROps[R]].apply {
            trace(msg)
        }

    override def warn(msg: String): Unit = console.warn(msg)

    override def warnR[R[+_] : ROps](msg: String): R[Unit] =
        implicitly[ROps[R]].apply {
            warn(msg)
        }

    override def debugR[R[+_] : ROps](msg: String): R[Unit] =
        implicitly[ROps[R]].apply {
            debug(msg)
        }


}
