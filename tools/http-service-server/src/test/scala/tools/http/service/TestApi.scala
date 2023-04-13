package tools.http.service

import tools.http.service.annotations.{Body, HttpGet, HttpHeaderValue, HttpPost}

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
trait TestApi[F[_]] extends SA[F] with SB[F] with SP[F]


trait SA[F[_]] {

    @HttpGet("/sum")
    def sum(a: Int, b: Int): F[Int]
}

trait SB[F[_]] {

    @HttpGet("/subtract")
    def subtract(a: Int, b: Int): F[Int]
}

trait SP[F[_]] {

    @HttpPost("/test-post")
    @HttpHeaderValue("Content-Type", "application/json")
    def testPost(@Body body: TestPostBody): F[String]

    @HttpPost("/test-post")
    @HttpHeaderValue("Content-Type", "application/json")
    def testPostNoResponse(@Body body: TestPostBody): F[Unit]

    @HttpPost("/test-post-no-body")
    @HttpHeaderValue("Content-Type", "application/json")
    def testPostNoBody(body: TestPostBody): F[Unit]
}

case class TestPostBody(
    name: String,
    msg: String,
    code: Int
)

object TestPostBody {
    implicit val rw: upickle.default.ReadWriter[TestPostBody] = upickle.default.macroRW
}