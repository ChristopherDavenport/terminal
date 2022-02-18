package io.chrisdavenport.terminal

import cats._
import cats.effect._
import cats.effect.std._
import cats.effect.syntax.all._
import scala.collection.immutable.Queue
import fansi.Str
import cats.syntax.all._
import cats.Applicative
import fs2._
import scala.concurrent.duration._

trait VirtualLineTerminal[F[_]] {
  def newLine(f: Int => fansi.Str): F[Unique.Token]
  def edit(token: Unique.Token, f: Int => fansi.Str): F[Unit]
  def delete(token: Unique.Token): F[Unit]

  def render: F[Unit]
}

object VirtualLineTerminal {

  def impl[F[_]: Async: Console]: Resource[F, VirtualLineTerminal[F]] = 
    Size.signal
      .evalMap(fromSizeSignal(_))
      .flatTap(vt => Stream.awakeEvery(10.millis).evalTap(_ => vt.render).compile.drain.background)

  def fromSizeSignal[F[_]: Console: Concurrent](signal: fs2.concurrent.Signal[F, Size]): F[VirtualLineTerminal[F]] = 
    Concurrent[F].ref(State.empty).map(ref => new VirtualLineTerminalImpl(ref, signal))

  // Width to Line
  case class LineFunction(f: Int => fansi.Str)

  case class State(
    lines: List[(Unique.Token, LineFunction)], // 
    rendered: Option[(fansi.Str, Size)] // todo store and only render diff for now if different then render
  )
  object State {
    def empty = State(List.empty, None)
  }


  private def internalRender(lines: List[(cats.effect.Unique.Token, LineFunction)], size: Size): fansi.Str = {
    val functions = lines.map(_._2)
    val out = functions.take(size.lines).map(_.f(size.cols).render).reverse
    fansi.Str(out.mkString("\n") ++ "\n")
  }

  private class VirtualLineTerminalImpl[F[_]: Spawn: Console](ref: Ref[F, State], signal: fs2.concurrent.Signal[F, Size]) extends VirtualLineTerminal[F]{
    def newLine(f: Int => fansi.Str): F[Unique.Token] = Unique[F].unique.flatMap(token => 
      ref.update{s => 
        val out = s.copy(lines = (token, LineFunction(f)) :: s.lines)
        out
      }.as(token)
    )
    
    def edit(token: Unique.Token, f: Int => Str): F[Unit] = ref.update(state => 
      state.lines.zipWithIndex.find(_._1._1 === token).map(_._2) match {
        case None => state
        case Some(idx) => 
          val newLines = state.lines.updated(idx, (token, LineFunction(f)))
          val out = State(newLines, state.rendered)
          out
      }


    )

    def delete(token: Unique.Token): F[Unit] = ref.update{state => 
      val newLines = state.lines.filter(_._1 =!= token)
      State(newLines, state.rendered)
    }

    private def renderNeed(size: Size): F[Option[(Option[(fansi.Str, Size)], (fansi.Str, Size))]] = ref.modify{state => 
      val out = internalRender(state.lines, size)
      if (out == state.rendered) state ->  None
      else (state.copy(rendered = Some(out, size)),  (state.rendered, (out, size)).some)
    }

    def render: F[Unit] = signal.get.flatMap(size => 
      renderNeed(size).flatMap(opt => 
        opt.traverse_{
          case (None, (s, _)) => 
            val out = s.render
            Console[F].print(out)
          case (Some((old, _)), (s, _)) => // TODO more complex logic to get overflow/underflow of vertical space right
            val oldLinesCount = old.plainText.split("\n").length - 1 
            val linesCount = s.plainText.split("\n").length - 1
            val preface = {
              s"\u001b[${oldLinesCount + 1}F"
            }
            val out = preface ++ s.render
            Console[F].print(out)
        }
      )
    )
  }

  // 60fps is 16 ms so an awake every 10ms perhaps.

  
}