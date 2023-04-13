package utility

import ru.sberbank.blockchain.cnft.commons.ROps

import scala.annotation.Annotation
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.macros.blackbox

/**
 * @author Alexey Polubelov
 */

object Utility {
    def forward[SRC, DEST](instance: SRC): DEST = macro MacrosImpl.forward[SRC, DEST]
}

object UtilityFor {
    implicit def utilityFor[T, Result[+_], EncodingType]: UtilityFor[T, Result, EncodingType] = macro MacrosImpl.utilityFor[T, Result, EncodingType]
}

trait UtilityFor[T, Result[+_], EncodingType] extends ImplicitOps[Result, EncodingType] {

    def routes(instance: T): List[Route[EncodingType, Result]]

    def proxy(handler: Handler[EncodingType, Result]): T

}

// =====================================================================================================================
// Routes Utility
// =====================================================================================================================

trait RoutesUtility[T, Result[+_], EncodingType] extends ImplicitOps[Result, EncodingType] {

    def routes(instance: T): List[Route[EncodingType, Result]]

}

object RoutesUtility {

    implicit def routesUtilityFor[T, Result[+_], EncodingType]: RoutesUtility[T, Result, EncodingType] = macro MacrosImpl.routesUtilityFor[T, Result, EncodingType]

}

// =====================================================================================================================
// Proxy Utility
// =====================================================================================================================

trait ProxyUtility[T, Result[+_], EncodingType] extends ImplicitOps[Result, EncodingType] {

    def proxy(handler: Handler[EncodingType, Result]): T

}

object ProxyUtility {
    implicit def proxyUtilityFor[T, Result[+_], EncodingType]: ProxyUtility[T, Result, EncodingType] = macro MacrosImpl.proxyUtilityFor[T, Result, EncodingType]
}

// =====================================================================================================================
//
// =====================================================================================================================

trait Route[EncodingType, Result[+_]] {

    def functionInfo: Info

    def argumentsInfo: Seq[Info]

    def execute(argumentsEncoded: Seq[EncodingType]): Result[EncodingType]
}

abstract case class RouteImpl[EncodingType, Result[+_]](
    functionInfo: Info,
    argumentsInfo: Seq[Info]
) extends Route[EncodingType, Result]

trait ImplicitOps[Result[+_], EncodingType] {

    def map[A, B](v: Result[A], f: A => B)(implicit ops: ROps[Result]): Result[B] = ops.map(v, f)

    def encode[T](value: T)(implicit encoder: Encoder[T, EncodingType]): EncodingType = encoder.encode(value)

    def decode[T](value: EncodingType)(implicit decoder: Decoder[T, EncodingType]): T = decoder.decode(value)

}

case class Info(
    name: String,
    meta: Seq[Annotation],
    aType: String
)

trait Handler[EncodingType, Result[+_]] {
    def handle(functionInfo: Info, argumentsInfo: Seq[Info], argumentsEncoded: Seq[EncodingType]): Result[EncodingType]
}

trait Encoder[T, EncodingType] {
    def encode(value: T): EncodingType
}

trait Decoder[T, EncodingType] {
    def decode(encoded: EncodingType): T
}

// =====================================================================================================================
//
// =====================================================================================================================

object MacrosImpl {

    def forward[SRC, DEST](context: blackbox.Context)(
        instance: context.Expr[SRC]
    )(implicit
        DESTTypeTag: context.WeakTypeTag[DEST],
    ): context.Expr[DEST] = {
        import context.universe._

        val destType = DESTTypeTag.tpe
        val methods = for {
            member <- destType.members.toList
            if member.isAbstract
            if member.isMethod
            if member.isPublic
            if !member.isConstructor
            if !member.isSynthetic
            symbol = member.asMethod
        } yield (symbol, symbol.typeSignatureIn(destType))

        val mDefinitions = methods.map { case (symbol, method) =>
            val methodParametersDefinition = method.paramLists.map(_.map(p => q"${p.name.toTermName}: ${p.typeSignature}"))
            val functionName = symbol.name
            //val methodInnerReturnType = method.finalResultType.typeArgs.head
            q"""
                    override def $functionName(...$methodParametersDefinition): ${method.finalResultType} =
                        $instance.$functionName(...$methodParametersDefinition)

            """
        }

        val result =
            q"""
               new ${destType.finalResultType}{
                            ..$mDefinitions
               }
             """

        //        println(result)
        context.Expr(result)
    }

