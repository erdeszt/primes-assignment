package primes

import io.grpc.Status
import primes.protocol.get.ZioGet._
import primes.protocol.get.{GetPrimesRequest, GetPrimesResponse}
import primes.protocol.Limits
import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio._
import zio.duration._
import zio.stream.ZStream

object PrimesImpl extends ZPrimes[ZEnv, Any] {

  // NOTE: Could be provided as a layer
  val primes = {
    var storage = Vector[Long](2L)
    var n       = 3L

    while (storage.length < Limits.MAX_NUMBER_OF_PRIMES) {
      var divisible = false
      val iterator  = storage.iterator
      while (iterator.hasNext && !divisible) {
        if (n % iterator.next() == 0) {
          divisible = true
        }
      }
      if (!divisible) {
        storage = storage :+ n
      }
      n += 2L
    }

    storage
  }

  // TODO: remove delay
  override def getPrimes(request: GetPrimesRequest): ZStream[ZEnv, Status, GetPrimesResponse] = {
    ZStream.fromIterable(primes.take(request.upTo).map(GetPrimesResponse(_))).schedule(Schedule.fixed(200.milliseconds))
  }

}

object PrimeServer extends ServerMain {
  def services: ServiceList[zio.ZEnv] = ServiceList.add(PrimesImpl)
}
