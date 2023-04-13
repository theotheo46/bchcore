import com.google.protobuf.CodedOutputStream
import org.scalajs.dom.crypto.{Crypto, SubtleCrypto}
import ru.sberbank.blockchain.cnft.common.types.Result
import ru.sberbank.blockchain.cnft.gate.model.{BlockEvent, TxResult}
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.model.{GeneID, RegulatorOperation}
import ru.sberbank.blockchain.cnft.wallet.spec.{IncomingMessage, ValidatedEvent}
import ru.sberbank.blockchain.cnft.wallet.store.HDPathStore
import scala.scalajs.js.typedarray.ArrayBufferView
import java.io.{File, FileOutputStream, PrintStream}
import scala.reflect.ClassTag
import scala.reflect.runtime.currentMirror
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import ru.sberbank.blockchain.cnft.wallet.CNFTCrypto
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.spec.CNFTWalletSpec
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import ru.sberbank.blockchain.common.cryptography.webcrypto.WebCryptoPBEncryption
import scala.collection.mutable
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.typeTag
import ru.sberbank.blockchain.cnft.wallet.CNFT

import scala.util.{Failure, Success, Try}

object PrintTS extends App {

    if(args.headOption.isEmpty){
        System.err.println("Invalid args - path required")
        System.exit(1)
    }

    val targetFile = args.head
    private val out = new PrintStream(
        new FileOutputStream(new File(targetFile), false)
    )

    private val types = mutable.Map.empty[universe.Type, String]

    private val separationSymbol = ";"

    private def addTypes(types: mutable.Map[universe.Type, String], tpe: universe.Type, pname: String): Unit = {

        val posInnerType = tpe.typeArgs.headOption
        if (tpe.toString == "T" || tpe =:= typeOf[js.Iterable[Short]] || tpe =:= typeOf[ru.sberbank.blockchain.common.cryptography.lib_elliptic.JSEC] || tpe =:= typeOf[ArrayBufferView]) {

        } else {
            if (posInnerType.isEmpty) {
                types += tpe.typeConstructor -> pname
                if (tpe =:= typeOf[scala.scalajs.js.typedarray.ArrayBuffer] || tpe =:= typeOf[ru.sberbank.blockchain.cnft.commons.Bytes] || tpe.toString == "R" || tpe =:= typeOf[String] || tpe =:= typeOf[Boolean] || tpe =:= typeOf[Unit] || tpe =:= typeOf[Long] || tpe =:= typeOf[Int] || tpe =:= typeOf[ru.sberbank.blockchain.cnft.commons.BigInt] || tpe.toString == "T" || tpe =:= typeOf[SubtleCrypto] || tpe =:= typeOf[CodedOutputStream] || tpe =:= typeOf[Uint8Array] || tpe =:= typeOf[ru.sberbank.blockchain.cnft.commons.Result[Nothing]].typeConstructor || tpe =:= typeOf[ru.sberbank.blockchain.cnft.commons.Collection[Nothing]].typeConstructor) {

                } else {
                    val tpeeString = tpe.toString
                    tpe.typeConstructor.decls.filter(_.isConstructor).filterNot(_.isSynthetic).map(_.asMethod).foreach(m => {
                        val params = m.paramLists.headOption.getOrElse(List.empty)
                        params.foreach { p =>
                            val pname = p.name.toString
                            val tpee = p.typeSignature
                            addTypes(types, tpee, pname)
                        }
                    }
                    )
                    if (tpeeString.startsWith("ru.sberbank.blockchain.cnft.wallet.walletmodel") || tpeeString.startsWith("ru.sberbank.blockchain.cnft.model")) print("") else {
                        tpe.decls.filter(_.isMethod).filterNot(_.isConstructor).filterNot(_.isStatic).filterNot(_.isSynthetic).filterNot(_.isPrivate).filterNot(_.isProtected).filterNot(_.isImplicit).map(_.asMethod).foreach { m =>
                            if (m.isCaseAccessor) {
                                print("")
                            } else {
                                val params = m.paramLists.headOption.getOrElse(List.empty)
                                params.foreach { p =>
                                    val tpee = p.typeSignature
                                    val pname = p.name.toString
                                    addTypes(types, tpee, pname)
                                }

                                addTypes(types, m.returnType, pname)
                            }
                        }
                    }
                }
            } else {
                if (tpe.typeConstructor =:= typeOf[ru.sberbank.blockchain.common.cryptography.lib_elliptic.JSEC] || tpe.typeConstructor =:= typeOf[scala.scalajs.js.typedarray.ArrayBuffer] || tpe.typeConstructor =:= typeOf[ru.sberbank.blockchain.cnft.commons.Bytes] || tpe.typeConstructor.toString == "R" || tpe.typeConstructor =:= typeOf[String] || tpe.typeConstructor =:= typeOf[Boolean] || tpe.typeConstructor =:= typeOf[Unit] || tpe.typeConstructor =:= typeOf[Long] || tpe.typeConstructor =:= typeOf[Int] || tpe.typeConstructor =:= typeOf[ru.sberbank.blockchain.cnft.commons.BigInt] || tpe.typeConstructor.toString == "T" || tpe.typeConstructor =:= typeOf[SubtleCrypto] || tpe.typeConstructor =:= typeOf[CodedOutputStream] || tpe.typeConstructor =:= typeOf[Uint8Array] || tpe.typeConstructor =:= typeOf[ru.sberbank.blockchain.cnft.commons.Result[Nothing]].typeConstructor || tpe.typeConstructor =:= typeOf[ru.sberbank.blockchain.cnft.commons.Collection[Nothing]].typeConstructor) {

                } else {
                    tpe.typeConstructor.decls.filter(_.isConstructor).filterNot(_.isSynthetic).map(_.asMethod).foreach(m => {
                        val params = m.paramLists.headOption.getOrElse(List.empty)
                        params.foreach { p =>
                            val pname = p.name.toString
                            val tpee = p.typeSignature
                            addTypes(types, tpee.typeConstructor, pname)
                        }
                    }
                    )
                }
                addTypes(types += tpe.typeConstructor -> pname, posInnerType.get, pname)
            }
        }

    }

