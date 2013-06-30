//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.Pointer
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

abstract class CardScreen (
  game :Everything, cache :UI.ImageCache, card :Card, counts :Option[(Int,Int)], source :CardButton
) extends EveryScreen(game) {

  override protected def layout () :Layout = AxisLayout.vertical().gap(0).offStretch

  override def wasAdded () {
    super.wasAdded()
    root.layer.setHitTester(UI.absorber)
    root.layer.addListener(new Pointer.Adapter {
      override def onPointerStart (event :Pointer.Event) = {
        // ignore events that hit a child layer but are being propagated back up to us
        if (event.hit == root.layer) onCardClick()
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
    UI.button("Sell") { maybeSellCard(card.toThingCard) {
      source.queueSell()
      pop()
    }},
    UI.stretchShim(),
    UI.button("Gift") { new GiftCardScreen(game, cache, card, source).push },
    UI.stretchShim(),
    UI.button("Share") { showShareDialog() },
    UI.stretchShim())

  protected def showShareDialog () {
    val (me, thing, everyURL) = (game.self.get.name, card.thing.name, game.sess.get.everythingURL)
    val (msg, ref, tgtId) =
      if (counts.map(_._2 == 0).getOrElse(false)) //  series completed
        (s"$me got the $thing card and completed the ${card.getSeries} series!", "got_comp", null)
      else if (card.giver == null)
        (s"$me got the $thing card", "got_card", null)
      else if (card.giver.userId == Card.BIRTHDAY_GIVER_ID)
        (s"$me got the $thing card as a birthday present!", "got_bgift", null)
      else
        (s"$me got the $thing from ${card.giver}.", "got_gift", card.giver.facebookId.toString)
    val toArgs = if (tgtId != null) Array("to", tgtId) else Array[String]()
    game.fb.showDialog("feed", toArgs ++ Array(
      "name", thing,
      "caption", msg,
      "description", card.thing.descrip,
      "link", everyURL,
      "picture", s"${game.sess.get.backendURL}cardimg?thing=${card.thing.thingId}",
      "actions", s"[ { 'name': 'Collect Everything!', 'link': '${everyURL}' } ]",
      "ref", ref)) // TODO: onSuccess, etc.?
  }
}
