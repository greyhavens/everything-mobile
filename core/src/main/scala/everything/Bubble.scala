//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN.graphics
import playn.core._
import pythagoras.f.{IPoint, IDimension, Dimension, Point, Points}
import tripleplay.ui.Element

object Bubbles {

  /** Used to render the contents of a bubble. */
  trait Contents {
    def width :Float
    def height :Float
    def render (canvas :Canvas, x :Float, y :Float)
  }

  val DEFAULT_WRAP_WIDTH = 200
  val STROKE_WIDTH = 3f
  val DEFAULT_TAIL_HEIGHT = 61f
  val HGAP = 10f
  val VGAP = 5f
  val RAD = 20f
  val HRAD = RAD/2
  val TVGAP = 15
  val NAME_FORMAT = new TextFormat().withFont(UI.writingFont(16))
  val DEETS_FORMAT = new TextFormat().withFont(UI.writingFont(12))

  sealed trait VPos
  object VPos {
    val ABOVE = new VPos {}
    val CENTER = new VPos {}
    val BELOW = new VPos {}
  }

  sealed trait TailType {
    def configPath (path :Path, top :Float, right :Float, bot :Float, left :Float,
                    tx :Float, tailHeight :Float, tail :IPoint)
  }

  object TailType {
    val SPEECH = new TailType {
      def configPath (path :Path, top :Float, right :Float, bot :Float, left :Float,
                      tx :Float, tailHeight :Float, tail :IPoint) {
        val tw = 20
        val tl = if (tail.x > 0) tx-2*tw else tx+tw
        val tr = tl+tw

        val (c1x, c2x, c3x, c4x) =
          if (tail.y > 0) {
            if (tail.x < 0) (tl, tl-tw/2, tx+tw/4, tl-tw/2) // left facing
            else            (tr+tw/2, tx-tw/4, tr+tw/2, tr) // right facing
          } else {
            if (tail.x < 0) (tl-tw/2, tx+tw/4, tl-tw/2, tl) // left facing
            else            (tr, tr+tw/2, tx-tw/4, tr+tw/2) // right facing
          }

        val (c1y, c2y) =
          if (tail.y > 0) (bot+tailHeight/3, bot+2*tailHeight/3)
          else            (top-tailHeight/3, top-2*tailHeight/3)
        val (c3y, c4y) = (c2y, c1y)

        path.moveTo(left+RAD, top)
        if (tail.y < 0) {
          path.lineTo(tl, top)
          path.bezierTo(c1x, c1y, c2x, c2y, tx, STROKE_WIDTH/2)
          path.bezierTo(c3x, c3y, c4x, c4y, tr, top)
          path.lineTo(right-RAD, top)
        } else {
          path.lineTo(right-RAD, top)
        }
        path.bezierTo(right-HRAD, top, right, top+HRAD, right, top+RAD)
        path.lineTo(right, bot-RAD)
        path.bezierTo(right, bot-HRAD, right-HRAD, bot, right-RAD, bot)
        if (tail.y > 0) {
          path.lineTo(tr, bot)
          path.bezierTo(c1x, c1y, c2x, c2y, tx, bot+tailHeight)
          path.bezierTo(c3x, c3y, c4x, c4y, tl, bot)
          path.lineTo(left+RAD, bot)
        } else {
          path.lineTo(left+RAD, bot)
        }
        path.bezierTo(left+HRAD, bot, left, bot-HRAD, left, bot-RAD)
        path.lineTo(left, top+RAD)
        path.bezierTo(left, top+HRAD, left+HRAD, top, left+RAD, top)
        path.close()
      }
    }

    val ARROW = new TailType {
      def configPath (path :Path, top :Float, right :Float, bot :Float, left :Float,
                      tx :Float, tailHeight :Float, tail :IPoint) {
        val tw = 20
        var (tl, tr) = (tx-tw/2, tx+tw/2)

        // constraint our arrow landing points to the non-curvy edge of the bubble
        if (tl < left+RAD) {
          tl = left+RAD
          tr = tl+tw
        } else if (tr > right-RAD) {
          tr = right-RAD
          tl = tr-tw
        }

        path.moveTo(left+RAD, top)
        if (tail.y < 0) {
          path.lineTo(tl, top)
          path.lineTo(tx, STROKE_WIDTH/2)
          path.lineTo(tr, top)
          path.lineTo(right-RAD, top)
        } else {
          path.lineTo(right-RAD, top)
        }
        path.bezierTo(right-HRAD, top, right, top+HRAD, right, top+RAD)
        path.lineTo(right, bot-RAD)
        path.bezierTo(right, bot-HRAD, right-HRAD, bot, right-RAD, bot)
        if (tail.y > 0) {
          path.lineTo(tr, bot)
          path.lineTo(tx, bot+tailHeight)
          path.lineTo(tl, bot)
          path.lineTo(left+RAD, bot)
        } else {
          path.lineTo(left+RAD, bot)
        }
        path.bezierTo(left+HRAD, bot, left, bot-HRAD, left, bot-RAD)
        path.lineTo(left, top+RAD)
        path.bezierTo(left, top+HRAD, left+HRAD, top, left+RAD, top)
        path.close()
      }
    }
  }

