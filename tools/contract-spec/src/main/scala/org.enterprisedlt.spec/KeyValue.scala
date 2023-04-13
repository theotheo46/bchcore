package org.enterprisedlt.spec

/**
 * @author Alexey Polubelov
 */
case class KeyValue[+T](
    key: String, //TODO: Key
    value: T
)