    def utilityFor[T, Result[+_], EncodingType](context: blackbox.Context)
        (implicit
            TTypeTag: context.WeakTypeTag[T],
            RTypeTag: context.WeakTypeTag[Result[_]],
            EnTypeTag: context.WeakTypeTag[EncodingType],
        ): context.Expr[UtilityFor[T, Result, EncodingType]] = {
        import context.universe._

        val TType = TTypeTag.tpe
        val ResultType = RTypeTag.tpe.typeConstructor
        val EncodingType = EnTypeTag.tpe

        val methods = for {
            member <- TType.members.toList
            if member.isAbstract
            if member.isMethod
            if member.isPublic
            if !member.isConstructor
            if !member.isSynthetic
            symbol = member.asMethod
            method = symbol.typeSignatureIn(TType)
            if method.finalResultType.typeConstructor =:= ResultType
        } yield (symbol, symbol.typeSignatureIn(TType))

        //==============================================================================
        val routes = methods.map { case (symbol, method) =>
            val functionName = symbol.name
            val annotations = symbol.annotations.map { annotation =>
                val t = annotation.tree.tpe
                val args = annotation.tree.children.tail
                q"new $t(..$args)"
            }
            val functionInfo = q"utility.Info(${functionName.toString}, Seq(..$annotations),${method.finalResultType.toString})"
            //TODO: make it "functional way"
            var index = -1
            val argsAndParams =
                method
                    .paramLists
                    .map { group =>
                        group.map { p =>
                            index += 1
                            val annotations = p.annotations.map { annotation =>
                                val t = annotation.tree.tpe
                                val args = annotation.tree.children.tail
                                q"new $t(..$args)"
                            }

                            (
                                q"utility.Info(${p.name.toTermName.toString}, Seq(..$annotations), ${p.typeSignature.toString})",
                                q"decode[${p.typeSignature}](argumentsEncoded($index))"
                            )
                        }
                    }
                    .headOption.map(_.unzip) //TODO: add support for methods with multiple groups
            val methodInnerReturnType = method.finalResultType.typeArgs.head
            // (argumentsInfo, parametersDecoded)
            val argumentsInfo = argsAndParams.map(_._1).getOrElse(List.empty)
            val functionCall = argsAndParams.map(params => q"instance.$functionName(..${params._2})").getOrElse(q"instance.$functionName")
            q"""
                new utility.RouteImpl[$EncodingType, $ResultType](
                    $functionInfo,
                    $argumentsInfo
                ) {
                    override def execute(argumentsEncoded: Seq[$EncodingType]) = {
                        val result = $functionCall
                        map(result, encode[$methodInnerReturnType])
                    }
                }
            """
        }

        //==============================================================================
        val mDefinitions = methods.map { case (symbol, method) =>
            val methodParametersDefinition = method.paramLists.map(_.map(p => q"${p.name.toTermName}: ${p.typeSignature}"))
            val functionName = symbol.name
            val annotations = symbol.annotations.map { annotation =>
                val t = annotation.tree.tpe
                val args = annotation.tree.children.tail
                q"new $t(..$args)"
            }
            val functionInfo = q"utility.Info(${functionName.toString}, Seq(..$annotations),${method.finalResultType.toString})"
            val (argumentsInfo, parametersDecoded) =
                method
                    .paramLists
                    .flatMap { group =>
                        group.map { p =>
                            val annotations = p.annotations.map { annotation =>
                                val t = annotation.tree.tpe
                                val args = annotation.tree.children.tail
                                q"new $t(..$args)"
                            }
                            (
                                q"utility.Info(${p.name.toTermName.toString}, Seq(..$annotations), ${p.typeSignature.toString})",
                                q"encode[${p.typeSignature}](${p.name.toTermName})"
                            )
                        }
                    }.unzip
            val methodAnnotations = symbol.annotations.map { annotation =>
                val t = annotation.tree.tpe
                val args = annotation.tree.children.tail
                q"new $t(..$args)"
            }
            val methodInnerReturnType = method.finalResultType.typeArgs.head
            q"""
                    @..$methodAnnotations
                    override def $functionName(...$methodParametersDefinition): ${method.finalResultType} = {
                        val result = handler.handle($functionInfo, $argumentsInfo, $parametersDecoded)
                        map(result, decode[$methodInnerReturnType])
                    }
                """
        }

        //==============================================================================
        val constructorArgs = TType.members
            .find(_.isConstructor)
            .flatMap(_.asMethod.paramLists.headOption)
            .toList.flatten
            .map { s =>
                s.name.toTermName
            }

        val result =
            q"""new utility.UtilityFor[$TType, $ResultType, $EncodingType]{

                    override def routes(instance: $TType): List[utility.Route[$EncodingType, $ResultType]] = List(..$routes)

                    override def proxy(handler: utility.Handler[$EncodingType, $ResultType]): $TType =
                        new ${TType.finalResultType} (..$constructorArgs){
                            ..$mDefinitions
                        }
                }
             """

        //        println(result)

        context.Expr(result)
    }

