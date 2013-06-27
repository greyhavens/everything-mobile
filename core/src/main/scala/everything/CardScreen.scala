//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.Pointer
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

abstract class CardScreen (
  game :Everything, cache :UI.ImageCache, card :Card, upStatus :SlotStatus => Unit
) extends EveryScreen(game) {

  override protected def layout () :Layout = AxisLayout.vertical().gap(0).offStretch

  override def createUI (root :Root) {
    root.layer.setHitTester(UI.absorber)
    root.layer.addListener(new Pointer.Adapter {
      override def onPointerStart (event :Pointer.Event) {
        onCardClick()
      }
    })
  }

  protected def onCardClick () :Unit

  protected def addHeader (root :Root) = root.add(
    UI.pathLabel(card.categories.map(_.name)),
    UI.headerLabel(card.thing.name),
    UI.tipLabel(s"${card.position+1} of ${card.things}"))

  protected def buttons (keepNotBack :Boolean) = UI.hgroup(
    UI.stretchShim(),
    back(if (keepNotBack) "Keep" else "Back"),
    UI.stretchShim(),
    UI.button("Sell") {
      maybeSellCard(card.toThingCard) { upStatus(SlotStatus.SOLD) }
    },
    UI.stretchShim(),
    UI.button("Gift") {
      new GiftCardScreen(game, cache, card, upStatus).push
    },
    UI.stretchShim(),
    UI.button("Share") {
      todo()
    },
    UI.stretchShim())
}
