//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.mutable.{Map => MMap}

import react.Functions
import react.IntValue

import playn.core.PlayN._
import playn.core._
import pythagoras.f.{Dimension, IDimension, FloatMath, MathUtil, Point}
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.ui.util.Insets
import tripleplay.util.{DestroyableBag, TextConfig}

import com.threerings.everything.data._

object UI {

  class ImageCache (game :Everything) {
    def apply (hash :String) :Image = {
      _images.getOrElseUpdate(hash, assets.getRemoteImage(game.cardImageURL(hash)))
    }
    private val _images = MMap[String,Image]()
  }

  class CardRenderer (prefix :String, width :Float, height :Float, textSize :Int,
                      shadow :Insets, border :Insets) {
    val front = getImage(prefix + "/front.png")
    val back  = getImage(prefix + "/back.png")
    val gift  = getImage(prefix + "/gift.png")
    val part  = getImage(prefix + "/part.png")

    val textHt = textSize + 4 // TODO: really?
    val size   = new Dimension(width, height)
    val ctr    = new Point(size.width/2, size.height/2)
    val insets = new Insets(shadow.top+border.top, shadow.right+border.right,
                            shadow.bottom+border.bottom, shadow.left+border.left)
    val imgBox = new Dimension(size.width-insets.width, size.height-insets.height-2*textHt)

    val cfg = new TextConfig(textColor).withFont(writingFont(textSize))
    val smallCfg = cfg.withFont(writingFont(MathUtil.ifloor(textSize*0.8f))).withWrapping(
      front.width-shadow.width-4, TextFormat.Alignment.CENTER)

    val partCfg = new TextConfig(0xFF640000).withFont(glyphFont(10))
    val smallPartCfg = partCfg.withFont(glyphFont(8)).withWrapping(
      front.width-shadow.width-10, TextFormat.Alignment.CENTER)

    def image (cache :ImageCache, card :ThingCard) = {
      val cardimg = graphics.createImage(front.width, front.height)
      cardimg.canvas.drawImage(front, 0, 0)
      val (nameCfg, name) = {
        val full = cfg.layout(card.name)
        if (full.width <= smallCfg.format.wrapWidth) (cfg, full)
        else (smallCfg, smallCfg.layout(card.name))
      }
      val rarity = cfg.layout(card.rarity.toString)
      cache(card.image).addCallback(cb { thing =>
        // TODO: if the name wraps and cuts into the card area, use a smaller imgBox to determine
        // how much we should scale our card, and move the card image down as well
        val scale = math.min(imgBox.width/thing.width, imgBox.height/thing.height)
        val (swidth, sheight) = (thing.width*scale, thing.height*scale)
        val sx = math.round(insets.left + (size.width-insets.width-swidth)/2)
        val sy = math.round(insets.top + (size.height-insets.height-sheight)/2)
        // cardimg.canvas.setStrokeColor(textColor).strokeRect(
        //   sx-0.5f, sy-0.5f, swidth+0.5f, sheight+0.5f)
        cardimg.canvas.drawImage(thing, sx, sy, swidth, sheight)
        nameCfg.renderCX(cardimg.canvas, name, cardimg.width/2,
                         insets.top + math.max((textHt-name.height)/2, -1))
        cfg.renderCX(cardimg.canvas, rarity, cardimg.width/2,
                     cardimg.height - rarity.height - insets.bottom)
      })
      cardimg
    }

    def partImage (card :ThingCard) = {
      val cardimg = graphics.createImage(part.width, part.height)
      cardimg.canvas.drawImage(part, 0, 0)
      val (nameCfg, name) = {
        val full = partCfg.layout(card.name)
        if (full.width <= smallPartCfg.format.wrapWidth) (partCfg, full)
        else (smallPartCfg, smallPartCfg.layout(card.name))
      }
      nameCfg.renderCX(cardimg.canvas, name,
                       insets.left + (cardimg.width - insets.width)/2,
                       // subtract one more here because box is not vertically centered, sigh
                       insets.top + (cardimg.height - insets.height - name.height)/2 - 1)
      cardimg
    }

    def statusImage (status :String) = {
      val image = graphics.createImage(front.width, front.height)
      val slay = statusCfg.layout(status)
      statusCfg.renderCX(image.canvas, slay, image.width/2, (image.height - slay.height)/2)
      image
    }
  }

