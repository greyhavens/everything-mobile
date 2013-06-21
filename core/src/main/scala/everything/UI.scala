//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.mutable.{Map => MMap}

import react.Functions
import react.IntValue

import playn.core.PlayN._
import playn.core._
import pythagoras.f.Point
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

  val titleFont = graphics.createFont("Helvetica", Font.Style.BOLD, 48)
  val menuFont = graphics.createFont("Helvetica", Font.Style.BOLD, 24)
  val buttonFont = graphics.createFont("Helvetica", Font.Style.BOLD, 24)
  val headerFont = graphics.createFont("Helvetica", Font.Style.BOLD, 16);
  val subHeaderFont = graphics.createFont("Helvetica", Font.Style.BOLD, 14);
  val tipFont = textFont(10)
  def textFont (size :Int) = graphics.createFont("Helvetica", Font.Style.PLAIN, size)

  val cardCfg = new TextConfig(textColor).withFont(textFont(10)) // TODO
  val statusCfg = new TextConfig(textColor).withFont(textFont(18)) // TODO

  val absorber = new Layer.HitTester {
    def hitTest (layer :Layer, p :Point) = layer.hitTestDefault(p) match {
      case  null => layer
      case child => child
    }
  }

  val sheet = SimpleStyles.newSheetBuilder().
    add(classOf[Button], Style.FONT.is(buttonFont)).
    create()

  def hgroup (gap :Int = 5) = new Group(AxisLayout.horizontal().gap(gap))
  def hgroup (elems :Element[_]*) :Group = add(hgroup(5), elems)
  def vgroup (elems :Element[_]*) = add(new Group(AxisLayout.vertical()), elems)
  def vsgroup (elems :Element[_]*) = add(new Group(AxisLayout.vertical().offStretch),  elems)
  def add (group :Group, elems :Seq[Element[_]]) = (group /: elems)(_ add _)

  /** Creates a vertical-only scroller containing `group`. */
  def vscroll (contents :Group) = new Scroller(contents).setBehavior(Scroller.Behavior.VERTICAL)

  /** Creates a shim with the specified dimensions. */
  def shim (width :Float, height :Float) = new Shim(width, height)
  /** Returns a shim configured with an [AxisLayout] stretch constraint. */
  def stretchShim () :Shim = AxisLayout.stretch(shim(1, 1))

  def headerLabel (text :String) = new Label(text).addStyles(Style.FONT.is(headerFont))
  def subHeaderLabel (text :String) = new Label(text).addStyles(Style.FONT.is(subHeaderFont))
  def tipLabel (text :String) = new Label(text).addStyles(Style.FONT.is(tipFont))
  def wrapLabel (text :String) = new Label(text).addStyles(Style.TEXT_WRAP.on, Style.HALIGN.left)

  def button (label :String, styles :Style.Binding[_]*)(action : =>Unit) =
    new Button(label).addStyles(styles :_*).onClick(unitSlot(action))
  def labelButton (text :String, styles :Style.Binding[_]*)(action : => Unit) = new Button(text) {
    override def getStyleClass = classOf[Label]
  }.addStyles(styles.toArray :_*).addStyles(Style.UNDERLINE.on).onClick(unitSlot(action))
  def imageButton (image :Image)(action : => Unit) = new Button(Icons.image(image)) {
    override def getStyleClass = classOf[Label]
  }.addStyles(Style.ICON_POS.above).onClick(unitSlot(action))

  def icon (image :Image) = new Label(Icons.image(image))
  /** Creates a label that displays a currency amount. */
  def moneyIcon (coins :Int) = new Label(coins.toString, Icons.image(coinsIcon))
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
    val title = cardCfg.layout(card.name)
    cardCfg.renderCX(cardimg.canvas, title, cardimg.width/2, 2)
    cache(card.image).addCallback(cb { thing =>
      // these are hardcoded because the image is asymmetric and has built-in shadow... blah.
      val scale = math.min(42/thing.width, 50/thing.height)
      val (swidth, sheight) = (thing.width*scale, thing.height*scale)
      val (sx, sy) = (math.round((64-swidth)/2), math.round((78-sheight)/2))
      // cardimg.canvas.setStrokeColor(textColor).strokeRect(
      //   sx-0.5f, sy-0.5f, swidth+0.5f, sheight+0.5f)
      cardimg.canvas.drawImage(thing, sx, sy, swidth, sheight)
    })
    val rarity = cardCfg.layout(card.rarity.toString)
    cardCfg.renderCX(cardimg.canvas, rarity, cardimg.width/2, cardimg.height-rarity.height-2)
    cardimg
  }

  def statusImage (status :String) = {
    val image = graphics.createImage(cardFront.width, cardFront.height)
    val slay = statusCfg.layout(status)
    statusCfg.renderCX(image.canvas, slay, image.width/2, (image.height - slay.height)/2)
    image
  }

  def statusUpper (card :Button) :(SlotStatus => Unit) = {
    def update (msg :String) {
      // TODO: swap out old icon in puff of smoke or something
      card.icon.update(Icons.image(statusImage(msg)))
      card.setEnabled(false)
    }
    _ match {
      case SlotStatus.GIFTED => update("Gifted!")
      case   SlotStatus.SOLD => update("Sold!")
      case _ => // ignore
    }
  }
}
