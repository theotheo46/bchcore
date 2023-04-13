package org.enterprisedlt.general.gson

/**
 * @author Alexey Polubelov
 */
trait TypeNameResolver {
    def resolveTypeByName(name: String): Class[_]

    def resolveNameByType(clazz: Class[_]): String
}
