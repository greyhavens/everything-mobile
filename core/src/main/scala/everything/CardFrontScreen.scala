//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core._
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

class CardFrontScreen (
  game :Everything, cache :UI.ImageCache, card :Card, counts :Option[(Int,Int)],
  upStatus :SlotStatus => Unit
) extends CardScreen(game, cache, card, counts, upStatus)  {

  def setMessage (msg :String) = {
    _msg = msg
    this
  }

  override def showTransitionCompleted () {
    super.showTransitionCompleted()

    // if we have a message from this giver, show it here
    if (_msg != null && _bubble == null) {
      _bubble = new Bubble(_msg, 210).above().tail(-75, 30).at(width-70, height-90).toLayer(this)
      _bubble.setScale(0.1f)
      iface.animator.tweenScale(_bubble).to(1f).in(200)
      layer.add(_bubble)
    }
  }

  override def createUI () {
    addHeader(root)
    val image = UI.frameImage(
      cache(card.thing.image), Thing.MAX_IMAGE_WIDTH/2, Thing.MAX_IMAGE_HEIGHT/2)
    root.add(UI.stretchShim(),
             UI.icon(image).addStyles(Style.ICON_POS.above),
             UI.shim(5, 5),
             UI.hgroup(UI.subHeaderLabel(s"Rarity: ${card.thing.rarity}"), UI.shim(15, 5),
                       UI.moneyIcon(card.thing.rarity.value)))
    if (card.giver != null) root.add(new Label(card.giver.name match {
      case null => "A birthday gift from Everything!"
      case name => s"A gift from ${card.giver}"
    }))
    counts match {
      case Some((haveCount, thingsRemaining)) =>
        root.add(status(haveCount, thingsRemaining, card))
      case None => // skip it
    }
    root.add(UI.stretchShim(), buttons(counts.isDefined))
  }

  override def onCardClick () {
    new CardBackScreen(game, cache, card, counts, upStatus).replace()
  }

  def status (have :Int, remain :Int, card :Card) :Element[_] = {
    if (have > 1) new Label(s"You already have $have of these cards.")
    else if (have > 0) new Label("You already have this card.")
    else if (remain == 1) new Label("You only need one more card to complete this series!")
    else if (remain == 0) new Label(s"You have completed the ${card.getSeries.name} series!")
    else {
      val cat = card.getSeries
      UI.hgroup(new Label(s"You have ${cat.things - remain} of ${cat.things}"),
                UI.labelButton(cat.name) {
                  new SeriesScreen(game, game.self.get, card.categories.map(_.name),
                                   cat.categoryId).push()
                })
    }
  }

  private var _msg :String = _
  private var _bubble :ImageLayer = _
}
