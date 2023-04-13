package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.commons.{LoggingSupport, Result}
import ru.sberbank.blockchain.cnft.wallet.spec._
import tools.http.service.HttpService._

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
// keep the import below for correct serialization
import ru.sberbank.blockchain.cnft._

/**
 * @author Alexey Polubelov
 */
@JSExportAll
@JSExportTopLevel("CNFT")
object CNFT extends ChainFactory[Result] with LoggingSupport {

    override def connect(url: String): CNFTFactory[Result] = createService[CNFTFactory[Result]](url)

}