//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.JavaConversions._

import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.ui.layout.TableLayout

class OpenGiftsScreen (game :Everything) extends EveryScreen(game) {

  val cache = new UI.ImageCache

  override def createUI (root :Root) {

    val COLS = 4
    val cards = new Group(new TableLayout(COLS).gaps(10, 10))
    game.gifts.foreach { card =>
      val btn = UI.imageButton(UI.cardGift)
      btn.clicked.connect(unitSlot {
        // TODO: spinner
        game.gameSvc.openGift(card.thingId, card.received).onFailure(onFailure).
          onSuccess(slot { res =>
            btn.icon.update(Icons.image(UI.cardImage(cache, res.card.toThingCard)))
            btn.clicked.connect(unitSlot {
              new CardScreen(game, cache, res, UI.statusUpper(btn)).push
            })
            btn.click()
          })
      }).once
      cards.add(btn)
    }

    root.add(UI.hgroup(UI.button("Back")(pop()),
                       AxisLayout.stretch(UI.headerLabel("Open Your Gifts!"))),
             AxisLayout.stretch(cards))
  }
}
