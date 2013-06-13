//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core._
import playn.core.PlayN._

import react.Functions
import react.IntValue

import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.util.DestroyableBag
import tripleplay.util.TextConfig

import com.threerings.everything.data.ThingCard

object UI {

  val coinsIcon = getImage("money.png")
  val cardFront = getImage("card_front.png")
  val cardBack = getImage("card_back.png")
  val cardGift = getImage("card_gift.png")

  val titleFont = graphics.createFont("Helvetica", Font.Style.BOLD, 48)
  val menuFont = graphics.createFont("Helvetica", Font.Style.BOLD, 24)
  def textFont (size :Int) = graphics.createFont("Helvetica", Font.Style.PLAIN, size)

  val cardCfg = new TextConfig(0xFF000000).withFont(textFont(12)) // TODO

  def sheet = SimpleStyles.newSheet

  /** Returns a shim configured with an [AxisLayout] stretch constraint. */
  def stretchShim :Shim = AxisLayout.stretch(shim(1, 1))

  /** Creates a shim with the specified dimensions. */
  def shim (width :Float, height :Float) = new Shim(width, height)

  /** Creates a label that displays a currency amount. */
  def moneyIcon (coins :IntValue, dbag :DestroyableBag) = {
    val label = new Label(coinsIcon)
    dbag.add(coins.map(Functions.TO_STRING).connectNotify(label.text.slot()))
    label
  }

  def getImage (path :String) = assets.getImage(s"images/$path")

  def cardImage (card :ThingCard) = {
    val image = graphics.createImage(cardFront.width, cardFront.height)
    image.canvas.drawImage(cardFront, 0, 0)
    val title = cardCfg.layout(card.name)
    cardCfg.renderCX(image.canvas, title, image.width/2, 2)
    // TODO: image, rarity
    image
  }

  def imageButton (image :Image) = new Button(Icons.image(image)) {
    override def getStyleClass = classOf[Label]
  }
}
