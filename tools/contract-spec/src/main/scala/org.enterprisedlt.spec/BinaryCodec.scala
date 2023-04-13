package org.enterprisedlt.spec

import java.lang.reflect.Type

/**
 * @author Alexey Polubelov
 */
trait BinaryCodec {

    def encode[T](value: T): Array[Byte]

    def decode[T](value: Array[Byte], clz: Type): T
}
