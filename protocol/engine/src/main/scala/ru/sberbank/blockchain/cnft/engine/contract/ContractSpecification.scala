package ru.sberbank.blockchain.cnft.engine.contract

import ru.sberbank.blockchain.cnft.common.types.{Collection, Result}
import ru.sberbank.blockchain.cnft.model.{AcceptedDeal, DataFeed, DataFeedValue, FeedType, FieldMeta, FieldType, RequiredDealFields, SmartContract, SmartContractState, SmartContractTemplate, TokenChangeRequestSmartContract, TokenContent, TokenOwner, TokenType}

import java.math.BigInteger
import java.time.{Instant, OffsetDateTime}
import scala.language.implicitConversions

/**
 * @author Alexey Polubelov
 */

// transaction execution context for Smart Contract
trait SmartContractOperationContext {

    def contract: SmartContract

    //

    def state: SmartContractState

    def updateStateField(index: Int, value: String): Unit

    def stateUpdate: Option[SmartContractState]

    def completeContract(): Unit

    // out world access
    def getTokenOwner(tokenId: String): TokenOwner

    def getTokenContent(tokenId: String): Option[TokenContent]

    def getTokenType(typeId: String): Option[TokenType]

    // TX context

    def nextUniqueId: String

    def timestamp: String

    def getDataFeed(feedId: String): Result[DataFeed]

    def getFeedValue(feedId: String): Result[DataFeedValue]


    // SC tokens

    def dealsAccepted: Collection[AcceptedDeal]

    def currentDeal: Option[AcceptedDeal]

    // SC Operations

    def changeTokens(toChangeId: String, neededVal: Collection[BigInteger]): Result[TokenChangeRequestSmartContract]

    def getTokenValue(tokenId: String): Result[BigInteger]
}

class InitialSmartContractOperationContext(context: SmartContractOperationContext, _state: SmartContractState) extends SmartContractOperationContext {
    override def contract: SmartContract = context.contract

    override def state: SmartContractState = _state

    override def updateStateField(index: Int, value: String): Unit = _state.state.update(index, value)

    override def completeContract(): Unit = context.completeContract()

    override def stateUpdate: Option[SmartContractState] = None // shell never be called

    override def getTokenOwner(tokenId: String): TokenOwner = context.getTokenOwner(tokenId)

    override def getTokenContent(tokenId: String): Option[TokenContent] = context.getTokenContent(tokenId)

    override def getTokenType(typeId: String): Option[TokenType] = context.getTokenType(typeId)

    override def nextUniqueId: String = context.nextUniqueId

    override def timestamp: String = context.timestamp

    override def getDataFeed(id: String): Result[DataFeed] = context.getDataFeed(id)

    override def getFeedValue(feedId: String): Result[DataFeedValue] = context.getFeedValue(feedId)

    override def dealsAccepted: Collection[AcceptedDeal] = context.dealsAccepted

    override def currentDeal: Option[AcceptedDeal] = context.currentDeal

    override def changeTokens(toChangeId: String, neededVal: Collection[BigInteger]): Result[TokenChangeRequestSmartContract] = context.changeTokens(toChangeId, neededVal)

    override def getTokenValue(tokenId: String): Result[BigInteger] = context.getTokenValue(tokenId)

}

trait RO[T] {
    def get(implicit context: SmartContractOperationContext): T
}

trait RW[T] {

    def get(implicit context: SmartContractOperationContext): T

    def set(v: T)(implicit context: SmartContractOperationContext): Unit

    def :=(v: T)(implicit context: SmartContractOperationContext): Unit = set(v)

    def ==(v: T)(implicit context: SmartContractOperationContext): Boolean = get == v
}

case class SCAttribute[T](
    index: Int,
    meta: FieldMeta,
    read: String => T
) extends RO[T] {
    override def get(implicit context: SmartContractOperationContext): T =
        read(context.contract.attributes(index))
}

case class SCStateField[T](
    index: Int,
    meta: FieldMeta,
    initial: T,
    read: String => T,
    write: T => String
) extends RW[T] {
    private[contract] val initialSerialized = write(initial)

    override def get(implicit context: SmartContractOperationContext): T =
        read(context.state.state(index))

    override def set(v: T)(implicit context: SmartContractOperationContext): Unit =
        context.updateStateField(index, write(v))
}


trait SmartContractDSL extends ISmartContract {
    // ---------------------------------------------------------------------------

    private var _fields = Array.empty[SCStateField[_]]

    private def registerField[T](
        id: String, initial: T,
        typeId: String,
        read: String => T, write: T => String
    ): RW[T] = {
        if (_fields.exists(_.meta.id == id)) throw new Exception(s"Field $id is already defined")
        val index = _fields.length
        val field = SCStateField[T](index, FieldMeta(id, typeId), initial, read, write)
        _fields = _fields :+ field
        field
    }

    val field: Fields = new Fields {

        override def Numeric(id: String, initial: BigInteger): RW[BigInteger] =
            registerField(id, initial, FieldType.Numeric, new BigInteger(_), _.toString)

        override def Text(id: String, initial: String): RW[String] =
            registerField(id, initial, FieldType.Text, _.toString, _.toString)

        override def Boolean(id: String, initial: Boolean): RW[Boolean] =
            registerField(id, initial, FieldType.Boolean, _.toBoolean, _.toString)

        override def Object[T <: AnyRef : upickle.default.ReadWriter](id: String, initial: T): RW[T] =
            registerField(
                id, initial, FieldType.Object,
                x => upickle.default.read(x),
                x => upickle.default.write(x)
            )
    }

