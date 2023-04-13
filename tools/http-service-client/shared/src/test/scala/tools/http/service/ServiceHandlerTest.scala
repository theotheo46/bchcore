package tools.http.service

import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.funsuite.AnyFunSuite
import ru.sberbank.blockchain.cnft.commons.{Collection, Result}
import tools.http.service.annotations.{Body, HttpGet, HttpPost}
import utility.Info

class ServiceHandlerTest extends AnyFunSuite {


    test("Post positive case") {
        val TestBaseURL = "http://test"
        val TestMethodPath = "/test-post"
        val TestBody = "TestBody"
        val TargetUrl = TestBaseURL + TestMethodPath
        val TestResult = Result.Ok("OK")

        val executorMock = Mockito.mock(classOf[HttpRequestsExecutor[Result]])
        when(executorMock.buildURL(TargetUrl, Collection.empty)).thenReturn(TargetUrl)
        when(executorMock.post(TargetUrl, TestBody, Collection.empty)).thenReturn(TestResult)

        val handler = new ServiceHandler[Result](executorMock)
        //
        // emulate call to method defined like:
        //  @HttpPost("/test-post")
        //  def testPost(@Body body)
        //
        val result = handler.handle(
            functionInfo = Info("testPost", Seq(HttpPost("/test-post")), "ru.sberbank.blockchain.cnft.Result[String]"),
            argumentsInfo = Seq(Info("body", Seq(Body()), "Body")),
            argumentsEncoded = Seq(TestBody)
        )
        assert(result == TestResult)
    }


    test("Get positive case") {
        val TestBaseURL = "http://test"
        val TestMethodPath = "/test-get"
        val TargetUrl = TestBaseURL + TestMethodPath
        val TestResult = Result.Ok("OK")

        val executorMock = Mockito.mock(classOf[HttpRequestsExecutor[Result]])
        when(executorMock.buildURL(TargetUrl, Collection.empty)).thenReturn(TargetUrl)
        when(executorMock.get(TargetUrl, Collection.empty)).thenReturn(TestResult)

        val handler = new ServiceHandler[Result](executorMock)
        val result = handler.handle(
            functionInfo = Info("testGet", Seq(HttpGet("/test-get")), "ru.sberbank.blockchain.cnft.Result[String]"),
            argumentsInfo = Seq(Info("body", Seq(Body()), "Body")),
            argumentsEncoded = Seq()
        )
        assert(result == TestResult)
    }

    test("Get with parameters / positive case") {
        val TestBaseURL = "http://test"
        val TestMethodPath = "/test-get"
        val TargetUrl = TestBaseURL + TestMethodPath
        val ResultUrl = TargetUrl + "?body=1"
        val TestResult = Result.Ok("OK")

        val executorMock = Mockito.mock(classOf[HttpRequestsExecutor[Result]])
        when(executorMock.buildURL(TargetUrl, Collection(NamedValue("body", "1")))).thenReturn(ResultUrl)
        when(executorMock.get(ResultUrl, Collection.empty)).thenReturn(TestResult)

        val handler = new ServiceHandler[Result](executorMock)
        val result = handler.handle(
            functionInfo = Info("testGet", Seq(HttpGet("/test-get")), "ru.sberbank.blockchain.cnft.Result[String]"),
            argumentsInfo = Seq(Info("body", Seq(), "Body")),
            argumentsEncoded = Seq("1")
        )

        assert(result == TestResult)
    }

    test("Post / no argumentsInfo") {
        val TestBaseURL = "http://test"
        val TestMethodPath = "/test-post"
        val TestResult = Result.Ok("OK")
        val TestBody = "TestBody"

        val executorMock = Mockito.mock(classOf[HttpRequestsExecutor[Result]])
        when(executorMock.post(TestBaseURL + TestMethodPath, TestBody, Collection.empty)).thenReturn(TestResult)

        val handler = new ServiceHandler[Result](executorMock)
        assertThrows[NoSuchElementException](
            handler.handle(
                functionInfo = Info("testPost", Seq(HttpPost("/test-post")), "ru.sberbank.blockchain.cnft.Result[String]"),
                argumentsInfo = Seq(),
                argumentsEncoded = Seq(TestBody)
            )
        )
    }

    test("Post / no argumentsEncoded") {
        val TestBaseURL = "http://test"
        val TestMethodPath = "/test-post"
        val TestResult = Result.Ok("OK")
        val TestBody = "TestBody"

        val executorMock = Mockito.mock(classOf[HttpRequestsExecutor[Result]])
        when(executorMock.post(TestBaseURL + TestMethodPath, TestBody, Collection.empty)).thenReturn(TestResult)

        val handler = new ServiceHandler[Result](executorMock)
        assertThrows[NoSuchElementException](
            handler.handle(
                functionInfo = Info("testPost", Seq(HttpPost("/test-post")), "ru.sberbank.blockchain.cnft.Result[String]"),
                argumentsInfo = Seq(Info("body", Seq(Body()), "Body")),
                argumentsEncoded = Seq()
            )
        )
    }


    //    test("TestGet") {
    //
    //
    //        val mockTestApi = Mockito.mock(classOf[TestApi[EndpointResult]])
    //
    //        val mockCloseableHttpResponse = Mockito.mock(classOf[CloseableHttpResponse])
    //        mockCloseableHttpResponse.setStatusCode(200)
    //        val url = "http://localhost:8080/sum"
    //        val api = mkAPI[TestApi[EndpointResult]](new ServiceHandler[EndpointResult]("http://localhost:8080", executor))
    //        val noApi = new ServiceHandler[EndpointResult]("http://localhost:8080", executor)
    //
    //        println(api.sum(3, 5))
    //
    //    }

}