    def routesUtilityFor[T, Result[+_], EncodingType](context: blackbox.Context)
        (implicit
            TTypeTag: context.WeakTypeTag[T],
            RTypeTag: context.WeakTypeTag[Result[_]],
            EnTypeTag: context.WeakTypeTag[EncodingType],
        ): context.Expr[RoutesUtility[T, Result, EncodingType]] = {
        import context.universe._

        val TType = TTypeTag.tpe
        val ResultType = RTypeTag.tpe.typeConstructor
        val EncodingType = EnTypeTag.tpe

        val methods = for {
            member <- TType.members.toList
            if member.isAbstract
            if member.isMethod
            if member.isPublic
            if !member.isConstructor
            if !member.isSynthetic
            symbol = member.asMethod
            method = symbol.typeSignatureIn(TType)
            if method.finalResultType.typeConstructor =:= ResultType
        } yield (symbol, symbol.typeSignatureIn(TType))

        //==============================================================================
        val routes = methods.map { case (symbol, method) =>
            val functionName = symbol.name
            val annotations = symbol.annotations.map { annotation =>
                val t = annotation.tree.tpe
                val args = annotation.tree.children.tail
                q"new $t(..$args)"
            }
            val functionInfo = q"utility.Info(${functionName.toString}, Seq(..$annotations),${method.finalResultType.toString})"
            //TODO: make it "functional way"
            var index = -1
            val argsAndParams =
                method
                    .paramLists
                    .map { group =>
                        group.map { p =>
                            index += 1
                            val annotations = p.annotations.map { annotation =>
                                val t = annotation.tree.tpe
                                val args = annotation.tree.children.tail
                                q"new $t(..$args)"
                            }

                            (
                                q"utility.Info(${p.name.toTermName.toString}, Seq(..$annotations), ${p.typeSignature.toString})",
                                q"decode[${p.typeSignature}](argumentsEncoded($index))"
                            )
                        }
                    }
                    .headOption.map(_.unzip) //TODO: add support for methods with multiple groups
            val methodInnerReturnType = method.finalResultType.typeArgs.head
            // (argumentsInfo, parametersDecoded)
            val argumentsInfo = argsAndParams.map(_._1).getOrElse(List.empty)
            val functionCall = argsAndParams.map(params => q"instance.$functionName(..${params._2})").getOrElse(q"instance.$functionName")
            q"""
                new utility.RouteImpl[$EncodingType, $ResultType](
                    $functionInfo,
                    $argumentsInfo
                ) {
                    override def execute(argumentsEncoded: Seq[$EncodingType]) = {
                        val result = $functionCall
                        map(result, encode[$methodInnerReturnType])
                    }
                }
            """
        }

        //==============================================================================

        val result =
            q"""new utility.RoutesUtility[$TType, $ResultType, $EncodingType]{
                    override def routes(instance: $TType): List[utility.Route[$EncodingType, $ResultType]] = List(..$routes)
                }
             """

        //        println(result)

        context.Expr(result)
    }


