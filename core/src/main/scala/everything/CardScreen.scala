//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import com.threerings.everything.data._

abstract class CardScreen (
  game :Everything, cache :UI.ImageCache, card :Card, upStatus :SlotStatus => Unit
) extends EveryScreen(game) {

  protected def buttons () = UI.hgroup(gap=15).add(
    UI.button("Keep")(pop()),
    UI.button("Sell") {
      maybeSellCard(card.toThingCard) { upStatus(SlotStatus.SOLD) }
    },
    UI.button("Gift") {
      new GiftCardScreen(game, cache, card, upStatus).push
    },
    UI.button("Share") {
      // TODO
    })
}
