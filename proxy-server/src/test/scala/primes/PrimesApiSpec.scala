package primes

import java.nio.charset.StandardCharsets
import sttp.client3._
import sttp.capabilities.zio.ZioStreams
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.duration._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.junit._

object PrimesApiSpecSbtRunner extends PrimesApiSpec
class PrimesApiSpec extends JUnitRunnableSpec {

  case class ServerThreads(prime: Fiber[Nothing, Unit], proxy: Fiber[Nothing, Unit])

  case class InvalidResponse(message: String) extends RuntimeException("Invalid response")

  val backend = HttpURLConnectionBackend()

  val startServers: URIO[ZEnv, ServerThreads] = (for {
    primeServer <- PrimeServer.run(List.empty).interruptible.unit.fork
    proxyServer <- ProxyServer.run(List.empty).interruptible.unit.fork
  } yield ServerThreads(primeServer, proxyServer)).provideSomeLayer[ZEnv](clock.Clock.live)

  def stopServers(threads: ServerThreads): UIO[Unit] = {
    (for {
      _ <- threads.proxy.interrupt.timeout(1.second)
      _ <- threads.prime.interrupt.timeout(1.second)
    } yield ()).provideLayer(clock.Clock.live)
  }

  def getPrimes(upTo: Int): IO[InvalidResponse, Vector[Long]] = {
    getPrimes(upTo.toString)
  }

  def getPrimes(upTo: String): IO[InvalidResponse, Vector[Long]] = {
    val request = basicRequest
      .get(uri"http://localhost:8080/prime/${upTo}")
      .response(asStreamUnsafe(ZioStreams))

    AsyncHttpClientZioBackend
      .managed()
      .use { backend =>
        request.send(backend).flatMap { response =>
          response.body match {
            case Left(error) => ZIO.fail(InvalidResponse(error))
            case Right(body) =>
              body.runCollect.orDie
                .map { rawBody =>
                  new String(rawBody.toArray, StandardCharsets.UTF_8)
                    .split("\n")
                    .map(_.toLong)
                    .toVector
                }
          }
        }
      }
      .refineToOrDie[InvalidResponse]
  }

  override def spec = suite("Primes API")(
    testM("It should return the first 10 primes") {
      for {
        primes <- getPrimes(10)
      } yield assert(primes)(equalTo(Vector[Long](2, 3, 5, 7, 11, 13, 17, 19, 23, 29)))
    },
    testM("It should return an error for negative numbers") {
      for {
        error <- getPrimes(-10).either
      } yield assert(error)(isLeft(hasField("message", _.message, equalTo("Number: -10 is out of range"))))
    },
    testM("It should return an error for invalid numbers") {
      for {
        error <- getPrimes("invalid").either
      } yield assert(error)(isLeft(hasField("message", _.message, equalTo("Invalid number format"))))
    }
  ) @@ aroundAll(startServers)(stopServers)

}
