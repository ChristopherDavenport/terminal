// package io.chrisdavenport.terminal

// import cats.effect._
// import cats.effect.std.Console
// import fs2._
// import scala.concurrent.duration._
// import cats.instances.stream

// object Main extends IOApp {

//   def run(args: List[String]): IO[ExitCode] = {
//     // IO.println("Line 1\nLine 2") *>
//     // Size.size[IO]
//     Stream.resource(VirtualLineTerminal.impl[IO])
//       .flatMap{vlt =>

//         def line(name: String, period: FiniteDuration) = Stream.eval(vlt.newLine((i: Int) => fansi.Str(s"Line $name - 0 seconds with $i width")))
//           .flatMap(token => 
//             Stream.awakeDelay[IO](period).flatMap{d => 
//               Stream.eval(vlt.edit(token, (i: Int) => fansi.Str(s"Line $name - ${d.toSeconds} seconds with $i width")))
//             }
//           )

//         def multiline(name: String, period: FiniteDuration) = Stream.eval(vlt.newLine((i: Int) => fansi.Str(s"Line $name - 0 seconds with $i width\nLine $name #2")))
//           .flatMap(token => 
//             Stream.awakeDelay[IO](period).flatMap{d => 
//               Stream.eval(vlt.edit(token, (i: Int) => fansi.Str(s"Line $name - ${d.toSeconds} seconds with $i width\nLine $name #2")))
//             }
//           )

//         // val f= 
//         //   line("1").take(3).compile.drain *>
//         //   line("2").take(3).compile.drain
//         // Stream.eval(f)
//         // println("vlt emitted")
//         Stream(
//           line("1", 1.second),
//           line("2", 2.seconds),
//           line("3", 5.seconds),
//           multiline("multi", 1.second)
//         ).parJoinUnbounded
//         // line("1")
//       }
//       .compile
//       .drain
  
//   }.as(ExitCode.Success)

// }