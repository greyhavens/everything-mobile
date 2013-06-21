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
) extends CardScreen(game, cache, card, upStatus)  {

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

  override def createUI (root :Root) {
    val image = UI.frameImage(
      cache(card.thing.image), Thing.MAX_IMAGE_WIDTH/2, Thing.MAX_IMAGE_HEIGHT/2)
    val imgbtn = UI.imageButton(image) {
      new CardBackScreen(game, cache, card, counts, upStatus).replace()
    };
    val bits = new Group(AxisLayout.vertical.gap(0)).add(
      new Label(s"Rarity: ${card.thing.rarity} - E${card.thing.rarity.value}"))
    if (card.giver != null) bits.add(new Label(card.giver.name match {
      case null => "A birthday gift from Everything!"
      case name => s"A gift from ${card.giver}"
    }))
    counts match {
      case Some((haveCount, thingsRemaining)) =>
        bits.add(new Label(status(haveCount, thingsRemaining, card)))
      case None => // skip it
    }
    root.add(header(), imgbtn, bits, UI.stretchShim(), buttons())
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

  private var _msg :String = _
  private var _bubble :ImageLayer = _
}