    private def printParams(params: List[reflect.runtime.universe.Symbol], types: mutable.Map[universe.Type, String]): Unit = {
        val len = params.size
        var i = 1
        params.foreach { p =>
            val tpe = p.typeSignature
            val pname = p.name.toString
            addTypes(types, tpe, pname)
            if (i == len) {
                out.print(s"${pname}: ${TypeMapper(tpe)}")
            } else {
                out.print(s"${pname}: ${TypeMapper(tpe)}, ")
            }
            i += 1
        }
    }

    def TypeMapper(tpe: universe.Type): String = {
        (tpe.typeConstructor, tpe.typeArgs.headOption) match {
            case (tpee, Some(t)) if tpee =:= typeOf[ru.sberbank.blockchain.cnft.common.types.Collection[Nothing]].typeConstructor => s"${TypeMapper(t)}[]"
            case (tpee, Some(t)) if tpee =:= typeOf[ru.sberbank.blockchain.cnft.commons.Result[Nothing]].typeConstructor => s"Promise<${TypeMapper(t)}>"
            case (tpee, Some(t)) if tpee =:= typeOf[TxResult[Nothing]].typeConstructor => s"TxResult<${TypeMapper(t)}>"
            case (tpee, Some(t)) if tpee =:= typeOf[BlockEvent[Nothing]].typeConstructor => s"BlockEvent<${TypeMapper(t)}>"
            case (tpee, Some(t)) if tpee =:= typeOf[IncomingMessage[Nothing]].typeConstructor => s"IncomingMessage<${TypeMapper(t)}>"
            case (tpe, Some(t)) if tpe.toString == "R" => s"Promise<${TypeMapper(t)}>"
            case (tpee, Some(t)) if tpee =:= typeOf[ValidatedEvent[Nothing]].typeConstructor => s"ValidatedEvent<${TypeMapper(t)}>"
            case (tpee, None) if tpee =:= typeOf[ru.sberbank.blockchain.common.cryptography.lib_elliptic.JSEC] => "any"
            case (tpee, None) if tpee =:= typeOf[ru.sberbank.blockchain.cnft.commons.Bytes] => "ArrayBuffer"
            case (tpee, None) if tpee =:= typeOf[Uint8Array] => "ArrayBuffer"
            case (tpee, None) if tpee =:= typeOf[String] => "string"
            case (tpee, None) if tpee =:= typeOf[Boolean] => "boolean"
            case (tpee, None) if tpee =:= typeOf[Unit] => "void"
            case (tpee, None) if tpee =:= typeOf[Long] => "number"
            case (tpee, None) if tpee =:= typeOf[Int] => "number"
            case (tpee, None) if tpee =:= typeOf[ru.sberbank.blockchain.cnft.commons.BigInt] => "number"
            case (tpee, _) => s"${tpee.toString.substring(tpee.toString.lastIndexOf(".") + 1)}"
        }
    }


