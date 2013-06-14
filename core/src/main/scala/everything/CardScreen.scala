//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core._
import playn.core.util.Callback
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

class CardScreen (game :Everything, cache :UI.ImageCache, info :CardResult,
                  upStatus :SlotStatus => Unit) extends EveryScreen(game) {

  override def createUI (root :Root) {
    root.add(new Label(info.card.thing.name),
             new Label(Category.getHierarchy(info.card.categories)),
             new Label(s"${info.card.position} of ${info.card.things}"),
             // TODO: tap image to flip card over
             AxisLayout.stretch(imageLabel(info.card.thing.image)),
             new Label(s"Rarity: ${info.card.thing.rarity} - E${info.card.thing.rarity.value}"),
             new Label(status(info.haveCount, info.thingsRemaining, info.card)),
             new Group(AxisLayout.horizontal().gap(15)).add(
               new Button("Sell").onClick(maybeSellCard _),
               new Button("Gift").onClick(giftCard _),
               new Button("Share").onClick(shareCard _),
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
      game.screens.push(new CardBackScreen(game, info.card), game.screens.flip.duration(400))
    })
    cache(hash).addCallback(new Callback[Image] {
      def onSuccess (image :Image) {
        label.icon.update(Icons.image(image))
      }
      def onFailure (cause :Throwable) {
        cause.printStackTrace(System.err) // TODO!
      }
    })
    label
  }

  protected def maybeSellCard (btn :Button) {
    val amount = info.card.thing.rarity.saleValue
    new Dialog(s"Sell Card", s"Sell ${info.card.thing.name} for E $amount") {
      override def okLabel = "Yes"
      override def cancelLabel = "No"
    }.onOK(sellCard).display()
  }

  protected def sellCard () {
    game.gameSvc.sellCard(info.card.thing.thingId, info.card.received.getTime).onFailure(onFailure).
      onSuccess(slot[(Int,Option[Boolean])] {
        case (coins, like) =>
          game.coins.update(coins)
          val catId = info.card.getSeries.categoryId
          like match {
            case Some(like) => game.likes.put(catId, like)
            case None => game.likes.remove(catId)
          }
          upStatus(SlotStatus.SOLD)
          pop()
      })
  }

  protected def giftCard (btn :Button) {
  }

  protected def shareCard (btn :Button) {
  }
}
