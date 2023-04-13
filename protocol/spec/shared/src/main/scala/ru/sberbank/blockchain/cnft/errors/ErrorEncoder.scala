package ru.sberbank.blockchain.cnft.errors

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("ErrorEncoder")
object ErrorEncoder {
    def encode(code: String, data: Seq[String]): String =
        if (data.nonEmpty)
        s"""{\"code\":\"$code\",\"data\":[\"${data.mkString("\", \"")}\"]}"""
        else s"""{\"code\":\"$code\"}"""
}
