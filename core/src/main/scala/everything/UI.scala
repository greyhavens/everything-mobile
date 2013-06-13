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
import tripleplay.util.TextConfig

object UI {

  val coinsIcon = getImage("money.png")

  val titleFont = graphics.createFont("Helvetica", Font.Style.BOLD, 48)
  val menuFont = graphics.createFont("Helvetica", Font.Style.BOLD, 24)

  def sheet = SimpleStyles.newSheet

  /** Returns a shim configured with an [AxisLayout] stretch constraint. */
  def stretchShim :Shim = AxisLayout.stretch(shim(1, 1))

  /** Creates a shim with the specified dimensions. */
  def shim (width :Float, height :Float) = new Shim(width, height)

  /** Creates a label that displays a currency amount. */
  def moneyIcon (currency :IntValue) = {
    val label = new Label(coinsIcon)
    currency.map(Functions.TO_STRING).connectNotify(label.text.slot())
    label
  }

  def getImage (path :String) = assets.getImage(s"images/$path")

  def imageButton (image :Image) = new Button(Icons.image(image)) {
    override def getStyleClass = classOf[Label]
  }
}
