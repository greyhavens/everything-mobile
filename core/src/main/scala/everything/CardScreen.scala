//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core._
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._
import com.threerings.everything.rpc.GameAPI._

class CardScreen (game :Everything, cache :UI.ImageCache, card :Card, counts :Option[(Int,Int)],
                  upStatus :SlotStatus => Unit) extends EveryScreen(game) {

  def this (game :Everything, cache :UI.ImageCache, info :CardResult, upStatus :SlotStatus => Unit) =
    this(game, cache, info.card, Some(info.haveCount -> info.thingsRemaining), upStatus)

  override def createUI (root :Root) {
    val image = UI.frameImage(cache(card.thing.image), Thing.MAX_IMAGE_WIDTH/2, Thing.MAX_IMAGE_HEIGHT/2)
    root.add(UI.stretchShim(),
             UI.headerLabel(card.thing.name),
             UI.tipLabel(Category.getHierarchy(card.categories)),
             UI.tipLabel(s"${card.position+1} of ${card.things}"),
             UI.imageButton(image).onClick(unitSlot {
               game.screens.push(new CardBackScreen(game, card), game.screens.flip.duration(400))
             }),
             new Label(s"Rarity: ${card.thing.rarity} - E${card.thing.rarity.value}"))
    counts match {
      case Some((haveCount, thingsRemaining)) =>
        root.add(new Label(status(haveCount, thingsRemaining, card)))
      case None => // skip it
    }
    root.add(UI.stretchShim(),
             UI.hgroup(gap=15).add(
               UI.button("Keep")(pop()),
               UI.button("Sell") {
                 maybeSellCard(card.toThingCard) { upStatus(SlotStatus.SOLD) }
               },
               UI.button("Gift") {
                 new GiftCardScreen(game, cache, card, upStatus).push
               },
               UI.button("Share") {
                 // TODO
               }))

    // TODO: trophies!
  }

  def status (have :Int, remain :Int, card :Card) = {
    if (have > 1) s"You already have $have of these cards."
    else if (have > 0) "You already have this card."
    else if (remain == 1) "You only need one more card to complete this series!"
    else if (remain == 0) s"You have completed the ${card.getSeries.name} series!"
    else {
      val total = card.getSeries.things
      s"You have ${total - remain} of $total ${card.getSeries.name}."
    }
  }
}