    // ---------------------------------------------------------------------------

    private var _attributes = Array.empty[SCAttribute[_]]

    private def registerAttribute[T](id: String, typeId: String, read: String => T): RO[T] = {
        if (_attributes.exists(_.meta.id == id)) throw new Exception(s"Attribute $id is already defined")
        val index = _attributes.length
        val field = SCAttribute[T](index, FieldMeta(id, typeId), read)
        _attributes = _attributes :+ field
        field
    }

    val attribute: Attributes = new Attributes {

        override def Numeric(id: String): RO[BigInteger] =
            registerAttribute(id, FieldType.Numeric, _.toNumeric)

        override def Text(id: String): RO[String] =
            registerAttribute(id, FieldType.Text, _.toString)

        override def Boolean(id: String): RO[Boolean] =
            registerAttribute(id, FieldType.Boolean, _.toBoolean)

        override def Date(id: String): RO[Instant] =
            registerAttribute(
                id, FieldType.Date,
                x => OffsetDateTime.parse(x).toInstant
            )

    }

    // ---------------------------------------------------------------------------

    protected def Fail(msg: String) = throw new Exception(msg)

    implicit class ResultDSL[X](r: Result[X]) {
        @inline def orFail: X =
            r match {
                case Right(value) => value
                case Left(msg) => Fail(msg)
            }
    }

    @inline implicit def s2v[T](s: RW[T])(implicit context: SmartContractOperationContext): T = s.get

    //    @inline implicit def s2v[C[_], T](s: RW[C[T]])(implicit context: SCExecutionContext): C[T] = s.get

    @inline implicit def a2v[T](a: RO[T])(implicit context: SmartContractOperationContext): T = a.get

    implicit class InstantOps(v: Instant) {

        @inline def >(o: Instant): Boolean = v.compareTo(o) > 0

        @inline def <(o: Instant): Boolean = v.compareTo(o) < 0

        @inline def <=(o: Instant): Boolean = v.compareTo(o) <= 0

        @inline def >=(o: Instant): Boolean = v.compareTo(o) >= 0

        @inline def ==(o: Instant): Boolean = v.compareTo(o) == 0

    }

    implicit class BigIntegerOps(v: BigInteger) {

        @inline def >(o: BigInteger): Boolean = v.compareTo(o) > 0

        @inline def <(o: BigInteger): Boolean = v.compareTo(o) < 0

        @inline def <=(o: BigInteger): Boolean = v.compareTo(o) <= 0

        @inline def >=(o: BigInteger): Boolean = v.compareTo(o) >= 0

        @inline def ===(o: BigInteger): Boolean = v.compareTo(o) == 0

        @inline def !==(o: BigInteger): Boolean = v.compareTo(o) != 0

        @inline def %(o: BigInteger): BigInteger = v.mod(o)

        @inline def /(o: BigInteger): BigInteger = v.divide(o)

        @inline def +(o: BigInteger): BigInteger = v.add(o)

        @inline def -(o: BigInteger): BigInteger = v.subtract(o)

        @inline def *(o: BigInteger): BigInteger = v.multiply(o)

    }

    implicit class StringOps(v: String) {

        @inline def toNumeric = new BigInteger(v)
    }


    implicit def toNumeric(v: Long): BigInteger = BigInteger.valueOf(v)


    // ---------------------------------------------------------------------------
    protected def templateId: String

    // TODO: create DSL for feeds
    protected def feedsRequire: Collection[FeedType]

    protected def requiredDealFields: Collection[RequiredDealFields]

    override lazy val templateInformation: SmartContractTemplate = {
        SmartContractTemplate(
            id = templateId,
            description = Collection.empty,
            feeds = feedsRequire,
            attributes = _attributes.map(_.meta),
            stateModel = _fields.map(_.meta),
            requiredDealFields = requiredDealFields
        )
    }

    final override def initialize(context: SmartContractOperationContext): Result[SmartContractState] = Result {
        implicit val ctx: SmartContractOperationContext = context
        // ensure all attributes set to correct values:

        _attributes.foreach(_.get)
        // set state fields to initial values:
        val state = context.state
        _fields.foreach { f =>
            state.state.update(f.index, f.initialSerialized)
        }
        state
    }.flatMap { state =>
        init(new InitialSmartContractOperationContext(context, state)).map(_ => state)
    }

    protected def init(implicit context: SmartContractOperationContext): Result[Unit]

}

trait Fields {
    def Numeric(id: String, initial: BigInteger): RW[BigInteger]

    def Text(id: String, initial: String): RW[String]

    def Boolean(id: String, initial: Boolean): RW[Boolean]

    def Object[T <: AnyRef : upickle.default.ReadWriter](id: String, initial: T): RW[T]
}

trait Attributes {
    def Numeric(id: String): RO[BigInteger]

    def Text(id: String): RO[String]

    def Boolean(id: String): RO[Boolean]

    def Date(id: String): RO[Instant]
}