    private def printInterfaces(tpe: universe.Type): Unit = {
        {
            (tpe.typeConstructor, tpe.typeArgs.headOption) match {
                case (tpee, _) if tpee =:= typeOf[ru.sberbank.blockchain.cnft.common.types.Collection[Nothing]].typeConstructor =>
                case (tpee, _) if tpee =:= typeOf[ru.sberbank.blockchain.cnft.commons.Result[Nothing]].typeConstructor =>
                case (tpee, _) if tpee =:= typeOf[SubtleCrypto] =>
                case (tpee, _) if tpee =:= typeOf[Crypto] =>
                case (tpee, _) if tpee =:= typeOf[js.Object] =>
                case (tpee, _) if tpee =:= typeOf[ChainServiceSpec[Result]].typeConstructor =>
                case (tpee, _) if tpee =:= typeOf[BlockEvent[Nothing]].typeConstructor =>
                    val genericType = tpee.typeParams.headOption.getOrElse(universe.NoSymbol).name
                    out.println(s"interface ${tpee.toString.substring(tpee.toString.lastIndexOf(".") + 1)}<${genericType}> {")
                    tpee.decls.filter(_.isConstructor).map(_.asMethod).foreach(m => {
                        val params = m.paramLists.headOption.getOrElse(List.empty)
                        params.foreach { p =>
                            val pname = p.name
                            out.println(s"  ${pname}: ${TypeMapper(p.typeSignature)};")
                        }
                    }
                    )
                    out.println("}")
                    out.println()
                case (tpee, _) if tpee =:= typeOf[IncomingMessage[Nothing]].typeConstructor =>
                    val genericType = tpee.typeParams.headOption.getOrElse(universe.NoSymbol).name
                    out.println(s"interface ${tpee.toString.substring(tpee.toString.lastIndexOf(".") + 1)}<${genericType}> {")
                    tpee.decls.filter(_.isConstructor).map(_.asMethod).foreach(m => {
                        val params = m.paramLists.headOption.getOrElse(List.empty)
                        params.foreach { p =>
                            val pname = p.name
                            out.println(s"  ${pname}: ${TypeMapper(p.typeSignature)};")
                        }
                    }
                    )
                    out.println("}")
                    out.println()
                case (tpee, _) if tpee =:= typeOf[TxResult[Nothing]].typeConstructor =>
                    val genericType = tpee.typeParams.headOption.getOrElse(universe.NoSymbol).name
                    out.println(s"interface ${tpee.toString.substring(tpee.toString.lastIndexOf(".") + 1)}<${genericType}> {")
                    tpee.decls.filter(_.isConstructor).map(_.asMethod).foreach(m => {
                        val params = m.paramLists.headOption.getOrElse(List.empty)
                        params.foreach { p =>
                            val pname = p.name
                            out.println(s"  ${pname}: ${TypeMapper(p.typeSignature)};")
                        }
                    }
                    )
                    out.println("}")
                    out.println()
                //ValidatedEvent
                case (tpee, _) if tpee =:= typeOf[ValidatedEvent[Nothing]].typeConstructor =>
                    val genericType = tpee.typeParams.headOption.getOrElse(universe.NoSymbol).name
                    out.println(s"interface ${tpee.toString.substring(tpee.toString.lastIndexOf(".") + 1)}<${genericType}> {")
                    tpee.decls.filter(_.isConstructor).map(_.asMethod).foreach(m => {
                        val params = m.paramLists.headOption.getOrElse(List.empty)
                        params.foreach { p =>
                            val pname = p.name
                            out.println(s"  ${pname}: ${TypeMapper(p.typeSignature)};")
                        }
                    }
                    )
                    out.println("}")
                    out.println()
                case (tpee, _) if tpee.toString == "R" =>
                case (tpee, _) if tpee =:= typeOf[ru.sberbank.blockchain.cnft.commons.Bytes] =>
                case (tpee, _) if tpee =:= typeOf[String] =>
                case (tpee, _) if tpee =:= typeOf[Boolean] =>
                case (tpee, _) if tpee =:= typeOf[Unit] =>
                case (tpee, _) if tpee =:= typeOf[Long] =>
                case (tpee, _) if tpee =:= typeOf[Int] =>
                case (tpee, _) if tpee =:= typeOf[Any] =>
                case (tpee, _) if tpee =:= typeOf[Uint8Array] =>
                case (tpee, _) if tpee =:= typeOf[ru.sberbank.blockchain.cnft.commons.BigInt] =>
                case (tpee, _) => {
                    val tpeeString = tpee.toString
                    if (!tpee.decls.exists(_.isConstructor)) {
                        out.println(s"interface ${tpeeString.substring(tpeeString.lastIndexOf(".") + 1)} {")

                    } else {
                        out.println(s"declare class ${tpeeString.substring(tpeeString.lastIndexOf(".") + 1)} {")

                    }
                    tpee.decls.filter(_.isConstructor).map(_.asMethod).foreach(m => {
                        val params = m.paramLists.headOption.getOrElse(List.empty)
//                        params.foreach { p =>
//                            val tyype = p.typeSignature
//                            val pname = p.name
//                            out.println(s"  ${pname}: ${TypeMapper(tyype)}${separationSymbol}")
//                        }
                        out.print(s"  constructor(")
                        printParams(params, types)
                        out.print(")")
                        out.println()
                    }
                    )
                    if (tpeeString.startsWith("ru.sberbank.blockchain.cnft.wallet.walletmodel") || tpeeString.startsWith("ru.sberbank.blockchain.cnft.model")) out.print("") else {
                        tpee.decls.filter(_.isMethod).filterNot(_.isConstructor).filterNot(_.isStatic).filterNot(_.isSynthetic).filterNot(_.isPrivate).filterNot(_.isProtected).filterNot(_.isImplicit).map(_.asMethod).foreach { m =>
                            if (m.isCaseAccessor) {
                                out.print("")
                            } else {
                                val params = m.paramLists.headOption.getOrElse(List.empty)
                                if (params.isEmpty) {
                                    out.print(s"  ${m.name}: ")
                                    out.print(s"${TypeMapper(m.returnType)}${separationSymbol}")
                                } else {
                                    out.print(s"  ${m.name}: (")
                                    val len = params.size
                                    var i = 1
                                    params.foreach { p =>
                                        val tpe = p.typeSignature
                                        val pname = p.name.toString
                                        if (i == len) {
                                            out.print(s"${pname}: ${TypeMapper(tpe)}")
                                        } else {
                                            out.print(s"${pname}: ${TypeMapper(tpe)}, ")
                                        }
                                        i += 1
                                    }
                                    out.print(s") =>  ${TypeMapper(m.returnType)}${separationSymbol}")
                                }
                                out.println()
                            }
                        }
                    }
                    out.println("}")
                    out.println()
                }
            }
        }
    }


