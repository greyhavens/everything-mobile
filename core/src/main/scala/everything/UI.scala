//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.mutable.{Map => MMap}

import playn.core.PlayN._
import playn.core._
import playn.core.util.Callback

import react.Functions
import react.IntValue

import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.util.DestroyableBag
import tripleplay.util.TextConfig

import com.threerings.everything.data.ThingCard

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
  val headerFont = graphics.createFont("Helvetica", Font.Style.BOLD, 16);
  val tipFont = textFont(10)
  def textFont (size :Int) = graphics.createFont("Helvetica", Font.Style.PLAIN, size)

  val cardCfg = new TextConfig(textColor).withFont(textFont(12)) // TODO
  val statusCfg = new TextConfig(textColor).withFont(textFont(18)) // TODO

  def sheet = SimpleStyles.newSheet

  /** Returns a shim configured with an [AxisLayout] stretch constraint. */
  def stretchShim :Shim = AxisLayout.stretch(shim(1, 1))

  /** Creates a shim with the specified dimensions. */
  def shim (width :Float, height :Float) = new Shim(width, height)

  /** Creates a label that displays a currency amount. */
  def moneyIcon (coins :Int) = new Label(coins.toString, Icons.image(coinsIcon))

  /** Creates a label that displays a (reactive) currency amount. */
  def moneyIcon (coins :IntValue, dbag :DestroyableBag) :Label = {
    val label = moneyIcon(0)
    dbag.add(coins.map(Functions.TO_STRING).connectNotify(label.text.slot()))
    label
  }

  def headerLabel (text :String) = new Label(text).addStyles(Style.FONT.is(headerFont))
  def tipLabel (text :String) = new Label(text).addStyles(Style.FONT.is(tipFont))

  def getImage (path :String) = assets.getImageSync(s"images/$path")

  def cardImage (cache :ImageCache, card :ThingCard) = {
    val cardimg = graphics.createImage(cardFront.width, cardFront.height)
    cardimg.canvas.drawImage(cardFront, 0, 0)
    val title = cardCfg.layout(card.name)
    cardCfg.renderCX(cardimg.canvas, title, cardimg.width/2, 2)
    cache(card.image).addCallback(new Callback[Image] {
      def onSuccess (thing :Image) {
        // these are hardcoded because the image is asymmetric and has built-in shadow... blah.
        val scale = math.min(42/thing.width, 50/thing.height)
        val (swidth, sheight) = (thing.width*scale, thing.height*scale)
        val (sx, sy) = (math.round((64-swidth)/2), math.round((78-sheight)/2))
        cardimg.canvas.setFillColor(textColor).fillRect(sx-1, sy-1, swidth+2, sheight+2)
        cardimg.canvas.drawImage(thing, sx, sy, swidth, sheight)
      }
      def onFailure (cause :Throwable) {} // nada
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

  def imageButton (image :Image) = new Button(Icons.image(image)) {
    override def getStyleClass = classOf[Label]
  }
}
