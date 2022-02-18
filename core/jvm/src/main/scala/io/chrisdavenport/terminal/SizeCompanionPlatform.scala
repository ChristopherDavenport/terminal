package io.chrisdavenport.terminal

import cats.syntax.all._
import io.chrisdavenport.process._
import io.chrisdavenport.process.syntax.all._
import cats.effect._
import fs2.concurrent.Signal

trait SizeCompanionPlatform {

  def size[F[_]: Async]: F[Size] = {
    val cp = ChildProcess.impl[F]
    (
      cp.exec(process"tput cols").map(_.trim.toInt),
      cp.exec(process"tput lines").map(_.trim.toInt)
    ).mapN(Size(_,_))
  }


  import sun.misc.{Signal => SunSignal, SignalHandler}

  def singleEvent[F[_]: Async]: F[Unit] = Async[F].async{ cb => 
    Async[F].delay{
      val terminalSizeChangedHandler: SignalHandler = {
        new SignalHandler {
          override def handle(sig: SunSignal): Unit = {
            cb(Right(()))
          }
        }
      }
      SunSignal.handle(new SunSignal("WINCH"), terminalSizeChangedHandler)
      None
    }
  }

  def signal[F[_]: Async]: Resource[F, Signal[F, Size]] = 
    Resource.eval(size).flatMap{
      fs2.Stream.repeatEval(singleEvent >> size)
        .holdResource(_)
    }
}