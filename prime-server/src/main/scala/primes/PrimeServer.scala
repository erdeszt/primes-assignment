package primes

import io.grpc.Status
import primes.protocol.get.ZioGet._
import primes.protocol.get.{GetPrimesRequest, GetPrimesResponse}
import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio._
import zio.duration._
import zio.stream.ZStream

object PrimesImpl extends ZPrimes[ZEnv, Any] {

  override def getPrimes(request: GetPrimesRequest): ZStream[ZEnv, Status, GetPrimesResponse] = {
    ZStream.fromIterable(List[Long](2, 3, 5, 7, 11, 13).map(GetPrimesResponse(_))).schedule(Schedule.fixed(1.second))
  }

}

object PrimeServer extends ServerMain {
  def services: ServiceList[zio.ZEnv] = ServiceList.add(PrimesImpl)
}
