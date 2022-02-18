package io.chrisdavenport.terminal.jsshims

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

private[terminal] object stdoutMod {

  def stdout =
    js.Dynamic.global.process.stdout.asInstanceOf[WriteStream]

  object strings {
    @js.native
    sealed trait resize extends js.Any
    @scala.inline
    def resize: resize = "resize".asInstanceOf[resize]
  }

  @JSImport("tty", "WriteStream")
  @js.native
  class WriteStream protected () extends js.Any {

    def getColorDepth(): Double = js.native
    def getColorDepth(env: js.Object): Double = js.native
    
    def getWindowSize(): js.Tuple2[Double, Double] = js.native
    
    def hasColors(): Boolean = js.native
    def hasColors(depth: Double): Boolean = js.native
    def hasColors(depth: Double, env: js.Object): Boolean = js.native
    def hasColors(env: js.Object): Boolean = js.native
    
    var isTTY: Boolean = js.native
    var rows: Double = js.native
    var columns: Double = js.native
    def addListener(event: java.lang.String, listener: js.Function1[/* repeated */ js.Any, Unit]): this.type = js.native
    @JSName("addListener")
    def addListener_resize(event: strings.resize, listener: js.Function0[Unit]): this.type = js.native
  }
}