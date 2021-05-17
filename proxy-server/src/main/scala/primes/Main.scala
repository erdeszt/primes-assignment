package primes

import zio._

object Main extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    console.putStrLn("Prime server").exitCode
  }
}
