package io.chrisdavenport.terminal

import cats.syntax.all._
import cats.effect._
import fs2.concurrent.Signal
import io.chrisdavenport.condemned._

trait SizeCompanionPlatform {
  def size[F[_]: Async]: F[Size] = Sync[F].delay(
    unsafeSize
  )
  def signal[F[_]: Async]: Resource[F, Signal[F, Size]] = Resource.eval(size).flatMap( size => 
    Resource.eval(Sync[F].delay{
      val chain = DeferredChain.start[F, Size](() => unsafeSize)
      val update: Function0[Unit] = () => chain.update()
      jsshims.stdoutMod.stdout.addListener_resize(jsshims.stdoutMod.strings.resize, update)
      chain
    }).flatMap{ chain => 
      chain.stream.holdResource(size)
    }
  )


  private def unsafeSize: Size = {
    val stdout = jsshims.stdoutMod.stdout
    try {
      val linesDouble = stdout.rows
      val colsDouble = stdout.columns
      val cols = colsDouble.toInt
      val lines = linesDouble.toInt
      Size(cols, lines)
    } catch {
      case e: Throwable => throw new Throwable("Not a TTY - Cannot get Size (are you in sbt???)", e)
    }
  } 

  private class DeferredChain[F[_]: Async, A](private val ref: UnsafeRef[UnsafeDeferred[F, A]], f: () => A){
    def update(): Unit = {
      val x = ref.modify(deff => 
        (UnsafeDeferred[F, A], deff)
      )
      val b = x.complete(f())
    }

    def stream: fs2.Stream[F, A] = fs2.Stream.repeatEval(Sync[F].delay(ref.get).flatMap(deff => deff.get))
  }
  private object DeferredChain { 
    def start[F[_]: Async, A](f: () => A) = new DeferredChain[F, A](UnsafeRef.of(UnsafeDeferred[F, A]), f)
  }
}