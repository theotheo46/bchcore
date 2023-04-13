package tools.http.service

import jakarta.servlet.http.HttpServletResponse
import org.mockito.Mockito.{verify, when}
import org.scalatest.funsuite.AnyFunSuite
import ru.sberbank.blockchain.cnft.commons.Result
//import utility.UtilityFor._

import java.io.{BufferedReader, StringReader}

class HttpServerServiceTest extends AnyFunSuite {

   // import UtilityFor._
    import tools.http.service.Mocks._
    import HttpServerService._

    test("Unsupported method type methodName / should works fine") {
        val request = requestMock
        val response = responseMock
        val methodName = "noMethod"
        val serviceJettyHandler = new ServiceJettyHandler[TestApi[Result]](testApiMock)

        when(request.getMethod).thenReturn(methodName)
        when(response.getWriter).thenReturn(Writer)

        serviceJettyHandler.service(request, response)
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST)

    }

    test("POST / when return status 404 (SC_NOT_FOUND)") {
        val testApi = testApiMock

        val request = requestMock
        val response = responseMock
        val methodName = "POST"
        val serviceJettyHandler = new ServiceJettyHandler[TestApi[Result]](testApi)

        when(request.getMethod).thenReturn(methodName)
        when(request.getPathInfo).thenReturn(null)

        serviceJettyHandler.service(request, response)
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND)

    }

    test("POST / testPost(TestPostBody) / when return status 200 (SC_OK)") {
        val testApi = testApiMock

        val request = requestMock
        val response = responseMock
        val methodName = "POST"
        val serviceJettyHandler = new ServiceJettyHandler[TestApi[Result]](testApi)
        val json = "{\"name\":\"name\", \"msg\":\"sdsd\", \"code\": 222}"

        when(request.getMethod).thenReturn(methodName)
        when(request.getPathInfo).thenReturn("/test-post")
        when(request.getReader).thenReturn(new BufferedReader(new StringReader(json)))
        when(
            testApi.testPost(TestPostBody(name = "name", msg = "sdsd", code = 222))
        ).thenReturn(Result.Ok("ok"))

        when(response.getWriter).thenReturn(Writer)

        serviceJettyHandler.service(request, response)
        verify(response).setStatus(HttpServletResponse.SC_OK)

    }

    test("POST / test-post-no-body / Mandatory parameter is missing") {
        val testApi = testApiMock

        val request = requestMock
        val response = responseMock
        val methodName = "POST"
        val serviceJettyHandler = new ServiceJettyHandler[TestApi[Result]](testApi)
        val json = "{\"name\":\"name\", \"msg\":\"sdsd\", \"code\": 222}"

        when(request.getMethod).thenReturn(methodName)
        when(request.getPathInfo).thenReturn("/test-post-no-body")
        when(request.getReader).thenReturn(new BufferedReader(new StringReader(json)))
        when(response.getWriter).thenReturn(Writer)

        serviceJettyHandler.service(request, response)
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)

    }

    test("POST / test-post-no-body / Mandatory parameter is missing2") {
        val testApi = testApiMock

        val request = requestMock
        val response = responseMock
        val methodName = "POST"
        val serviceJettyHandler = new ServiceJettyHandler[TestApi[Result]](testApi)
        val json = "{\"name\":\"name\", \"msg\":\"sdsd\", \"code\": 222}"

        when(request.getMethod).thenReturn(methodName)
        when(request.getPathInfo).thenReturn("/test-post-no-body")
        when(request.getReader).thenReturn(new BufferedReader(new StringReader(json)))
        when(response.getWriter).thenReturn(Writer)

        serviceJettyHandler.service(request, response)
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)

    }

    test("GET / sum / 200 (SC_OK) ") {
        val testApi = testApiMock

        val request = requestMock
        val response = responseMock
        val methodName = "GET"
        val serviceJettyHandler = new ServiceJettyHandler[TestApi[Result]](testApi)

        when(request.getMethod).thenReturn(methodName)
        when(request.getPathInfo).thenReturn("/sum")
        when(request.getParameter("a")).thenReturn("3")
        when(request.getParameter("b")).thenReturn("4")
        when(testApi.sum(3, 4)).thenReturn(Result.Ok(7))
        when(response.getWriter).thenReturn(Writer)

        serviceJettyHandler.service(request, response)
        verify(response).setStatus(HttpServletResponse.SC_OK)

    }

    test("GET / sum / Mandatory parameter is missing") {
        val testApi = testApiMock

        val request = requestMock
        val response = responseMock
        val methodName = "GET"
        val serviceJettyHandler = new ServiceJettyHandler[TestApi[Result]](testApi)

        when(request.getMethod).thenReturn(methodName)
        when(request.getPathInfo).thenReturn("/sum")
        when(response.getWriter).thenReturn(Writer)

        serviceJettyHandler.service(request, response)
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)

    }

    test("GET / sum / 404 (SC_NOT_FOUND)") {
        val testApi = testApiMock

        val request = requestMock
        val response = responseMock
        val methodName = "GET"
        val serviceJettyHandler = new ServiceJettyHandler[TestApi[Result]](testApi)

        when(request.getMethod).thenReturn(methodName)
        when(response.getWriter).thenReturn(Writer)

        serviceJettyHandler.service(request, response)
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND)

    }
}