    out.println(
        s"""/* eslint-disable @typescript-eslint/ban-types */
           |declare module '@sbt/wallet-lib' {""".stripMargin)
    printm[WebCryptoPBEncryption]()
    printm[CNFTWalletSpec[Result]]()
    printm[WalletCrypto[Result]]()
    printm[CNFTCrypto.type]()
    printm[HDPathStore.type]()
    printm[ChainServiceSpec[Result]]()
    printm[CNFT.type]()

    printInterfaces()

    printEnum[GeneID.type, GeneID.type](GeneID)
    Try(printEnum[RegulatorOperation.type, RegulatorOperation.type](RegulatorOperation)) match {
        case Failure(exception) => out.println(s"RegulatorOperation not printed errpr:${exception.getMessage}")
        case Success(value) => out.print("")
    }
    out.println("}")

    def printInterfaces(): Unit = {
        types.keySet.foreach { tpe =>
            printInterfaces(tpe)
        }
    }

    def printEnum[T: ClassTag, R: TypeTag](someInstance: T): Unit = {
        val im = currentMirror reflect someInstance
        out.println(s"export enum ${typeTag[R].tpe.typeSymbol.name} {")
        typeTag[R].tpe.decls.filter(_.isMethod).filterNot(_.isSynthetic).map(_.asMethod).filterNot(_.isConstructor).foreach {
            m =>
                (im reflectMethod m.asMethod).apply() match {
                    case s: String => out.println(s"  ${m.name} = '${s}',")
                    case arr: Array[String] => out.println(s"  ${m.name} = [${arr.map(element => s"'${element}'").mkString(", ")}],")
                    case _ => out.println("Not String and Array[String]")
                }
        }
        out.println("}")
        out.println()
    }

    def printm[T: TypeTag](): Unit = {
        out.println(s"declare class ${typeTag.tpe.typeSymbol.name} { ")
        typeTag[T].tpe.decls
            .filter(_.isMethod)
            .filter { m =>
                if (m.isConstructor) {
                    m.asMethod.paramLists.headOption.getOrElse(List.empty).nonEmpty
                } else true
            }
            .filterNot(_.isSynthetic)
            .filterNot(_.isPrivate)
            .filterNot(_.isProtected)
            .filterNot(_.isImplicit)
            .map(_.asMethod)
            .foreach { m =>
                val params = m.paramLists.headOption.getOrElse(List.empty)
                if (m.isConstructor) {
                    out.print(s"  constructor(")
                    printParams(params, types)
                    out.print(")")
                    out.println()
                } else {
                    if (typeTag.tpe.typeSymbol.toString.split(" ").headOption.getOrElse("") == "object")
                        out.print(s"  static ${m.name}: ")
                    else
                        out.print(s"  ${m.name}: ")
                    if (params.nonEmpty) out.print("(")
                    printParams(params, types)
                    if (params.nonEmpty) out.print(") =>")
                    addTypes(types, m.returnType, m.name.toString)
                    out.print(s" ${TypeMapper(m.returnType)}${separationSymbol}")
                    out.println()
                }
            }

        out.print("}")
        out.println()
        out.println()
    }

}