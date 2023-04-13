import org.scalatest.funsuite.AnyFunSuite
import ru.sberbank.blockchain.cnft.megacuks.SignByResponseValidator

class SignByResponseValidatorTest extends AnyFunSuite {

    test("test rqUID validation") {
        val validInput = "bb17cd3b-864e-1597-b6a7-11b891e2cf1c"
        val invalidInput = "01234zk"

        println("test rqUID validation")

        assert(SignByResponseValidator.rqUID(validInput).map(_ => true).getOrElse(false), "should be valid")
        assert(SignByResponseValidator.rqUID(invalidInput).map(_ => false).getOrElse(true), "should be not valid")
    }

    test("test ServiceName validation") {
        val validInput = "0123456789abcdefrweqjklWkdfnop034"
        val invalidInput = "0123456789abcdefrweqjklWkdfnop0340123456789abcdefrweqjklWkdfnop0340123456789abcdefrweqjklWkdfnop0340123456789abcdefrweqjklWkdfnop034"

        println("test ServiceName validation")

        assert(SignByResponseValidator.serviceName(validInput).map(_ => true).getOrElse(false), "should be valid")
        assert(SignByResponseValidator.serviceName(invalidInput).map(_ => false).getOrElse(true), "should be not valid")
    }

    test("test SystemId validation") {
        val validInput = "0123456789abcdefrweqjklWkdfnop034"
        val invalidInput = "0123456789abcdefrweqjklWkdfnop0340123456789abcdefrweqjklWkdfnop0340123456789abcdefrweqjklWkdfnop0340123456789abcdefrweqjklWkdfnop034"

        println("test SystemId validation")

        assert(SignByResponseValidator.serviceName(validInput).map(_ => true).getOrElse(false), "should be valid")
        assert(SignByResponseValidator.serviceName(invalidInput).map(_ => false).getOrElse(true), "should be not valid")
    }

    test("test StatusCode validation") {
        val validInput = "0123456789"
        val invalidInput = "01234a"

        println("test StatusCode validation")

        assert(SignByResponseValidator.statusCode(validInput).map(_ => true).getOrElse(false), "should be valid")
        assert(SignByResponseValidator.statusCode(invalidInput).map(_ => false).getOrElse(true), "should be not valid")
    }

    test("test fileData validation") {
        val validInput = "iMSJEXSxA/UecVdW+suSK6GK//jhe0YORPd/U7OMgGk3oLKMoEmeEPgxpgbfnoyaNnhHSYgqO8="
        val invalidInput = "01234a#2ad"

        println("test fileData validation")

        assert(SignByResponseValidator.fileData(validInput).map(_ => true).getOrElse(false), "should be valid")
        assert(SignByResponseValidator.fileData(invalidInput).map(_ => false).getOrElse(true), "should be not valid")
    }

    test("test SignedData validation") {
        val validInput = "iMSJEXSxA/UecVdW+suSK6GK//jhe0YORPd/U7OMgGk3oLKMoEmeEPgxpgbfnoyaNnhHSYgqO8="
        val invalidInput = "01234a#2ad"

        println("test SignedData validation")

        assert(SignByResponseValidator.signedData(validInput).map(_ => true).getOrElse(false), "should be valid")
        assert(SignByResponseValidator.signedData(invalidInput).map(_ => false).getOrElse(true), "should be not valid")
    }

}
