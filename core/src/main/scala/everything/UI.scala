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

  val cardSize   = new Dimension(154/2, 184/2)
  val cardCtr    = new Point(cardSize.width/2, cardSize.height/2)
  val cardShadow = new Insets(1, 3, 3, 1) // shadow
  val cardInsets = new Insets(cardShadow.top+2, cardShadow.right+5,
                              cardShadow.bottom+1, cardShadow.left+5) // shadow+border
  val cardTextHt = 14
  val partCardTextHt = 12
  val cardImgBox = new Dimension(cardSize.width-cardInsets.width,
                                 cardSize.height-cardInsets.height-2*cardTextHt)

  val textColor = 0xFF442D17
  lazy val coinsIcon = getImage("money.png")
  lazy val cardFront = getImage("card_front.png")
  lazy val cardBack = getImage("card_back.png")
  lazy val cardGift = getImage("card_gift.png")
  lazy val cardPart = getImage("card_part.png")

  lazy val like = (getImage("like/pos.png"), getImage("like/pos_sel.png"))
  lazy val hate = (getImage("like/neg.png"), getImage("like/neg_sel.png"))

  val titleFont = machineFont(38)
  val menuFont = machineFont(24)
  val textFont = writingFont(16)
  val moneyFont = machineFont(12)
  val buttonFont = writingFont(20)
  val headerFont = machineFont(18)
  val subHeaderFont = machineFont(12)
  val notesHeaderFont = machineFont(14)
  val tipFont = writingFont(14)
  val factsFont = graphics.createFont("Georgia", Font.Style.PLAIN, 16)

  def machineFont (size :Float) = graphics.createFont(
    "Copperplate Gothic Bold", Font.Style.PLAIN, size)
  def writingFont (size :Float) = graphics.createFont("Josschrift", Font.Style.PLAIN, size)
  def glyphFont (size :Int) = graphics.createFont("Copperplate", Font.Style.BOLD, size)

  val statusCfg = new TextConfig(textColor).withFont(writingFont(18))
  val collectCfg = new TextConfig(textColor).withFont(writingFont(24))
  val cardCfg = new TextConfig(textColor).withFont(writingFont(10))
  val smallCardCfg = cardCfg.withFont(writingFont(8)).withWrapping(
    cardFront.width-cardShadow.width-4, TextFormat.Alignment.CENTER)
  val partCardCfg = new TextConfig(0xFF640000).withFont(glyphFont(10))
  val smallPartCardCfg = partCardCfg.withFont(glyphFont(8)).withWrapping(
    cardFront.width-cardShadow.width-10, TextFormat.Alignment.CENTER)

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
  def headerLabel (text :String) = label(text, headerFont)
  def subHeaderLabel (text :String) = label(text, subHeaderFont)
  def tipLabel (text :String) = label(text, tipFont)
  def wrapLabel (text :String) = new Label(text).addStyles(Style.TEXT_WRAP.on, Style.HALIGN.left)
  def glyphLabel (glyph :String) = label(glyph, glyphFont(14))

  def pathLabel (path :Seq[String], fontSize :Int = 14) = {
    val (font, gfont) = (writingFont(fontSize), glyphFont(fontSize))
    (UI.hgroup() /: path)((g, p) => g.childCount match {
      case 0 => g.add(label(p, font))
      case n => g.add(label(Category.SEP_CHAR, gfont), label(p, font))
    })
  }

  def inertButton (label :String, styles :Style.Binding[_]*) :Button =
    new Button(label).addStyles(styles :_*)
  def button (label :String, styles :Style.Binding[_]*)(action : =>Unit) :Button =
    inertButton(label, styles :_*).onClick(unitSlot(action))
  def labelButton (text :String, styles :Style.Binding[_]*)(action : => Unit) :Button =
    new LabelButton(text).addStyles(styles :_*).onClick(unitSlot(action))
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

  def cardImage (cache :ImageCache, card :ThingCard) = {
    val cardimg = graphics.createImage(cardFront.width, cardFront.height)
    cardimg.canvas.drawImage(cardFront, 0, 0)
    val (nameCfg, name) = {
      val full = cardCfg.layout(card.name)
      if (full.width <= smallCardCfg.format.wrapWidth) (cardCfg, full)
      else (smallCardCfg, smallCardCfg.layout(card.name))
    }
    val rarity = cardCfg.layout(card.rarity.toString)
    cache(card.image).addCallback(cb { thing =>
      // TODO: if the name wraps and cuts into the card area, use a smaller cardImgBox to determine
      // how much we should scale our card, and move the card image down as well
      val scale = math.min(cardImgBox.width/thing.width, cardImgBox.height/thing.height)
      val (swidth, sheight) = (thing.width*scale, thing.height*scale)
      val sx = math.round(cardInsets.left + (cardSize.width-cardInsets.width-swidth)/2)
      val sy = math.round(cardInsets.top + (cardSize.height-cardInsets.height-sheight)/2)
      // cardimg.canvas.setStrokeColor(textColor).strokeRect(
      //   sx-0.5f, sy-0.5f, swidth+0.5f, sheight+0.5f)
      cardimg.canvas.drawImage(thing, sx, sy, swidth, sheight)
      nameCfg.renderCX(cardimg.canvas, name, cardimg.width/2,
                       cardInsets.top + math.max((cardTextHt-name.height)/2, -1))
      cardCfg.renderCX(cardimg.canvas, rarity, cardimg.width/2,
                       cardimg.height - rarity.height - cardInsets.bottom)
    })
    cardimg
  }

  def partCardImage (card :ThingCard) = {
    val cardimg = graphics.createImage(cardPart.width, cardPart.height)
    cardimg.canvas.drawImage(cardPart, 0, 0)
    val (nameCfg, name) = {
      val full = partCardCfg.layout(card.name)
      if (full.width <= smallPartCardCfg.format.wrapWidth) (partCardCfg, full)
      else (smallPartCardCfg, smallPartCardCfg.layout(card.name))
    }
    nameCfg.renderCX(cardimg.canvas, name,
                     cardInsets.left + (cardimg.width - cardInsets.width)/2,
                     // subtract one more here because box is not vertically centered, sign
                     cardInsets.top + (cardimg.height - cardInsets.height - name.height)/2 - 1)
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
    override def getStyleClass = classOf[LabelButton]
  }
}
