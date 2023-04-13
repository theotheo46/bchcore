package ru.sberbank.blockchain.cnft.model

import ru.sberbank.blockchain.cnft.commons.ROps

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
@scala.scalajs.js.annotation.JSExportAll
@scala.scalajs.js.annotation.JSExportTopLevel("TokenId")
case class TokenId(
    typeId: String,
    instanceId: String
)

object TokenId {

    import ROps._

    private val prefixLength = 2
    def from[R[+_]](id: String)(implicit R: ROps[R]): R[TokenId] = {

        for {
            size <- R{Integer.parseInt(id.take(prefixLength), 16)}
            tokenTypeId <- R{id.slice(prefixLength, size + prefixLength)}
            tokenInstanceId <- R{id.drop(size + prefixLength)}
        } yield
            TokenId(
                typeId = tokenTypeId,
                instanceId = tokenInstanceId
            )
    }

    def encode[R[+_]](typeId: String, instanceId: String)(implicit R: ROps[R]): R[String] = R{
            s"0${typeId.length.toHexString}".takeRight(prefixLength) + typeId + instanceId
    }

}