    def proxyUtilityFor[T, Result[+_], EncodingType](context: blackbox.Context)
        (implicit
            TTypeTag: context.WeakTypeTag[T],
            RTypeTag: context.WeakTypeTag[Result[_]],
            EnTypeTag: context.WeakTypeTag[EncodingType],
        ): context.Expr[ProxyUtility[T, Result, EncodingType]] = {
        import context.universe._

        val TType = TTypeTag.tpe
        val ResultType = RTypeTag.tpe.typeConstructor
        val EncodingType = EnTypeTag.tpe

        val methods = for {
            member <- TType.members.toList
            if member.isAbstract
            if member.isMethod
            if member.isPublic
            if !member.isConstructor
            if !member.isSynthetic
            symbol = member.asMethod
            method = symbol.typeSignatureIn(TType)
            if method.finalResultType.typeConstructor =:= ResultType
        } yield (symbol, symbol.typeSignatureIn(TType))

        //==============================================================================
        val mDefinitions = methods.map { case (symbol, method) =>
            val methodParametersDefinition = method.paramLists.map(_.map(p => q"${p.name.toTermName}: ${p.typeSignature}"))
            val functionName = symbol.name
            val annotations = symbol.annotations.map { annotation =>
                val t = annotation.tree.tpe
                val args = annotation.tree.children.tail
                q"new $t(..$args)"
            }
            val functionInfo = q"utility.Info(${functionName.toString}, Seq(..$annotations),${method.finalResultType.toString})"
            val (argumentsInfo, parametersDecoded) =
                method
                    .paramLists
                    .flatMap { group =>
                        group.map { p =>
                            val annotations = p.annotations.map { annotation =>
                                val t = annotation.tree.tpe
                                val args = annotation.tree.children.tail
                                q"new $t(..$args)"
                            }
                            (
                                q"utility.Info(${p.name.toTermName.toString}, Seq(..$annotations), ${p.typeSignature.toString})",
                                q"encode[${p.typeSignature}](${p.name.toTermName})"
                            )
                        }
                    }.unzip
            val methodAnnotations = symbol.annotations.map { annotation =>
                val t = annotation.tree.tpe
                val args = annotation.tree.children.tail
                q"new $t(..$args)"
            }
            val methodInnerReturnType = method.finalResultType.typeArgs.head
            q"""
                    @..$methodAnnotations
                    override def $functionName(...$methodParametersDefinition): ${method.finalResultType} = {
                        val result = handler.handle($functionInfo, $argumentsInfo, $parametersDecoded)
                        map(result, decode[$methodInnerReturnType])
                    }
                """
        }

        //==============================================================================
        val constructorArgs = TType.members
            .find(_.isConstructor)
            .flatMap(_.asMethod.paramLists.headOption)
            .toList.flatten
            .map { s =>
                s.name.toTermName
            }

        val result =
            q"""new utility.ProxyUtility[$TType, $ResultType, $EncodingType]{
                    override def proxy(handler: utility.Handler[$EncodingType, $ResultType]): $TType =
                        new ${TType.finalResultType} (..$constructorArgs){
                            ..$mDefinitions
                        }
                }
             """

        //        println(result)

        context.Expr(result)
    }
}