  def makeContents (text :String, wrapWidth :Float) = new Contents {
    val layout = graphics.layoutText(text, NAME_FORMAT.withWrapWidth(wrapWidth))
    def width = layout.width
    def height = layout.height
    def render (canvas :Canvas, x :Float, y :Float) = canvas.fillText(layout, x, y)
  }

  def makeContents (icon :Image, name :String, details :String, wrapWidth :Float) = new Contents {
    val nl = graphics.layoutText(name, NAME_FORMAT.withWrapWidth(wrapWidth))
    val dl = graphics.layoutText(details, DEETS_FORMAT.withWrapWidth(wrapWidth))
    def width = icon.width + HGAP + math.max(nl.width, dl.width)
    def height = nl.height + VGAP + dl.height
    def render (canvas :Canvas, x :Float, y :Float) {
      canvas.drawImage(icon, x, y)
      canvas.fillText(nl, x + icon.width + HGAP, y)
      canvas.fillText(dl, x + icon.width + HGAP, y + nl.height + VGAP)
    }
  }
}
import Bubbles._

/** Displays text in a word bubble. */
class Bubble (contents :Contents) {

  def this (text :String, wrapWidth :Float) = this(makeContents(text, wrapWidth))
  def this (text :String) = this(text, DEFAULT_WRAP_WIDTH)
  def this (icon :Image, name :String, details :String, wrapWidth :Float) =
    this(makeContents(icon, name, details, wrapWidth))
  def this (icon :Image, name :String, details :String) =
    this(icon, name, details, DEFAULT_WRAP_WIDTH)

  /** Causes the bubble to extend upward from its show point. */
  def above () = { _vpos = VPos.ABOVE ; this }
  /** Causes the bubble to be centered vertically on its show point. */
  def center () = { _vpos = VPos.CENTER ; this }

  /** Configures this bubble with a tail at the specified x offset into the bubble. If x is negative,
    * it will be measured from the right side of the bubble. */
  def upTail (x :Float) = tail(x, -DEFAULT_TAIL_HEIGHT)

  /** Configures this bubble with a tail at the specified x offset into the bubble. If x is negative,
    * it will be measured from the right side of the bubble. */
  def downTail (x :Float) = tail(x, DEFAULT_TAIL_HEIGHT)

  /** Configures this bubble with a tail at the specified x offset into the bubble and with the
    * specified tail height: positive y indicates down tail, negative y indicates up. If x is 0 the
    * tail will be centered in the bubble. If x is negative, it will be measured from the right
    * side of the bubble. */
  def tail (x :Float, height :Float) = {
    _tail = new Point(x, height)
    this
  }

  /** Configures this bubble to have an arrow tail rather than speech tail. */
  def arrowTail () = { _tailType = TailType.ARROW ; this }

  /** Configures the position at which this bubble will be displayed. */
  def at (x :Float, y :Float) :this.type = {
    _x = x
    _y = y
    this
  }

  /** Configures the position at which this bubble will be displayed. */
  def at (pos :IPoint) :this.type = at(pos.x, pos.y)

  /** Uses the supplied layer (as well as its alignment configuration) to position the bubble. When
    * shown above or below the anchor, a 5 pixel gap is added. */
  def at (screen :EveryScreen, anchor :Layer.HasSize) :this.type = {
    val size = new Dimension(anchor.width, anchor.height)
    _position = new Runnable { def run () { at(screen.pos(anchor), size) }}
    this
  }

  /** Uses the supplied element (as well as its alignment configuration) to position the bubble. When
    * shown above or below the anchor, a 5 pixel gap is added. */
  def at (screen :EveryScreen, anchor :Element[_]) :this.type = {
    _position = new Runnable { def run () { at(screen.pos(anchor.layer), anchor.size) }}
    this
  }

