//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.Pointer
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

abstract class CardScreen (
  game :Everything, cache :UI.ImageCache, card :Card, counts :Option[(Int,Int)],
  upStatus :SlotStatus => Unit
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
      showShareDialog()
    },
    UI.stretchShim())

  protected def showShareDialog () {
    val (me, thing) = (game.self.get.name, card.thing.name)
    val (msg, ref, tgtId) =
      if (counts.map(_._2 == 0).getOrElse(false)) //  series completed
        (s"$me got the $thing card and completed the ${card.getSeries} series!", "got_comp", null)
      else if (card.giver == null)
        (s"$me got the $thing card", "got_card", null)
      else if (card.giver.userId == Card.BIRTHDAY_GIVER_ID)
        (s"$me got the $thing card as a birthday present!", "got_bgift", null)
      else
        (s"$me got the $thing from ${card.giver}.", "got_gift", card.giver.facebookId.toString)
    val imageURL = s"${game.sess.get.backendURL}cardimg?thing=${card.thing.thingId}"
    game.fb.showCardDialog(ref, msg, thing, card.thing.descrip, imageURL,
                           Category.getHierarchy(card.categories), card.thing.rarity.toString,
                           game.sess.get.everythingURL, tgtId) // TODO: onSuccess, etc.?
  }
}
