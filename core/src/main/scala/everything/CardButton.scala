//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.RFuture
import tripleplay.ui._

import com.threerings.everything.data._
import com.threerings.everything.rpc.GameAPI

class CardButton (game :Everything, host :EveryScreen, cache :UI.ImageCache) extends Button {

  protected var _card :ThingCard = _
  protected var _msg :String = null
  protected var _counts :Option[(Int,Int)] = None
  protected var _cachedCard :Card = _

  addStyles(Style.ICON_POS.above)
  update(SlotStatus.UNFLIPPED, null)

  def update (status :SlotStatus, card :ThingCard) = {
    _card = card
    upStatus(status)
    this
  }

  override def getStyleClass = classOf[Label]

  override def click () {
    super.click()
    if (isRevealed(_card)) onView()
    else onReveal()
  }

  protected def isRevealed (card :ThingCard) = card != null && card.image != null

  protected def isGift = false

  /** Called if an unflipped card is clicked. */
  protected def onReveal () {
    // default to NOOP
  }

  /** Called if an already-flipped card is clicked. */
  protected def onView () {
    if (_cachedCard != null) viewCard(_cachedCard)
    else game.gameSvc.getCard(new CardIdent(game.self.get.userId, _card.thingId, _card.received)).
      bindComplete(enabledSlot). // disable while req is in-flight
      onFailure(host.onFailure).
      onSuccess(viewCard _)
  }

  protected def reveal (res :GameAPI.GiftResult) {
    _msg = res.message
    reveal(res :GameAPI.CardResult)
  }

  protected def reveal (res :GameAPI.CardResult) {
    // TODO: animate the flip, etc.
    update(SlotStatus.FLIPPED, res.card.toThingCard)
    _counts = Some((res.haveCount, res.thingsRemaining))
    viewCard(res.card)
  }

  protected def viewCard (card :Card) {
    _cachedCard = card
    new CardFrontScreen(game, cache, card, _counts, upStatus _).setMessage(_msg).push()
  }

  protected def upStatus (status :SlotStatus) {
    def dispensed (msg :String) = {
      // TODO: swap out old icon in puff of smoke or something
      setEnabled(false)
      _card = null
      UI.statusImage(msg)
    }
    import SlotStatus._
    icon.update(Icons.image(status match {
      case        GIFTED|
          RECRUIT_GIFTED => dispensed("Gifted!")
      case          SOLD => dispensed("Sold!")
      case     UNFLIPPED => if (isGift) UI.cardGift else UI.cardBack
      case       FLIPPED => UI.cardImage(cache, _card)
    }))
  }
}
