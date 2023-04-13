package org.enterprisedlt.spec

import scala.annotation.StaticAnnotation

/**
 * @author Alexey Polubelov
 */

case class ContractOperation(value: OperationType) extends StaticAnnotation

case class ContractInit() extends StaticAnnotation

case class Restrict(value: Array[String]) extends StaticAnnotation

case class Transient() extends StaticAnnotation