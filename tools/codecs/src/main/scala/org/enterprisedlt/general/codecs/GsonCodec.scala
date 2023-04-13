package org.enterprisedlt.general.codecs

import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

import com.google.gson.{Gson, GsonBuilder}
import org.enterprisedlt.spec.BinaryCodec
import org.slf4j.{Logger, LoggerFactory}

/**
 * @author Alexey Polubelov
 */
case class GsonCodec(
    skipText: Boolean = true,
    gsonOptions: GsonBuilder => GsonBuilder = x => x
) extends BinaryCodec {
    private val logger: Logger = LoggerFactory.getLogger(this.getClass)

    override def encode[T](value: T): Array[Byte] =
        (value match {
            case text: String if skipText =>
                logger.trace(s"Skipped encoding of pure text value '$value'")
                text
            case _ =>
                val result = gson.toJson(value)
                logger.trace(s"Encoded '$value' ==> '$result'")
                result
        }).getBytes(StandardCharsets.UTF_8)


    override def decode[T](value: Array[Byte], clz: Type): T =
        clz match {
            case v if skipText && classOf[String].equals(v) =>
                logger.trace(s"Skipped decoding of pure text value '$value'")
                new String(value, StandardCharsets.UTF_8).asInstanceOf[T]
            case _ =>
                val result = gson.fromJson[T](new String(value, StandardCharsets.UTF_8), clz)
                logger.trace(s"Decoded '$value' ==> '$result'")
                result
        }


    private[this] def gson: Gson = gsonOptions(new GsonBuilder).create
}
