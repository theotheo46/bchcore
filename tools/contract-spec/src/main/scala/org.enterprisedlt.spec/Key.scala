package org.enterprisedlt.spec

/**
 * @author Alexey Polubelov
 */
case class Key(
    parts: String*
)

object Key {
    private[this] val _empty = Key()

    def empty: Key = _empty

    implicit def StringToKey: String => Key = s => Key(s)
}
