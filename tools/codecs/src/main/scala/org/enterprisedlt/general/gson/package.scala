package org.enterprisedlt.general

import com.google.gson.GsonBuilder


/**
 * @author Alexey Polubelov
 */
package object gson {

    implicit class GsonBuilderOptions(builder: GsonBuilder) {
        def encodeTypes(typeFieldName: String = "type", typeNamesResolver: TypeNameResolver = DefaultTypeNameResolver): GsonBuilder =
            builder.registerTypeAdapterFactory(new TypeAwareTypeAdapterFactory(typeFieldName, typeNamesResolver))
    }

}
