package ru.sberbank.blockchain.cnft.wallet.spec

import ru.sberbank.blockchain.cnft.model.MemberInformation
import upickle.default

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Alexey Polubelov
 */
@JSExportTopLevel("IncomingMessage")
@JSExportAll
case class IncomingMessage[T](
    from: MemberInformation,
    message: T
)

object IncomingMessage {
    implicit def IncomingMessageRW[T: default.ReadWriter]: default.ReadWriter[IncomingMessage[T]] = upickle.default.macroRW
}