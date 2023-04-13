package utility

import org.mockito.Mockito.{verify, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import ru.sberbank.blockchain.cnft.commons.{ROps, Result}
import upickle.default.{macroRW, ReadWriter => RW}

import scala.annotation.StaticAnnotation
import scala.language.higherKinds


/**
 * @author Alexey Polubelov
 */
class UtilityTest extends AnyFreeSpec with EitherValues {

    trait ForwardTo {
        def methodToForward(inputI: Int, inputS: String, inputD: Person): String
    }

    object ForwardFrom {
        def methodToForward(inputI: Int, inputS: String, inputD: Person): String =
            s"Got: $inputI, $inputS, $inputD"
    }

    trait TestApi[F[_]] extends Subtract[F] with Sum[F] with Methods[F]

    trait Subtract[F[_]] {

        @HttpGet("/sum-int")
        @Dummy
        def sumInt(a: Int, @QueryParameter b: Int): F[Int]

        @HttpGet("/sum-short")
        def sumShort(a: Short, @QueryParameter b: Short): F[Short]

        @HttpGet("/sum-long")
        def sumLong(a: Long, @QueryParameter b: Long): F[Long]

        @HttpGet("/sum-double")
        def sumDouble(a: Double, @QueryParameter b: Double): F[Double]

        @HttpGet("/sum-float")
        def sumFloat(a: Float, @QueryParameter b: Float): F[Float]
    }

    trait Sum[F[_]] {

        @HttpGet("/subtract-int")
        def subtractInt(@QueryParameter a: Int, b: Int): F[Int]

        @HttpGet("/subtract-short")
        def subtractShort(@QueryParameter a: Short, b: Short): F[Short]

        @HttpGet("/subtract-long")
        def subtractLong(@QueryParameter a: Long, b: Long): F[Long]

        @HttpGet("/subtract-double")
        def subtractDouble(@QueryParameter a: Double, b: Double): F[Double]

        @HttpGet("/subtract-float")
        def subtractFloat(@QueryParameter a: Float, b: Float): F[Float]
    }

    trait Methods[F[_]] {

        @HttpGet("/greet")
        def greet(name: String): F[String]

        @HttpGet("/put-string")
        def putString(value: String): F[Unit]

        @HttpGet("/put-person")
        def putPerson(person: Person): F[String]

        @HttpGet("/get-person")
        def getPerson(value: String): F[Person]

        @HttpGet("/get-person-option")
        def getPersonOption(value: String): F[Option[Person]]

        @HttpGet("/test-boolean")
        def testBoolean(value: Boolean): F[Boolean]

        @HttpGet("/list-person")
        def listPerson: F[Array[Person]]

    }

    class TestApiImpl[R[+_]](implicit R: ROps[R]) extends TestApi[R] {

        override def subtractInt(a: Int, b: Int): R[Int] = R {
            a - b
        }

        override def subtractShort(a: Short, b: Short): R[Short] = R((a - b).toShort)

        override def subtractLong(a: Long, b: Long): R[Long] = R(a - b)

        override def subtractDouble(a: Double, b: Double): R[Double] = R(a - b)

        override def subtractFloat(a: Float, b: Float): R[Float] = R(a - b)

        override def sumInt(a: Int, b: Int): R[Int] = R(a + b)

        override def sumShort(a: Short, b: Short): R[Short] = R((a + b).toShort)

        override def sumLong(a: Long, b: Long): R[Long] = R(a + b)

        override def sumDouble(a: Double, b: Double): R[Double] = R(a + b)

        override def sumFloat(a: Float, b: Float): R[Float] = R(a + b)

        override def greet(name: String): R[String] = R(s"Greetings, $name!")

        override def testBoolean(value: Boolean): R[Boolean] = R(value)

        override def putString(value: String): R[Unit] = R(())

        override def putPerson(person: Person): R[String] = R("Success")

        override def getPerson(value: String): R[Person] = R(Person("John Doe", 30))

        override def getPersonOption(value: String): R[Option[Person]] = R(Option(Person("John Doe", 30)))

        override def listPerson: R[Array[Person]] = R(Array(Person("John Doe", 30), Person("Joanna Doe", 35)))
    }

    def mkAPI[T](handler: Handler[String, Result])(implicit Utility: UtilityFor[T, Result, String]): T =
        Utility.proxy(handler)

    def mkRoutes[T](instance: T)(implicit Utility: UtilityFor[T, Result, String]): Seq[Route[String, Result]] =
        Utility.routes(instance)

    "initial test for" - {

        "routing and proxy" in {
            import UtilityFor._
            import UpickleEncoding._
            import UpickleStringImplicits._
            import UpickleUnitImplicits._

            val routes: Seq[Route[String, Result]] = mkRoutes[TestApi[Result]](new TestApiImpl)
            val client: TestApi[Result] =
                mkAPI[TestApi[Result]](
                    (functionInfo: Info, argumentsInfo: Seq[Info], argumentsEncoded: Seq[String]) => {
                        //                        val args = argumentsInfo.zip(argumentsEncoded)
                        routes
                            .find(r => r.functionInfo == functionInfo)
                            .toRight(s"No such route: $functionInfo")
                            .map(_.execute(argumentsEncoded))
                        match {
                            case Left(msg) => Result.Fail(msg)
                            case Right(result) => result
                        }
                    }
                )
            val rA: Result[Int] = client.sumInt(1, 2)
            assert(rA.isRight)
            assert(rA.contains(3))

            val rB = client.subtractInt(1, 2)
            assert(rB.isRight)
            assert(rB.contains(-1))
        }

        "methods forwarding" in {
            val originalValue = ForwardFrom.methodToForward(123, "abc", Person("vasya", 100))

            val forwarded: ForwardTo = Utility.forward[ForwardFrom.type, ForwardTo](ForwardFrom)

            val forwardedValue = forwarded.methodToForward(123, "abc", Person("vasya", 100))
            //            println(forwardedValue)
            assert(forwardedValue == originalValue)
        }
    }


    "routing" - {
        import UtilityFor._
        import UpickleEncoding._
        import UpickleStringImplicits._
        import UpickleUnitImplicits._

        val routes = mkRoutes[TestApi[Result]](new TestApiImpl)
        val testApi = Mockito.mock(classOf[TestApi[Result]])


        "has expected number of routes" in {
            val expectedNumberOfRoutes = 17
            // Check number of routes are same as declared
            assert(routes.length == expectedNumberOfRoutes)
        }

        "works as expected with Double input and Double output" in {
            val a: Double = 3.0
            val b: Double = 2.0

            when(testApi.sumDouble(a, b)).thenReturn(Result.Ok(5.0))
            val expectedSumResult = testApi.sumDouble(a, b)
            routes.find(_.functionInfo.name == "sumDouble")
                .map { route =>
                    route.execute(List(a, b).map(_.toString)).map(_.toDouble) shouldBe expectedSumResult
                }
            verify(testApi).sumDouble(a, b)


            when(testApi.subtractDouble(a, b)).thenReturn(Result.Ok(1.0))
            val expectedSubtractResult = testApi.subtractDouble(a, b)
            routes.find(_.functionInfo.name == "subtractDouble")
                .map { route =>
                    route.execute(List(a, b).map(_.toString)).map(_.toDouble) shouldBe expectedSubtractResult
                }
        }

        "works as expected with Float input and Float output" in {
            val a: Float = 3.toFloat
            val b: Float = 2.toFloat

            when(testApi.sumFloat(a, b)).thenReturn(Result.Ok(5.floatValue()))
            val expectedSumResult = testApi.sumFloat(a, b)
            routes.find(_.functionInfo.name == "sumFloat")
                .map { route =>
                    route.execute(List(a, b).map(_.toString)).map(_.toFloat) shouldBe expectedSumResult
                }
            verify(testApi).sumFloat(a, b)


            when(testApi.subtractFloat(a, b)).thenReturn(Result.Ok(1.floatValue()))
            val expectedSubtractResult = testApi.subtractFloat(a, b)
            routes.find(_.functionInfo.name == "subtractFloat")
                .map { route =>
                    route.execute(List(a, b).map(_.toString)).map(_.toFloat) shouldBe expectedSubtractResult
                }
        }


        "works as expected with Short input and Short output" in {
            val a: Short = 3
            val b: Short = 2

            when(testApi.sumShort(a, b)).thenReturn(Result.Ok(5.shortValue))
            val expectedSumResult = testApi.sumShort(a, b)
            routes.find(_.functionInfo.name == "sumShort")
                .map { route =>
                    route.execute(List(a, b).map(_.toString)).map(_.toShort) shouldBe expectedSumResult
                }
            verify(testApi).sumShort(a, b)


            when(testApi.subtractShort(a, b)).thenReturn(Result.Ok(1.shortValue))
            val expectedSubtractResult = testApi.subtractShort(a, b)
            routes.find(_.functionInfo.name == "subtractShort")
                .map { route =>
                    route.execute(List(a, b).map(_.toString)).map(_.toShort) shouldBe expectedSubtractResult
                }
        }


        "works as expected with Long input and Long output" in {
            val a: Long = 3
            val b: Long = 2

            when(testApi.sumLong(a, b)).thenReturn(Result.Ok(5L))
            val expectedSumResult = testApi.sumLong(a, b)
            routes.find(_.functionInfo.name == "sumLong")
                .map { route =>
                    route.execute(List(a, b).map(_.toString)).map(_.toLong) shouldBe expectedSumResult
                }
            verify(testApi).sumLong(a, b)


            when(testApi.subtractLong(a, b)).thenReturn(Result.Ok(1L))
            val expectedSubtractResult = testApi.subtractLong(a, b)
            routes.find(_.functionInfo.name == "subtractLong")
                .map { route =>
                    route.execute(List(a, b).map(_.toString)).map(_.toLong) shouldBe expectedSubtractResult
                }
        }


        "works as expected with Int input and Int output" in {
            val a = 3
            val b = 2

            when(testApi.sumInt(a, b)).thenReturn(Result.Ok(5))
            val expectedSumResult = testApi.sumInt(a, b)
            routes.find(_.functionInfo.name == "sumInt")
                .map { route =>
                    route.execute(List(a, b).map(_.toString)).map(_.toInt) shouldBe expectedSumResult
                }
            verify(testApi).sumInt(a, b)


            when(testApi.subtractInt(a, b)).thenReturn(Result.Ok(1))
            val expectedSubtractResult = testApi.subtractInt(a, b)
            routes.find(_.functionInfo.name == "subtractInt")
                .map { route =>
                    route.execute(List(a, b).map(_.toString)).map(_.toInt) shouldBe expectedSubtractResult
                }
        }

        "works as expected with String input and String output" in {
            val name = "John Doe"
            when(testApi.greet(name)).thenReturn(Result.Ok("Greetings, John Doe!"))
            val expectedGreetResult = testApi.greet(name)
            routes.find(_.functionInfo.name == "greet")
                .map { route =>
                    route.execute(List(name)) shouldBe expectedGreetResult
                }
            verify(testApi).greet(name)
        }

        "works as expected with String input and Unit output" in {
            val value = "Important information"
            when(testApi.putString(value)).thenReturn(Result.Ok(()))
            testApi.putString(value)
            routes.find(_.functionInfo.name == "put-string")
                .map { route =>
                    route.execute(List(value)) shouldBe Result.Ok(())
                }
            verify(testApi).putString(value)
        }

        "works as expected with Boolean input and Boolean output" in {
            val value = true
            when(testApi.testBoolean(value)).thenReturn(Result.Ok(true))
            val expectedTestBooleanResult = testApi.testBoolean(value).map(_.toString)
            routes.find(_.functionInfo.name == "testBoolean")
                .map { route =>
                    route.execute(List(value.toString)) shouldBe expectedTestBooleanResult
                }
            verify(testApi).testBoolean(value)
        }

        "works as expected with Object input and String output" in {
            val person = Person("John Doe", 30)
            val personJson = """{"name":"John Doe","age":30}"""

            when(testApi.putPerson(person)).thenReturn(Result.Ok("Success"))
            val expectedPutPersonResult = testApi.putPerson(person)
            routes.find(_.functionInfo.name == "putPerson")
                .map { route =>
                    route.execute(List(personJson)) shouldBe expectedPutPersonResult
                }
            verify(testApi).putPerson(person)
        }

        "works as expected with String input and Object output" in {
            val value = "John Doe"
            val person = Person("John Doe", 30)
            val expectedPersonJson = """{"name":"John Doe","age":30}"""
            when(testApi.getPerson(value)).thenReturn(Result.Ok(person))
            testApi.getPerson(value)
            routes.find(_.functionInfo.name == "getPerson")
                .map { route =>
                    route.execute(List(value)) shouldBe Result.Ok(expectedPersonJson)
                }
            verify(testApi).getPerson(value)
        }

        "works as expected with String input and Option Object output" in {
            val value = "John Doe"
            val person = Option(Person("John Doe", 30))
            val expectedPersonOptionJson = """[{"name":"John Doe","age":30}]"""
            when(testApi.getPersonOption(value)).thenReturn(Result.Ok(person))
            testApi.getPersonOption(value)
            routes.find(_.functionInfo.name == "getPersonOption")
                .map { route =>
                    route.execute(List(value)) shouldBe Result.Ok(expectedPersonOptionJson)
                }
            verify(testApi).getPersonOption(value)
        }

        "works as expected with no input and Array Object output" in {
            val personArray = Array(Person("John Doe", 30), Person("Joanna Doe", 35))
            val expectedPersonArrayJson = """[{"name":"John Doe","age":30},{"name":"Joanna Doe","age":35}]"""
            when(testApi.listPerson).thenReturn(Result.Ok(personArray))
            testApi.listPerson
            routes.find(_.functionInfo.name == "listPerson")
                .map { route =>
                    route.execute(List()) shouldBe Result.Ok(expectedPersonArrayJson)
                }
            verify(testApi).listPerson
        }
    }

    "proxying" - {

        import UtilityFor._
        import UpickleEncoding._
        import UpickleStringImplicits._
        import UpickleUnitImplicits._

        val mockHandler = Mockito.mock(classOf[Handler[String, Result]])
        val testClient: TestApi[Result] = mkAPI[TestApi[Result]](mockHandler)

        "works as expected Int input and Int output" in {
            val a = 3
            val b = 2

            when(mockHandler.handle(ArgumentMatchers.eq(Info("sumInt", List(HttpGet("/sum-int"), Dummy()), "ru.sberbank.blockchain.cnft.Result[Int]")), ArgumentMatchers.eq(Seq(Info("a", List(), "Int"), Info("b", List(QueryParameter()), "Int"))), ArgumentMatchers.eq(Seq("3", "2")))).thenReturn(Result.Ok("5"))
            when(mockHandler.handle(ArgumentMatchers.eq(Info("subtractInt", List(HttpGet("/subtract-int")), "ru.sberbank.blockchain.cnft.Result[Int]")), ArgumentMatchers.eq(Seq(Info("a", List(QueryParameter()), "Int"), Info("b", List(), "Int"))), ArgumentMatchers.eq(Seq("3", "2")))).thenReturn(Result.Ok("1"))


            val expectedSumResult = testClient.sumInt(a, b)
            verify(mockHandler).handle(functionInfo = Info("sumInt", List(HttpGet("/sum-int"), Dummy()), "ru.sberbank.blockchain.cnft.Result[Int]"), argumentsInfo = Seq(Info("a", List(), "Int"), Info("b", List(QueryParameter()), "Int")), argumentsEncoded = Seq("3", "2"))
            assert(expectedSumResult.value == 5)

            val expectedSubtractResult = testClient.subtractInt(a, b)
            verify(mockHandler).handle(functionInfo = Info("subtractInt", List(HttpGet("/subtract-int")), "ru.sberbank.blockchain.cnft.Result[Int]"), argumentsInfo = Seq(Info("a", List(QueryParameter()), "Int"), Info("b", List(), "Int")), argumentsEncoded = Seq("3", "2"))
            assert(expectedSubtractResult.value == 1)

        }

        "works as expected Short input and Short output" in {
            val a: Short = 3
            val b: Short = 2

            when(mockHandler.handle(ArgumentMatchers.eq(Info("sumShort", List(HttpGet("/sum-short")), "ru.sberbank.blockchain.cnft.Result[Short]")), ArgumentMatchers.eq(Seq(Info("a", List(), "Short"), Info("b", List(QueryParameter()), "Short"))), ArgumentMatchers.eq(Seq("3", "2")))).thenReturn(Result.Ok("5"))
            when(mockHandler.handle(ArgumentMatchers.eq(Info("subtractShort", List(HttpGet("/subtract-short")), "ru.sberbank.blockchain.cnft.Result[Short]")), ArgumentMatchers.eq(Seq(Info("a", List(QueryParameter()), "Short"), Info("b", List(), "Short"))), ArgumentMatchers.eq(Seq("3", "2")))).thenReturn(Result.Ok("1"))


            val expectedSumResult = testClient.sumShort(a, b)
            verify(mockHandler).handle(functionInfo = Info("sumShort", List(HttpGet("/sum-short")), "ru.sberbank.blockchain.cnft.Result[Short]"), argumentsInfo = Seq(Info("a", List(), "Short"), Info("b", List(QueryParameter()), "Short")), argumentsEncoded = Seq("3", "2"))
            assert(expectedSumResult.value == 5.toShort)

            val expectedSubtractResult = testClient.subtractShort(a, b)
            verify(mockHandler).handle(functionInfo = Info("subtractShort", List(HttpGet("/subtract-short")), "ru.sberbank.blockchain.cnft.Result[Short]"), argumentsInfo = Seq(Info("a", List(QueryParameter()), "Short"), Info("b", List(), "Short")), argumentsEncoded = Seq("3", "2"))
            assert(expectedSubtractResult.value == 1.toShort)

        }

        "works as expected Long input and Long output" in {
            val a = 3L
            val b = 2L

            when(mockHandler.handle(ArgumentMatchers.eq(Info("sumLong", List(HttpGet("/sum-long")), "ru.sberbank.blockchain.cnft.Result[Long]")), ArgumentMatchers.eq(Seq(Info("a", List(), "Long"), Info("b", List(QueryParameter()), "Long"))), ArgumentMatchers.any())).thenReturn(Result.Ok("5"))
            when(mockHandler.handle(ArgumentMatchers.eq(Info("subtractLong", List(HttpGet("/subtract-long")), "ru.sberbank.blockchain.cnft.Result[Long]")), ArgumentMatchers.eq(Seq(Info("a", List(QueryParameter()), "Long"), Info("b", List(), "Long"))), ArgumentMatchers.eq(Seq("3", "2")))).thenReturn(Result.Ok("1"))


            val expectedSumResult = testClient.sumLong(a, b)
            verify(mockHandler).handle(functionInfo = Info("sumLong", List(HttpGet("/sum-long")), "ru.sberbank.blockchain.cnft.Result[Long]"), argumentsInfo = Seq(Info("a", List(), "Long"), Info("b", List(QueryParameter()), "Long")), argumentsEncoded = Seq("3", "2"))
            assert(expectedSumResult.value == 5)

            val expectedSubtractResult = testClient.subtractLong(a, b)
            verify(mockHandler).handle(functionInfo = Info("subtractLong", List(HttpGet("/subtract-long")), "ru.sberbank.blockchain.cnft.Result[Long]"), argumentsInfo = Seq(Info("a", List(QueryParameter()), "Long"), Info("b", List(), "Long")), argumentsEncoded = Seq("3", "2"))
            assert(expectedSubtractResult.value == 1)

        }

        "works as expected Float input and Float output" in {
            val a = 3.toFloat
            val b = 2.toFloat

            when(mockHandler.handle(ArgumentMatchers.eq(Info("sumFloat", List(HttpGet("/sum-float")), "ru.sberbank.blockchain.cnft.Result[Float]")), ArgumentMatchers.eq(Seq(Info("a", List(), "Float"), Info("b", List(QueryParameter()), "Float"))), ArgumentMatchers.any())).thenReturn(Result.Ok("5"))
            when(mockHandler.handle(ArgumentMatchers.eq(Info("subtractFloat", List(HttpGet("/subtract-float")), "ru.sberbank.blockchain.cnft.Result[Float]")), ArgumentMatchers.eq(Seq(Info("a", List(QueryParameter()), "Float"), Info("b", List(), "Float"))), ArgumentMatchers.eq(Seq("3", "2")))).thenReturn(Result.Ok("1"))


            val expectedSumResult = testClient.sumFloat(a, b)
            verify(mockHandler).handle(functionInfo = Info("sumFloat", List(HttpGet("/sum-float")), "ru.sberbank.blockchain.cnft.Result[Float]"), argumentsInfo = Seq(Info("a", List(), "Float"), Info("b", List(QueryParameter()), "Float")), argumentsEncoded = Seq("3", "2"))
            assert(expectedSumResult.value == 5)

            val expectedSubtractResult = testClient.subtractFloat(a, b)
            verify(mockHandler).handle(functionInfo = Info("subtractFloat", List(HttpGet("/subtract-float")), "ru.sberbank.blockchain.cnft.Result[Float]"), argumentsInfo = Seq(Info("a", List(QueryParameter()), "Float"), Info("b", List(), "Float")), argumentsEncoded = Seq("3", "2"))
            assert(expectedSubtractResult.value == 1)

        }


        "works as expected Double input and Double output" in {
            val a = 3.0
            val b = 2.0

            when(mockHandler.handle(ArgumentMatchers.eq(Info("sumDouble", List(HttpGet("/sum-double")), "ru.sberbank.blockchain.cnft.Result[Double]")), ArgumentMatchers.eq(Seq(Info("a", List(), "Double"), Info("b", List(QueryParameter()), "Double"))), ArgumentMatchers.any())).thenReturn(Result.Ok("5"))
            when(mockHandler.handle(ArgumentMatchers.eq(Info("subtractDouble", List(HttpGet("/subtract-double")), "ru.sberbank.blockchain.cnft.Result[Double]")), ArgumentMatchers.eq(Seq(Info("a", List(QueryParameter()), "Double"), Info("b", List(), "Double"))), ArgumentMatchers.eq(Seq("3", "2")))).thenReturn(Result.Ok("1"))


            val expectedSumResult = testClient.sumDouble(a, b)
            verify(mockHandler).handle(functionInfo = Info("sumDouble", List(HttpGet("/sum-double")), "ru.sberbank.blockchain.cnft.Result[Double]"), argumentsInfo = Seq(Info("a", List(), "Double"), Info("b", List(QueryParameter()), "Double")), argumentsEncoded = Seq("3", "2"))
            assert(expectedSumResult.value == 5)

            val expectedSubtractResult = testClient.subtractDouble(a, b)
            verify(mockHandler).handle(functionInfo = Info("subtractDouble", List(HttpGet("/subtract-double")), "ru.sberbank.blockchain.cnft.Result[Double]"), argumentsInfo = Seq(Info("a", List(QueryParameter()), "Double"), Info("b", List(), "Double")), argumentsEncoded = Seq("3", "2"))
            assert(expectedSubtractResult.value == 1)

        }

        "works as expected String input and String Output" in {
            val name = "John Doe"
            when(mockHandler.handle(ArgumentMatchers.eq(Info("greet", List(HttpGet("/greet")), "ru.sberbank.blockchain.cnft.Result[String]")), ArgumentMatchers.eq(Seq(Info("name", List(), "String"))), ArgumentMatchers.eq(Seq(name)))).thenReturn(Result.Ok("Greetings, John Doe!"))
            val expectedGreetingsResult = testClient.greet(name)
            verify(mockHandler).handle(functionInfo = Info("greet", List(HttpGet("/greet")), "ru.sberbank.blockchain.cnft.Result[String]"), argumentsInfo = Seq(Info("name", List(), "String")), argumentsEncoded = Seq("John Doe"))
            assert(expectedGreetingsResult.value == "Greetings, John Doe!")

        }

        "works as expected String input and Unit Output" in {
            val value = "Important Information"
            when(mockHandler.handle(ArgumentMatchers.eq(Info("putString", List(HttpGet("/put-string")), "ru.sberbank.blockchain.cnft.Result[String]")), ArgumentMatchers.eq(Seq(Info("value", List(), "String"))), ArgumentMatchers.eq(Seq(value)))).thenReturn(Result.Ok(""))
            val expectedPutResult = testClient.putString(value)
            verify(mockHandler).handle(functionInfo = Info("putString", List(HttpGet("/put-string")), "ru.sberbank.blockchain.cnft.Result[String]"), argumentsInfo = Seq(Info("value", List(), "String")), argumentsEncoded = Seq(value))
            expectedPutResult.value.shouldBe(())
        }

        "works as expected String input and Object Output" in {
            val value = "John Doe"
            val person = Person("John Doe", 30)
            val personJson = """{"name":"John Doe","age":30}"""

            when(mockHandler.handle(ArgumentMatchers.eq(Info("getPerson", List(HttpGet("/get-person")), "ru.sberbank.blockchain.cnft.Result[Person]")), ArgumentMatchers.eq(Seq(Info("value", List(), "Person"))), ArgumentMatchers.eq(Seq(value)))).thenReturn(Result.Ok(personJson))
            val expectedPutResult = testClient.getPerson(value)
            verify(mockHandler).handle(functionInfo = Info("getPerson", List(HttpGet("/get-person")), "ru.sberbank.blockchain.cnft.Result[Person]"), argumentsInfo = Seq(Info("value", List(), "Person")), argumentsEncoded = Seq(value))
            assert(expectedPutResult.value == person)
        }

        "works as expected String input and Option Object Output" in {
            val value = "John Doe"
            val person = Person("John Doe", 30)
            val personJson = """[{"name":"John Doe","age":30}]"""

            when(mockHandler.handle(ArgumentMatchers.eq(Info("getPersonOption", List(HttpGet("/get-person-option")), "ru.sberbank.blockchain.cnft.Result[Option[Person]]")), ArgumentMatchers.eq(Seq(Info("value", List(), "String"))), ArgumentMatchers.eq(Seq(value)))).thenReturn(Result.Ok(personJson))
            val expectedGetOptionResult = testClient.getPersonOption(value)
            verify(mockHandler).handle(functionInfo = Info("getPersonOption", List(HttpGet("/get-person-option")), "ru.sberbank.blockchain.cnft.Result[Option[Person]]"), argumentsInfo = Seq(Info("value", List(), "String")), argumentsEncoded = Seq(value))
            assert(expectedGetOptionResult.value == Option(person))
        }

        "works as expected no input and List Object Output" in {
            val personArray = Array(Person("John Doe", 30), Person("Joanna Doe", 35))
            val expectedPersonArrayJson = """[{"name":"John Doe","age":30},{"name":"Joanna Doe","age":35}]"""

            when(mockHandler.handle(ArgumentMatchers.eq(Info("listPerson", List(HttpGet("/list-person")), "ru.sberbank.blockchain.cnft.Result[Array[Person]]")), ArgumentMatchers.eq(Seq()), ArgumentMatchers.eq(Seq()))).thenReturn(Result.Ok(expectedPersonArrayJson))
            val expectedListPersonResult = testClient.listPerson
            verify(mockHandler).handle(functionInfo = Info("listPerson", List(HttpGet("/list-person")), "ru.sberbank.blockchain.cnft.Result[Array[Person]]"), argumentsInfo = Seq(), argumentsEncoded = Seq())
            assert(expectedListPersonResult.value sameElements personArray)
        }


        "works as expected Object input and String Output" in {
            val person = Person("John Doe", 30)
            val personJson = """{"name":"John Doe","age":30}"""

            when(mockHandler.handle(ArgumentMatchers.eq(Info("putPerson", List(HttpGet("/put-person")), "ru.sberbank.blockchain.cnft.Result[Person]")), ArgumentMatchers.eq(Seq(Info("person", List(), "Person"))), ArgumentMatchers.eq(Seq(personJson)))).thenReturn(Result.Ok("Success"))
            val expectedPutResult = testClient.putPerson(person)
            verify(mockHandler).handle(functionInfo = Info("putPerson", List(HttpGet("/put-person")), "ru.sberbank.blockchain.cnft.Result[Person]"), argumentsInfo = Seq(Info("person", List(), "Person")), argumentsEncoded = Seq(personJson))
            assert(expectedPutResult.value == "Success")
        }


        "works as expected Boolean input and Boolean Output" in {
            val value = true
            when(mockHandler.handle(ArgumentMatchers.eq(Info("testBoolean", List(HttpGet("/test-boolean")), "ru.sberbank.blockchain.cnft.Result[Boolean]")), ArgumentMatchers.eq(Seq(Info("value", List(), "Boolean"))), ArgumentMatchers.eq(Seq(value.toString)))).thenReturn(Result.Ok(value.toString))
            val expectedTestBooleanResult = testClient.testBoolean(value)
            verify(mockHandler).handle(functionInfo = Info("testBoolean", List(HttpGet("/test-boolean")), "ru.sberbank.blockchain.cnft.Result[Boolean]"), argumentsInfo = Seq(Info("value", List(), "Boolean")), argumentsEncoded = Seq("true"))
            assert(expectedTestBooleanResult.value == value)

        }
    }

}


object UpickleEncoding {

    implicit def upickleEncoder[T: upickle.default.Writer]: Encoder[T, String] = (value: T) => upickle.default.write(value)

    implicit def upickleDecoder[T: upickle.default.Reader]: Decoder[T, String] = (value: String) => upickle.default.read(value)

}

object UpickleStringImplicits {

    implicit val upickleStringEncoder: Encoder[String, String] = (value: String) => value
    implicit val upickleStringDecoder: Decoder[String, String] = (value: String) => value

}

object UpickleUnitImplicits {

    implicit val upickleUnitEncoder: Encoder[Unit, String] = (_: Unit) => ""
    implicit val upickleUnitDecoder: Decoder[Unit, String] = (_: String) => ()

}


case class HttpGet(url: String) extends StaticAnnotation

case class QueryParameter(name: String = "") extends StaticAnnotation

case class Dummy() extends StaticAnnotation

case class Person(name: String, age: Int)

object Person {
    implicit val rw: RW[Person] = macroRW
}
