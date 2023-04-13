package tools.http.service

import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.Request
import org.mockito.Mockito
import ru.sberbank.blockchain.cnft.commons.Result

import java.io.{PrintWriter, StringWriter}

object Mocks {
    val Writer: PrintWriter = new PrintWriter(new StringWriter())

    def testApiMock: TestApi[Result] = Mockito.mock(classOf[TestApi[Result]])

    def baseRequestMock: Request = Mockito.mock(classOf[Request])

    def requestMock: HttpServletRequest = Mockito.mock(classOf[HttpServletRequest])

    def responseMock: HttpServletResponse = Mockito.mock(classOf[HttpServletResponse])

}
