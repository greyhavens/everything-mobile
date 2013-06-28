//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.JavaConversions._

import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.ui.layout.TableLayout

class OpenGiftsScreen (game :Everything) extends EveryScreen(game) {

  val cache = new UI.ImageCache(game)

  override def createUI (root :Root) {
    val cards = new Group(new TableLayout(4).gaps(10, 10))
    game.gifts.foreach { card =>
      cards.add(new CardButton(game, this, cache) {
        override protected def isGift = true
        override protected def onReveal () {
          shaking.update(true)
          game.gameSvc.openGift(card.thingId, card.received).onFailure(onFailure).
            onSuccess(slot { res =>
              game.gifts.remove(card)
              reveal(res)
            })
        }
      })
    }

    root.add(header("Open Your Gifts!"), AxisLayout.stretch(cards))
  }
}
