package primes

import primes.protocol.get.ZioGet._
import zio._
import io.grpc.Status
import primes.protocol.get.{GetPrimesRequest, GetPrimesResponse}
import zio.stream.ZStream
import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList

object PrimesImpl extends ZPrimes[ZEnv, Any] {

  override def getPrimes(request: GetPrimesRequest): ZStream[ZEnv, Status, GetPrimesResponse] = {
    ZStream.fromIterable(List(2, 3, 5, 7, 11, 13).map(GetPrimesResponse(_)))
  }

}

object Main extends ServerMain {
  def services: ServiceList[zio.ZEnv] = ServiceList.add(PrimesImpl)
}
