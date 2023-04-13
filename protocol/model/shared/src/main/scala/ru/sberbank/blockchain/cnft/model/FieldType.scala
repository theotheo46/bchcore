package ru.sberbank.blockchain.cnft.model

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Alexey Polubelov
 */
@JSExportAll
@JSExportTopLevel("FieldType")
object FieldType {
    val Text = "text"
    val Numeric = "numeric"
    val Date = "date"
    val Boolean = "boolean"
    val Object = "object"
}
