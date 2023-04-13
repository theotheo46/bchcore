package tools.http.service.annotations

import scala.annotation.StaticAnnotation

/**
 * @author Alexey Polubelov
 */

sealed trait HttpMethodAnnotation

case class HttpGet(url: String) extends StaticAnnotation with HttpMethodAnnotation

case class HttpPost(url: String) extends StaticAnnotation with HttpMethodAnnotation

case class HttpHeaderValue(key: String, value: String) extends StaticAnnotation with HttpMethodAnnotation

case class FromHttpHeader(key: String) extends StaticAnnotation with HttpMethodAnnotation

//TODO: add and implement Put, Delete, Patch, ...

case class Body() extends StaticAnnotation