  lazy val card = new CardRenderer(
    "card", 154/2, 184/2, 10, new Insets(1, 3, 3, 1), new Insets(2, 5, 1, 5))
  lazy val bigCard = new CardRenderer(
    "bigcard", 206/2, 246/2, 14, new Insets(1.5f, 4, 4, 1.5f), new Insets(2.5f, 7, 2, 7))

  val textColor = 0xFF442D17
  lazy val coinsIcon = getImage("money.png")
  lazy val like = (getImage("like/pos.png"), getImage("like/pos_sel.png"))
  lazy val hate = (getImage("like/neg.png"), getImage("like/neg_sel.png"))
  lazy val megaCard = getImage("megacard/front.png")
  lazy val pageBG = getImage("page_repeat.png")

  val titleFont = machineFont(38)
  val menuFont = machineFont(24)
  val textFont = writingFont(16)
  val moneyFont = machineFont(12)
  val buttonFont = writingFont(20)
  val headerFont = machineFont(18)
  val subHeaderFont = machineFont(12)
  val notesHeaderFont = machineFont(14)
  val tipFont = writingFont(14)
  val collectFont = writingFont(24)
  val factsFont = graphics.createFont("Georgia", Font.Style.PLAIN, 16)
  val notesFont = graphics.createFont("Georgia", Font.Style.PLAIN, 14)

  def machineFont (size :Float) = graphics.createFont(
    "CopperplateGothic-Bold", Font.Style.PLAIN, size)
  def writingFont (size :Float) = graphics.createFont("Josschrift", Font.Style.PLAIN, size)
  def glyphFont (size :Int) = graphics.createFont("Copperplate", Font.Style.BOLD, size)

  val statusCfg = new TextConfig(textColor).withFont(writingFont(18))
  val collectCfg = new TextConfig(textColor).withFont(collectFont)

  val absorber = new Layer.HitTester {
    def hitTest (layer :Layer, p :Point) = layer.hitTestDefault(p) match {
      case  null => layer
      case child => child
    }
  }

  def sheet = SimpleStyles.newSheetBuilder().
    add(classOf[Element[_]], Style.COLOR.is(textColor), Style.FONT.is(textFont)).
    add(classOf[Element[_]], Style.Mode.DISABLED, Style.COLOR.is(0xFF999999)).
    add(classOf[Button], Style.BACKGROUND.is(Background.blank().inset(0, 1, 1, 0)),
        Style.FONT.is(buttonFont), Style.UNDERLINE.on, Style.TEXT_EFFECT.shadow,
        Style.SHADOW.is(0x55000000), Style.SHADOW_X.is(1f), Style.SHADOW_Y.is(1f)).
    add(classOf[Button], Style.Mode.SELECTED,
        Style.SHADOW.is(0x00000000), Style.BACKGROUND.is(Background.blank().inset(1, 0, 0, 1))).
    add(classOf[Button], Style.Mode.DISABLED, Style.TEXT_EFFECT.none).
    add(classOf[LabelButton], Style.BACKGROUND.is(Background.blank().inset(0, 0.5f, 0.5f, 0)),
        Style.FONT.is(textFont), Style.SHADOW_X.is(0.5f), Style.SHADOW_Y.is(0.5f)).
    add(classOf[LabelButton], Style.Mode.SELECTED,
        Style.BACKGROUND.is(Background.blank().inset(0.5f, 0, 0, 0.5f))).
    // add(classOf[Group], Style.BACKGROUND.is(Background.solid(0xFFCCCCCC))).
    create()

