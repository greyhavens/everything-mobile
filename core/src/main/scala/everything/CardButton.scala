//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.FloatMath
import react.{RFuture, Value}
import scala.util.Random
import tripleplay.anim.Animation
import tripleplay.shaders.RotateYShader
import tripleplay.ui._

import com.threerings.everything.data._
import com.threerings.everything.rpc.GameAPI

class CardButton (game :Everything, host :EveryScreen, cache :UI.ImageCache)
    extends SizableWidget(UI.cardSize) {
  import CardButton._

  /** The layer that contains our card image. */
  val ilayer = graphics.createImageLayer()
  ilayer.setOrigin(UI.cardCtr.x, UI.cardCtr.y)
  ilayer.setAlpha(0) // start out invisible
  layer.addAt(ilayer, UI.cardCtr.x, UI.cardCtr.y)

  /** Whether or not this card is currently jiggling. We jiggle the card while waiting for data after
    * the user has requested to flip it. */
  val shaking = Value.create(false)
  shaking.connect(slot { isShaking =>
    if (isShaking && _shaker == null) {
      _shaker = host.iface.animator.
        shake(ilayer).bounds(-1, 1, -1, 1).cycleTime(50).in(60000).handle
    } else if (!isShaking && _shaker != null) {
      ilayer.setTranslation(UI.cardCtr.x, UI.cardCtr.y)
      _shaker.cancel()
      _shaker = null
    }
  })

  enableInteraction()
  update(SlotStatus.UNFLIPPED, null)

  protected var _card :ThingCard = _
  protected var _msg :String = null
  protected var _counts :Option[(Int,Int)] = None
  protected var _cachedCard :Card = _
  protected var _shaker :Animation.Handle = _
  protected var _entree :Entree = CardButton.fadeIn

  def update (status :SlotStatus, card :ThingCard) = {
    _card = card
    upStatus(status)
    this
  }

  /** Configures this button's animated entree based on the supplied random seed. */
  def entree (entree :Entree) = { _entree = entree ; this }

  /** Whether or not this card has been revealed. */
  protected def isRevealed (card :ThingCard) = card != null && card.image != null

  /** Whether or not to use the gift card background. */
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
    // disable interaction during the reveal animation
    setEnabled(false)
    // wait until our thing image is ready, then start the flip
    cache(res.card.thing.image).addCallback(cb { thing =>
      shaking.update(false) // we can stop shaking now

      // add another layer over our current one which will display the back of the card
      val blayer = graphics.createImageLayer(ilayer.image)
      layer.add(blayer.setDepth(1)) // render above our current layer

      // create the rotating shaders; this is a bit hacky because the rotating shaders work in
      // fractions of full screen coordinates, so we have to figure out where (in screen coordinates)
      // the center of our card is... meh
      val pos = Layer.Util.layerToScreen(layer, UI.cardCtr.x, UI.cardCtr.y)
      pos.x /= host.width
      pos.y = 0.5f // pos.y /= host.height
      val topShader = new RotateYShader(graphics.ctx, pos.x, pos.y, 1)
      blayer.setShader(topShader)
      val botShader = new RotateYShader(graphics.ctx, pos.x, pos.y, 1)
      ilayer.setShader(botShader)

      // run up our flipping animation
      host.iface.animator.tween(new Animation.Value() {
        def initial = 0
        def set (pct :Float) = {
          topShader.angle = FloatMath.PI * pct;
          botShader.angle = FloatMath.PI * (pct - 1);
          if (pct >= 0.5f && !_flipped) {
            blayer.setDepth(-1);
            _flipped = true
          }
        }
        var _flipped = false
      }).to(1).in(300).`then`.action(new Runnable() {
        def run () {
          blayer.setShader(null)
          ilayer.setShader(null)
          blayer.destroy()
          setEnabled(true) // reenable interaction
          viewCard(res.card)
        }
      })

      // update our main image to be the new card (this will be flipped in)
      update(SlotStatus.FLIPPED, res.card.toThingCard)
      _counts = Some((res.haveCount, res.thingsRemaining))
    })
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
    ilayer.setImage(status match {
      case        GIFTED|
          RECRUIT_GIFTED => dispensed("Gifted!")
      case          SOLD => dispensed("Sold!")
      case     UNFLIPPED => if (isGift) UI.cardGift else UI.cardBack
      case       FLIPPED => UI.cardImage(cache, _card)
    })
  }

  // our various entrees
  protected def fx () = new FX(host, ilayer).delay(10, 100)

  override protected def getStyleClass = classOf[CardButton]

  override protected def layout () {
    super.layout()
    // the first time we're properly positioned, perform our dramatic entrance
    if (_entree != null) {
      _entree(this)
      _entree = null
    }
  }

  override protected def onClick (event :Pointer.Event) {
    if (isRevealed(_card)) onView()
    else onReveal()
  }
}

object CardButton {
  type Entree = CardButton => Unit

  val fadeIn :Entree = _.fx().fadeIn(300)
  val flyIn  :Entree = _.fx().fadeIn(1).flyIn(500)
  val dropIn :Entree = _.fx().fadeIn(1).dropIn(2, 500)
  val popIn :Entree = _.fx().fadeIn(1).popIn(500)

  def randomEntree () :Entree = {
    val entrees = Seq[Entree](fadeIn, flyIn, dropIn, popIn)
    entrees(Random.nextInt(entrees.size))
  }
}
