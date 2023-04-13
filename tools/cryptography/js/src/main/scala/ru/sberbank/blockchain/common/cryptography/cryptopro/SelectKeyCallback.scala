package ru.sberbank.blockchain.common.cryptography.cryptopro

import ru.sberbank.blockchain.cnft.commons.{Collection, Result}

import scala.scalajs.js

/**
 * @author Alexey Polubelov
 */
@js.native
trait SelectKeyCallback extends js.Object {
    def selectKey(certificates: Collection[Collection[String]]): Result[String] = js.native
}