  def hgroup (gap :Int = 5) = new Group(AxisLayout.horizontal().gap(gap))
  def hgroup (elems :Element[_]*) :Group = add(hgroup(5), elems)
  def vgroup (elems :Element[_]*) = add(new Group(AxisLayout.vertical().offConstrain), elems)
  def vgroup0 (elems :Element[_]*) = add(new Group(AxisLayout.vertical().offConstrain.gap(0)), elems)
  def vsgroup (elems :Element[_]*) = add(new Group(AxisLayout.vertical().offStretch),  elems)
  def plate (image :Element[_], elems :Element[_]*) = {
    val right = vgroup0(elems :_*).setConstraint(AxisLayout.stretched).addStyles(Style.HALIGN.left)
    hgroup().add(image, right)
  }
  def bgroup (elems :Element[_]*) = {
    val group = new Group(AxisLayout.horizontal().gap(0)).add(stretchShim())
    (group /: elems)(_ add(_, stretchShim()))
    group
  }
  def stretchBox () = AxisLayout.stretch(new Box())
  protected def add (group :Group, elems :Seq[Element[_]]) = (group /: elems)(_ add _)

  /** Creates a vertical-only scroller containing `group`. */
  def vscroll (contents :Element[_]) = new Scroller(contents).setBehavior(Scroller.Behavior.VERTICAL)

  /** Creates a shim with the specified dimensions. */
  def shim (width :Float, height :Float) = new Shim(width, height)
  /** Returns a shim configured with an [AxisLayout] stretch constraint. */
  def stretchShim () :Shim = AxisLayout.stretch(shim(1, 1))

  def label (text :String, font :Font) = new Label(text).addStyles(Style.FONT.is(font))
  def headerLabel (text :String) = label(text, headerFont).addStyles(Style.AUTO_SHRINK.on)
  def subHeaderLabel (text :String) = label(text, subHeaderFont)
  def tipLabel (text :String) = label(text, tipFont)
  def wrapLabel (text :String) = new Label(text).addStyles(Style.TEXT_WRAP.on, Style.HALIGN.left)
  def glyphLabel (glyph :String) = label(glyph, glyphFont(14))

  trait GlyphRenderer {
    def computeSize (hintX :Float, hintY :Float) :Dimension
    def render (canvas :Canvas) :Unit
  }
  class RenderedWidget (renderer :GlyphRenderer) extends Widget[RenderedWidget] {
    protected def getStyleClass = classOf[RenderedWidget]
    protected def createLayoutData (hintX :Float, hintY :Float) = new LayoutData {
      override def computeSize (hintX :Float, hintY :Float) = renderer.computeSize(hintX, hintY)
      override def layout (left :Float, top :Float, width :Float, height :Float) {
        super.layout(left, top, width, height)
        if (width == 0 && height == 0) _glyph.destroy()
        else {
          _glyph.prepare(width, height)
          _glyph.layer().setTranslation(left, top)
          renderer.render(_glyph.canvas)
        }
      }
    }
    protected val _glyph = new Glyph()
  }
  def glyphWidget (renderer :GlyphRenderer) = new RenderedWidget(renderer)

  def pathLabel (path :Seq[String], fontSize :Int = 14) = glyphWidget(new GlyphRenderer() {
    final val MinFontSize = 8
    var lay :Layout = _
    var hint = 0f

    def computeSize (hintX :Float, hintY :Float) = {
      lay = (if (hint == hintX && lay != null) lay
             else if (hintX == 0) new Layout(fontSize)
             else layForSize(hintX, fontSize))
      hint = hintX
      lay.size
    }

    def render (canvas :Canvas) {
      (if (lay != null && lay.size.width <= canvas.width) lay
       else layForSize(canvas.width, fontSize)).render(canvas)
    }

    def layForSize (width :Float, fontSize :Int) :Layout = {
      val lay = new Layout(fontSize)
      if (lay.size.width <= width || fontSize <= MinFontSize) lay
      else layForSize(width, fontSize-1)
    }

    class Layout (fontSize :Int) {
      val (font, gfont) = (writingFont(fontSize), glyphFont(fontSize))
      val sepFmt = new TextFormat().withFont(gfont)
      val sepLay = graphics.layoutText(Category.SEP_CHAR, sepFmt)
      val pathFmt = new TextFormat().withFont(font)
      val pathLays = path.map(graphics.layoutText(_, pathFmt))
      val size = new Dimension((sepLay.width + Gap*2) * (path.size-1) + pathLays.map(_.width).sum,
                               math.max(sepLay.height, pathLays.map(_.height).max))
      def render (canvas :Canvas) {
        canvas.setFillColor(textColor)
        val dx = (canvas.width - size.width)/2
        val nx = render(canvas, pathLays.head, dx)
        (nx /: pathLays.drop(1)) { (x, lay) =>
          val nx = render(canvas, sepLay, x+Gap)
          render(canvas, lay, nx+Gap)
        }
      }
      def render (canvas :Canvas, text :TextLayout, dx :Float) = {
        canvas.fillText(text, dx, (canvas.height - text.height)/2)
        dx + text.width
      }

      final val Gap = 2f
    }
  })

