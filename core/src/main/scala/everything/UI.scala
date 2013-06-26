//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.mutable.{Map => MMap}

import react.Functions
import react.IntValue

import playn.core.PlayN._
import playn.core._
import pythagoras.f.{Dimension, FloatMath, MathUtil, Point}
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.util.DestroyableBag
import tripleplay.util.TextConfig

import com.threerings.everything.data._

object UI {

  class ImageCache {
    def apply (hash :String) :Image = {
      _images.getOrElseUpdate(hash, assets.getRemoteImage( // TODO: more proper
        s"http://s3.amazonaws.com/everything.threerings.net/${hash}"))
    }
    private val _images = MMap[String,Image]()
  }

  val textColor = 0xFF442D17
  val coinsIcon = getImage("money.png")
  val cardFront = getImage("card_front.png")
  val cardBack = getImage("card_back.png")
  val cardGift = getImage("card_gift.png")

  val buttonUp = getImage("button/up.png")
  val buttonDn = getImage("button/down.png")
  val wideBtnUp = getImage("button/wide_up.png")
  val wideBtnDn = getImage("button/wide_down.png")

  val Machine = "Copperplate Gothic Bold"
  val Handwriting = "Josschrift"

  val titleFont = graphics.createFont(Machine, Font.Style.PLAIN, 38)
  val menuFont = graphics.createFont(Machine, Font.Style.PLAIN, 24)
  val moneyFont = graphics.createFont(Machine, Font.Style.PLAIN, 12)
  val buttonFont = graphics.createFont(Machine, Font.Style.PLAIN, 16)
  val wideButtonFont = graphics.createFont(Machine, Font.Style.PLAIN, 20)
  val headerFont = graphics.createFont(Machine, Font.Style.PLAIN, 18)
  val subHeaderFont = graphics.createFont(Machine, Font.Style.PLAIN, 12)
  val notesHeaderFont = graphics.createFont(Machine, Font.Style.PLAIN, 14)
  val tipFont = textFont(14)
  val factsFont = graphics.createFont("Georgia", Font.Style.PLAIN, 16)
  def textFont (size :Int) = graphics.createFont(Handwriting, Font.Style.PLAIN, size)

  def glyphFont (size :Int) = graphics.createFont("Arial" /*platformType match {
    case Platform.Type.ANDROID => "Droid Sans"
    case _                     => "Times New Roman"
  }*/, Font.Style.PLAIN, size)

  val statusCfg = new TextConfig(textColor).withFont(textFont(18))
  val cardCfg = new TextConfig(textColor).withFont(textFont(10)).withWrapping(
    cardFront.width-8, TextFormat.Alignment.CENTER)
  val smallCardCfg = new TextConfig(textColor).withFont(textFont(8)).withWrapping(
    cardFront.width-8, TextFormat.Alignment.CENTER)

  val absorber = new Layer.HitTester {
    def hitTest (layer :Layer, p :Point) = layer.hitTestDefault(p) match {
      case  null => layer
      case child => child
    }
  }

  def sheet = SimpleStyles.newSheetBuilder().
    add(classOf[Element[_]], Style.COLOR.is(textColor), Style.FONT.is(textFont(16))).
    add(classOf[Element[_]], Style.Mode.DISABLED, Style.COLOR.is(0xFF999999)).
    add(classOf[Button], Style.FONT.is(buttonFont),
        Style.BACKGROUND.is(Background.image(buttonUp).inset(0, 13, 2, 13))).
    add(classOf[Button], Style.Mode.SELECTED,
        Style.BACKGROUND.is(Background.image(buttonDn).inset(2, 11, 0, 15))).
    add(classOf[WideTextButton], Style.FONT.is(wideButtonFont),
        Style.BACKGROUND.is(Background.image(wideBtnUp).inset(1, 25, 5, 25))).
    add(classOf[WideTextButton], Style.Mode.SELECTED,
        Style.BACKGROUND.is(Background.image(wideBtnDn).inset(3, 23, 3, 27))).
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
  protected def add (group :Group, elems :Seq[Element[_]]) = (group /: elems)(_ add _)

  /** Creates a vertical-only scroller containing `group`. */
  def vscroll (contents :Group) = new Scroller(contents).setBehavior(Scroller.Behavior.VERTICAL)

  /** Creates a shim with the specified dimensions. */
  def shim (width :Float, height :Float) = new Shim(width, height)
  /** Returns a shim configured with an [AxisLayout] stretch constraint. */
  def stretchShim () :Shim = AxisLayout.stretch(shim(1, 1))

  def label (text :String, font :Font) = new Label(text).addStyles(Style.FONT.is(font))
  def headerLabel (text :String) = label(text, headerFont)
  def subHeaderLabel (text :String) = label(text, subHeaderFont)
  def tipLabel (text :String) = label(text, tipFont)
  def wrapLabel (text :String) = new Label(text).addStyles(Style.TEXT_WRAP.on, Style.HALIGN.left)
  def glyphLabel (glyph :String) = label(glyph, glyphFont(14))

