//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

abstract class CardScreen (
  game :Everything, cache :UI.ImageCache, card :Card, upStatus :SlotStatus => Unit
) extends EveryScreen(game) {

  protected def header () = new Group(AxisLayout.vertical.gap(0)).add(
    UI.headerLabel(card.thing.name),
    UI.pathLabel(card.categories.map(_.name)),
    UI.tipLabel(s"${card.position+1} of ${card.things}"))

  protected def buttons () = UI.hgroup(
    UI.button("Keep")(pop()),
    UI.button("Sell") {
      maybeSellCard(card.toThingCard) { upStatus(SlotStatus.SOLD) }
    },
    UI.button("Gift") {
      new GiftCardScreen(game, cache, card, upStatus).push
    },
    UI.button("Share") {
      todo()
    })
}
