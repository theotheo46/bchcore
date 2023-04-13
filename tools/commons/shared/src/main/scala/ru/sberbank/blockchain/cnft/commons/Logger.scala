package ru.sberbank.blockchain.cnft.commons

import scala.language.higherKinds

/**
 * @author Maxim Fedin
 */
trait Logger {

    def info(msg: String): Unit

    def infoR[R[+_]: ROps](msg:String): R[Unit]

    def debug(msg: String): Unit

    def debugR[R[+_]: ROps](msg:String): R[Unit]

    def error(msg: String): Unit

    def errorR[R[+_]: ROps](msg:String): R[Unit]

    def error(msg: String, error: Throwable): Unit

    def trace(msg: String): Unit

    def traceR[R[+_]: ROps](msg:String): R[Unit]

    def warn(msg: String): Unit

    def warnR[R[+_]: ROps](msg:String): R[Unit]
}
