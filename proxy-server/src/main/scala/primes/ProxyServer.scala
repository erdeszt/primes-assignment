package primes

import io.grpc.ManagedChannelBuilder
import primes.protocol.get.GetPrimesRequest
import primes.protocol.get.ZioGet._
import scalapb.zio_grpc.ZManagedChannel
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.stream.ZStream

object ProxyServer extends zio.App {

  private val separator = ZStream.fromIterable("\n".getBytes()).forever

  final case class GrpcError(status: io.grpc.Status) extends RuntimeException(s"GRPC Error. Status=${status}")

  sealed abstract class DomainError(message: String) extends RuntimeException(message)
  final case class InvalidNumberFormatError(original: Throwable) extends DomainError("Invalid number format")
  final case class NumberOutOfRangeError(number: Int) extends DomainError(s"Number: ${number} is out of range")

  val clientLayer = PrimesClient
    .live(ZManagedChannel(ManagedChannelBuilder.forAddress("127.0.0.1", 9000).usePlaintext()))
    .orDie

  val app = Http.collectM[Request] { case Method.GET -> Root / "prime" / rawNumber =>
    val primes = for {
      number <- ZIO.effect(rawNumber.toInt).mapError(InvalidNumberFormatError(_))
      validatedNumber <- ZIO.cond(number > 0, number, NumberOutOfRangeError(number))
    } yield PrimesClient
      .getPrimes(GetPrimesRequest(validatedNumber))
      .flatMap(response => ZStream.fromIterable(response.prime.toString.getBytes) ++ separator.take(1))
      .mapError(GrpcError(_))
      .refineOrDie(PartialFunction.empty)

    primes
      .fold(
        error => Response.fromHttpError(HttpError.BadRequest(error.getMessage)),
        primesStream =>
          Response.http(
            status = Status.OK,
            headers = List(Header.transferEncodingChunked),
            content = HttpData.fromStream(primesStream)
          )
      )
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    Server.start(8080, app).provideSomeLayer[zio.ZEnv](clientLayer).exitCode
  }
}