  def inertButton (label :String, styles :Style.Binding[_]*) :Button =
    new Button(label).addStyles(styles :_*)
  def button (label :String, styles :Style.Binding[_]*)(action : =>Unit) :Button =
    inertButton(label, styles :_*).onClick(unitSlot(action))
  def labelButton (text :String, styles :Style.Binding[_]*)(action : => Unit) :Button =
    new LabelButton(text).addStyles(styles :_*).onClick(unitSlot(action))
  def toggleButton (text :String, styles :Style.Binding[_]*)(action : => Unit) :ToggleButton = {
    val tb = new ToggleButton(text) {
      override protected def getStyleClass = classOf[Button]
    }.addStyles(styles :_*)
    tb.clicked.connect(unitSlot(action))
    tb
  }
  def imageButton (up :Image, down :Image)(action : => Unit) :ImageButton =
    new ImageButton(up, down).onClick(unitSlot(action))
  def moneyButton (amount :Int)(action :(Button => Unit)) = {
    val b = button(amount.toString)(())
    b.clicked.connect(action)
    b.icon.update(Icons.image(coinsIcon))
    b.addStyles(Style.ICON_GAP.is(0))
    b
  }

  def icon (image :Image) = new Label(Icons.image(image))
  /** Creates a label that displays a currency amount. */
  def moneyIcon (coins :Int) = new Label(coins.toString, Icons.image(coinsIcon)).
    addStyles(Style.FONT.is(moneyFont), Style.ICON_GAP.is(0))
  /** Creates a label that displays a (reactive) currency amount. */
  def moneyIcon (coins :IntValue, dbag :DestroyableBag) :Label = {
    val label = moneyIcon(0)
    dbag.add(coins.map(Functions.TO_STRING).connectNotify(label.text.slot()))
    label
  }
  def pupIcon (pup :Powerup) = icon(pupImage(pup))

  lazy val backImage = {
    val lay = graphics.layoutText("\u27A8", new TextFormat().withFont(glyphFont(28)))
    val img = graphics.createImage(lay.width, lay.height)
    img.canvas.scale(-1, 1).setFillColor(textColor).fillText(lay, -lay.width, 0)
    img
  }

  def getImage (path :String) = assets.getImageSync(s"images/$path")
  def pupImage (pup :Powerup) = getImage(s"pup/${pup.name.toLowerCase}.png")

  def friendImage (name :PlayerName) :Image = friendImage(name.facebookId)
  def friendImage (fbId :Long) :Image = {
    _friends.getOrElseUpdate(fbId, assets.getRemoteImage(
      s"https://graph.facebook.com/$fbId/picture?width=100&height=100"))
  }
  private val _friends = MMap[Long,Image]()

