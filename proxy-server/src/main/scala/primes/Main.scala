package primes

import primes.protocol.get.GetPrimesRequest
import primes.protocol.get.ZioGet._
import zio._
import io.grpc.ManagedChannelBuilder
import scalapb.zio_grpc.ZManagedChannel

object Main extends zio.App {

  val clientLayer = PrimesClient.live(
    ZManagedChannel(
      ManagedChannelBuilder.forAddress("127.0.0.1", 9000).usePlaintext()
    )
  )

  val app = PrimesClient
    .getPrimes(GetPrimesRequest(10))
    .foreach(prime => console.putStrLn(s"Prime: ${prime}"))

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    app.provideSomeLayer[zio.ZEnv](clientLayer).exitCode
  }
}
