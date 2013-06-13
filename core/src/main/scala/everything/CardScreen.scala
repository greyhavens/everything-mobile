//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN.assets
import playn.core._
import playn.core.util.Callback
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data.{Card, Category, Rarity}

class CardScreen (game :Everything, card :Card) extends EveryScreen(game) {

  override def createUI (root :Root) {
    // could also be "You already have this card"
    val status = "You have 5 of 10 " + card.getSeries.name
    root.add(new Label(card.thing.name),
             new Label(Category.getHierarchy(card.categories)),
             new Label(s"${card.position} of ${card.things}"),
             // TODO: tap image to flip card over
             AxisLayout.stretch(imageLabel(card.thing.image)),
             new Label(s"Rarity: ${card.thing.rarity} - E${card.thing.rarity.saleValue}"),
             new Label(status),
             new Group(AxisLayout.horizontal()).add(
               new Button("Sell"),
               new Button("Gift"),
               new Button("Share"),
               new Button("Keep")))
  }

  def imageLabel (hash :String) = {
    val label = new Button {
      override def getStyleClass = classOf[Label]
    }
    label.onClick(unitSlot {
      game.screens.push(new CardBackScreen(game, card), game.screens.flip)
    })
    val image = assets.getRemoteImage( // TODO: more proper
      s"http://s3.amazonaws.com/everything.threerings.net/${hash}.jpg")
    image.addCallback(new Callback[Image] {
      def onSuccess (image :Image) {
        label.icon.update(Icons.image(image))
      }
      def onFailure (cause :Throwable) {
        cause.printStackTrace(System.err) // TODO!
      }
    })
    label
  }
}