  def frameImage (image :Image, width :Float, height :Float) = {
    val frame = graphics.createImage(width, height)
    // frame.canvas.setFillColor(0xFFCC99CC).fillRect(0, 0, width, height)
    image.addCallback(cb { img =>
      val b = 1f
      val scale = math.min((width-2*b)/img.width, (height-2*b)/img.height)
      val (iwidth, iheight) = (img.width*scale, img.height*scale)
      val (fwidth, fheight) = (iwidth+2*b, iheight+2*b)
      val (fx, fy) = ((width-fwidth)/2, (height-fheight)/2)
      frame.canvas.
        setFillColor(0xFFFFFFFF).fillRect(fx, fy, fwidth, fheight).
        setStrokeColor(textColor).strokeRect(fx+b/2, fy+b/2, fwidth-b, fheight-b).
        translate(fx+b, fy+b).
        scale(scale, scale).drawImage(img, 0, 0)
    })
    frame
  }

  // from http://hansmuller-flex.blogspot.com/2011/10/more-about-approximating-circular-arcs.html
  def pieImage (pct :Float, radius :Float) :Image = {
    case class Curve (x1 :Float, y1 :Float, x2 :Float, y2 :Float,
                      x3 :Float, y3 :Float, x4 :Float, y4 :Float)
    def smallArc (a1 :Float, a2 :Float) :Curve = {
      // Compute all four points for an arc that subtends the same total angle but is centered on
      // the X-axis
      val a = (a2 - a1) / 2f
      val x4 = radius * FloatMath.cos(a)
      val y4 = radius * FloatMath.sin(a)
      val x1 = x4
      val y1 = -y4
      val q1 = x1*x1 + y1*y1
      val q2 = q1 + x1*x4 + y1*y4
      val k2 = 4/3f * (FloatMath.sqrt(2 * q1 * q2) - q2) / (x1 * y4 - y1 * x4)
      val x2 = x1 - k2 * y1
      val y2 = y1 + k2 * x1
      val x3 = x2
      val y3 = -y2
      // Find the arc points' actual locations by computing x1,y1 and x4,y4 and rotating the
      // control points by a + a1
      val ar = a + a1
      val cos_ar = FloatMath.cos(ar)
      val sin_ar = FloatMath.sin(ar)
      return Curve(
        x1 = radius * FloatMath.cos(a1),
        y1 = radius * FloatMath.sin(a1),
        x2 = x2 * cos_ar - y2 * sin_ar,
        y2 = x2 * sin_ar + y2 * cos_ar,
        x3 = x3 * cos_ar - y3 * sin_ar,
        y3 = x3 * sin_ar + y3 * cos_ar,
        x4 = radius * FloatMath.cos(a2),
        y4 = radius * FloatMath.sin(a2)
      )
    }

    val twoPi = 2*FloatMath.PI
    val piOver2 = FloatMath.PI/2
    val astart = -piOver2
    val aend = astart + twoPi*pct
    val image = graphics.createImage(2*radius+2, 2*radius+2)

    // Compute the sequence of arc curves, up to PI/2 at a time. Total arc angle is less than 2PI.
    val (xc, yc) = (radius+0.5f, radius+0.5f)
    var a1 = astart
    var remAngle = aend - astart
    var path :Path = null
    while (remAngle > MathUtil.EPSILON) {
      val a2 = a1 + Math.min(remAngle, piOver2)
      val curve = smallArc(a1, a2)
      if (path == null) {
        path = image.canvas.createPath()
        path.moveTo(xc, yc)
        path.lineTo(curve.x1 + xc, curve.y1 + yc)
      }
      path.bezierTo(curve.x2 + xc, curve.y2 + yc,
                    curve.x3 + xc, curve.y3 + yc,
                    curve.x4 + xc, curve.y4 + yc)
      remAngle -= Math.abs(a2 - a1)
      a1 = a2
    }
    path.lineTo(xc, yc)
    path.close()

    image.canvas.setFillColor(textColor).fillPath(path)
    image.canvas.setStrokeColor(textColor).strokeCircle(xc, yc, radius)
    image
  }

  protected class LabelButton (text :String, icon :Icon) extends Button(text, icon) {
    def this (text :String) = this(text, null)
    def this (icon :Icon) = this(null, icon)
    override def getStyleClass = classOf[LabelButton]
  }
}
