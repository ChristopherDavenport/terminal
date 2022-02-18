package io.chrisdavenport.terminal

import cats.effect._
import cats.effect.std.Console
import fs2._
import scala.concurrent.duration._
import cats.instances.stream

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    IO.println("Line 1\nLine 2") *>
    // Stream.resource(Size.signal[IO]).flatMap{ signal => 
    // Stream.eval(VirtualLineTerminal.fromSizeSignal[IO](signal))
    Stream.resource(VirtualLineTerminal.impl[IO])
      .flatMap{vlt =>

        def line(name: String) = Stream.eval(vlt.newLine((i: Int) => fansi.Str(s"Line $name - 0 seconds with $i width")))
          .flatMap(token => 
            Stream.awakeDelay[IO](1.second).flatMap{d => 
              Stream.eval(vlt.edit(token, (i: Int) => fansi.Str(s"Line $name - ${d.toSeconds} seconds with $i width")))
            }
          )

        val f= line("1").take(3).compile.drain *>
          line("2").take(3).compile.drain
        Stream.eval(f)
        // println("vlt emitted")
        // Stream(
        //   line("1"),
        //   line("2"),
        //   line("3")
        // ).parJoinUnbounded
        // line("1")
      }
    // fs2.Stream.resource(Size.signal[IO])
    //   .flatMap(s => s.discrete)
    //   .evalMap(s => IO.println(s))
      .compile
      .drain
      .as(ExitCode.Success)
  }

}