  /** Uses the supplied position and size define an anchor rectangle (as well as its alignment
    * configuration) to position the bubble. When shown above or below the anchor rectangle, a 5
    * pixel gap is added. */
  def at (pos :IPoint, size :IDimension) :this.type = {
    val x = pos.x + size.width/2
    val y = _vpos match {
      case  VPos.BELOW => pos.y + size.height + 5
      case  VPos.ABOVE => pos.y - 5
      case VPos.CENTER => pos.y + size.height/2
    }
    at(x, y)
  }

  /** Configures this bubble to pass through the click that dismisses it. */
  def passThroughClick () = { _passThroughClick = true ; this }

  // /** Displays this bubble at its configured coordinates.
  //   * @param andThen a runnable to be invoked when this bubble is dismissed (or null). */
  // def show (screen :EveryScreen, andThen :Runnable) {
  //   val layer = toLayer(screen)
  //   layer.setScale(0.1f)
  //   screen.iface.animator.tweenScale(layer).to(1f).in(200)
  //   val overlay = screen.createOverlay(layer)
  //   if (andThen != null) overlay.onDismiss(andThen)
  //   if (_passThroughClick) overlay.passThroughClick()
  //   overlay.display()
  // }

  /** Creates and positions the layer that displays this bubble. */
  def toLayer (screen :EveryScreen) :ImageLayer = {
    val bubble = createBubble()
    // if we have a thunk that will compute our position, run it
    if (_position != null) _position.run()
    bubble.setTranslation(_x, _y)
    // adjust the bubble's origin to ensure it doesn't extend off the screen
    val (bw, bh) = (bubble.width, bubble.height)
    val (maxx, maxy) = (screen.width-5, screen.height-5)
    var (ox, oy) = (bubble.originX, bubble.originY)
    if (_x - ox < 5) ox = _x - 5
    else if (_x + bw - ox > maxx) ox = bw - (maxx - _x)
    if (_y - oy < 5) oy = _y - 5
    else if (_y + bh - oy > maxy) oy = bh - (maxy - _y)
    bubble.setOrigin(ox, oy)
    bubble
  }

  // /** Converts this bubble into a runnable that shows the bubble. */
  // def toRunnable (screen :EveryScreen, andThen :Runnable) = new Runnable {
  //   def run () { show(screen, andThen) }
  // }

  protected def createBubble () :ImageLayer = {
    val (width, height) = (contents.width+RAD*2, contents.height+TVGAP*2)
    val tailHeight = math.abs(_tail.y)
    val image = graphics.createImage(width+STROKE_WIDTH, height+tailHeight+STROKE_WIDTH)

    val left = STROKE_WIDTH/2
    val right = left+width
    val top = left + (if (_tail.y < 0) tailHeight else 0)
    val bot = top+height
    val tx = if (_tail.x < 0) right+_tail.x else if (_tail.x > 0) left+_tail.x else left+width/2

    val path = image.canvas.createPath
    _tailType.configPath(path, top, right, bot, left, tx, tailHeight, _tail)

    // draw everything into the canvas image
    image.canvas().setFillColor(0xFFFFF1DA).fillPath(path)
    image.canvas().setFillColor(UI.textColor)
    contents.render(image.canvas(), left+RAD, top+TVGAP)
    image.canvas().setStrokeColor(0xFF321601).setStrokeWidth(STROKE_WIDTH)
    image.canvas().setLineJoin(Canvas.LineJoin.ROUND).strokePath(path)

    val layer = graphics.createImageLayer(image)
    if (_tail != Points.ZERO) {
      layer.setOrigin(tx, if (_tail.y > 0) bot+tailHeight else top-tailHeight)
    } else _vpos match {
      case  VPos.ABOVE => layer.setOrigin(layer.width/2, layer.height)
      case VPos.CENTER => layer.setOrigin(layer.width/2, layer.height/2)
      case  VPos.BELOW => layer.setOrigin(layer.width/2, 0)
    }
    layer
  }

  protected var (_x, _y) = (0f, 0f)
  protected var _vpos = VPos.BELOW
  protected var _tail = Points.ZERO // zero indicates no tail
  protected var _tailType = TailType.SPEECH
  protected var _passThroughClick = false
  protected var _position :Runnable = _
}