  def pathLabel (path :Seq[String], fontSize :Int = 14) = {
    val (font, gfont) = (textFont(fontSize), glyphFont(fontSize))
    (UI.hgroup() /: path)((g, p) => g.childCount match {
      case 0 => g.add(label(p, font))
      case n => g.add(label(Category.SEP_CHAR, gfont), label(p, font))
    })
  }

  def inertButton (label :String, styles :Style.Binding[_]*) :Button =
    new TextButton(label).addStyles(styles :_*)
  def button (label :String, styles :Style.Binding[_]*)(action : =>Unit) :Button =
    inertButton(label, styles :_*).onClick(unitSlot(action))
  def wideButton (label :String, styles :Style.Binding[_]*)(action : =>Unit) :Button =
    new WideTextButton(label).addStyles(styles :_*).onClick(unitSlot(action))
  def labelButton (text :String, styles :Style.Binding[_]*)(action : => Unit) :Button =
    new LabelButton(text).addStyles(styles.toArray :_*).addStyles(Style.UNDERLINE.on).
      onClick(unitSlot(action))
  def imageButton (image :Image)(action : => Unit) :Button =
    new LabelButton(Icons.image(image)).addStyles(Style.ICON_POS.above).onClick(unitSlot(action))
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
    addStyles(Style.FONT.is(moneyFont))
  /** Creates a label that displays a (reactive) currency amount. */
  def moneyIcon (coins :IntValue, dbag :DestroyableBag) :Label = {
    val label = moneyIcon(0)
    dbag.add(coins.map(Functions.TO_STRING).connectNotify(label.text.slot()))
    label
  }

  def getImage (path :String) = assets.getImageSync(s"images/$path")

  def friendImage (name :PlayerName) :Image = friendImage(name.facebookId)
  def friendImage (fbId :Long) :Image = {
    _friends.getOrElseUpdate(fbId, assets.getRemoteImage(
      s"https://graph.facebook.com/$fbId/picture?width=100&height=100"))
  }
  private val _friends = MMap[Long,Image]()

  def frameImage (image :Image, width :Float, height :Float) = {
    val frame = graphics.createImage(width, height)
    image.addCallback(cb { img =>
      val border = 1
      val scale = math.min((width-2*border)/img.width, (height-2*border)/img.height)
      val (iwidth, iheight) = (img.width*scale, img.height*scale)
      val (fwidth, fheight) = (iwidth+2*border, iheight+2*border)
      val (fx, fy) = ((width-fwidth)/2, (height-fheight)/2)
      frame.canvas.
        setStrokeColor(textColor).strokeRect(fx, fy, fwidth-0.5f, fheight-0.5f).
        translate(fx+border, fy+border).
        scale(scale, scale).drawImage(img, 0, 0)
    })
    frame
  }

  def cardImage (cache :ImageCache, card :ThingCard) = {
    val cardimg = graphics.createImage(cardFront.width, cardFront.height)
    cardimg.canvas.drawImage(cardFront, 0, 0)
    val nameCfg = if (card.name.length > 10) smallCardCfg else cardCfg
    val title = nameCfg.layout(card.name)
    val rarity = cardCfg.layout(card.rarity.toString)
    cache(card.image).addCallback(cb { thing =>
      // these are hardcoded because the image is asymmetric and has built-in shadow... blah.
      val scale = math.min(42/thing.width, 50/thing.height)
      val (swidth, sheight) = (thing.width*scale, thing.height*scale)
      val (sx, sy) = (math.round((64-swidth)/2), math.round((78-sheight)/2))
      // cardimg.canvas.setStrokeColor(textColor).strokeRect(
      //   sx-0.5f, sy-0.5f, swidth+0.5f, sheight+0.5f)
      cardimg.canvas.drawImage(thing, sx, sy, swidth, sheight)
      nameCfg.renderCX(cardimg.canvas, title, cardimg.width/2, 2)
      cardCfg.renderCX(cardimg.canvas, rarity, cardimg.width/2, cardimg.height-rarity.height-2)
    })
    cardimg
  }

  def statusImage (status :String) = {
    val image = graphics.createImage(cardFront.width, cardFront.height)
    val slay = statusCfg.layout(status)
    statusCfg.renderCX(image.canvas, slay, image.width/2, (image.height - slay.height)/2)
    image
  }

  // from http://hansmuller-flex.blogspot.com/2011/10/more-about-approximating-circular-arcs.html
  def pieImage (pct :Float, radius :Float) = {
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
    override def getStyleClass = classOf[Label]
  }

  protected class TextButton (text :String) extends Button(text) {
    override protected def computeSize (hintX :Float, hintY :Float) :Dimension = {
      val d = super.computeSize(hintX, hintY)
      // d.width = buttonUp.width
      d.height = 2*buttonUp.height/3
      d
    }
  }

  protected class WideTextButton (text :String) extends Button(text) {
    override protected def computeSize (hintX :Float, hintY :Float) :Dimension = {
      val d = super.computeSize(hintX, hintY)
      // d.width = wideBtnUp.width
      d.height = wideBtnUp.height
      d
    }
    override protected def getStyleClass = classOf[WideTextButton]
  }
}
