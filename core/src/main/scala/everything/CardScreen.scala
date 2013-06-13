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

class CardScreen (game :Everything, info :CardResult) extends EveryScreen(game) {

  override def createUI (root :Root) {
    root.add(new Label(info.card.thing.name),
             new Label(Category.getHierarchy(info.card.categories)),
             new Label(s"${info.card.position} of ${info.card.things}"),
             // TODO: tap image to flip card over
             AxisLayout.stretch(imageLabel(info.card.thing.image)),
             new Label(s"Rarity: ${info.card.thing.rarity} - E${info.card.thing.rarity.saleValue}"),
             new Label(status(info.haveCount, info.thingsRemaining, info.card)),
             new Group(AxisLayout.horizontal()).add(
               new Button("Sell"),
               new Button("Gift"),
               new Button("Share"),
               new Button("Keep").onClick(pop _)))

    // TODO: trophies!
  }

  def status (have :Int, remain :Int, card :Card) = {
    if (have > 1) s"You already have $have of these cards."
    else if (have > 0) "You already have this card."
    else if (remain == 1) "You only need one more card to complete this series!"
    else if (remain == 0) s"You have completed the ${card.getSeries.name} series!"
    else {
      val total = card.getSeries.things
      "You have ${total - remain} of $total ${card.getSeries.name}."
    }
  }

  def imageLabel (hash :String) = {
    val label = new Button {
      override def getStyleClass = classOf[Label]
    }
    label.onClick(unitSlot {
      game.screens.push(new CardBackScreen(game, info.card), game.screens.flip)
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
