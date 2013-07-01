//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.{FloatMath, Point}
import react.{RFuture, Value}
import scala.util.Random
import tripleplay.anim.Animation
import tripleplay.shaders.RotateYShader
import tripleplay.ui._

import com.threerings.everything.data._
import com.threerings.everything.rpc.GameAPI

class CardButton (
  game :Everything, host :EveryScreen, cache :UI.ImageCache, cardsEnabled :Value[JBoolean]
) extends SizableWidget(UI.cardSize) {
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
  bindEnabled(cardsEnabled)
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

  /** Queues this card up to be sold. This may be processed immediately if the card is on screen, or
    * it will be deferred until the screen containing this card re-revealed. */
  def queueSell () {
    if (host.isVisible.get) sell()
    else host.onShown.connect(unitSlot { sell() }).once()
  }

  /** Queues this card up to be gifted. See [queueSell] for details on when this will happen. */
  def queueGift (friend :PlayerName, msg :String) {
    if (host.isVisible.get) gift(friend, msg)
    else host.onShown.connect(unitSlot { gift(friend, msg) }).once()
  }

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
      bindComplete(cardsEnabled.slot). // disable cards while req is in-flight
      onFailure(host.onFailure).
      onSuccess(viewCard _)
  }

  protected def reveal (res :GameAPI.GiftResult) {
    _msg = res.message
    reveal(res :GameAPI.CardResult)
  }

  protected def reveal (res :GameAPI.CardResult) {
    // disable card interaction during the reveal animation
    cardsEnabled.update(false)
    // wait until our thing image is ready, then start the flip
    cache(res.card.thing.image).addCallback(cb { thing =>
      shaking.update(false) // we can stop shaking now
      // flip the card over (use the current image as the old image for the flip)
      animateFlip(ilayer.image)(viewCard(res.card))
      // update our image to be the new card (this will be flipped in)
      update(SlotStatus.FLIPPED, res.card.toThingCard)
      _counts = Some((res.haveCount, res.thingsRemaining))
      // once the flip animation completes, viewCard will be called
    })
  }

  protected def animateFlip (oldImage :Image)(afterFlip : =>Unit) {
    // add another layer over our current one which will display the back of the card
    val blayer = graphics.createImageLayer(oldImage)
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
        afterFlip
      }
    })
  }

  protected def viewCard (card :Card) {
    _cachedCard = card
    cardsEnabled.update(true) // reenable card interaction
    new CardFrontScreen(game, cache, card, _counts, this).setMessage(_msg).push()
  }

  protected def sell () {
    shaking.update(true)
    game.gameSvc.sellCard(_card.thingId, _card.received).onFailure(host.onFailure).
      onSuccess(slot { res =>
        shaking.update(false)
        game.coins.update(res.coins)
        val catId = _card.categoryId
        res.newLike match {
          case null => game.likes.remove(catId)
          case like => game.likes.put(catId, like)
        }
        // TODO: shatter old image into pieces, turn those into coins, then fly the coins up to
        // the money label on our host screen (or ??? if it has no money label?)
        upStatus(SlotStatus.SOLD)
      })
  }

  protected def gift (friend :PlayerName, msg :String) {
    setEnabled(false)
    // issue our service call to gift the card, but don't process the result just yet
    val result = game.gameSvc.giftCard(_card.thingId, _card.received, friend.userId, msg)
    // animate the card flipping back over to the gift back, then handle the service result
    animateFlip(ilayer.image) {
      // TODO: instead of flipping directly to gift card back, flip to normal card back then
      // animate a bow wrapping around the card
      result.onFailure(host.onFailure).onSuccess(unitSlot {
        // create a copy of the card back image and animate it flying off the screen
        val fly = graphics.createImageLayer(ilayer.image)
        fly.setDepth(10) // render above everything else on screen
        fly.setOrigin(fly.width/2, fly.height/2)
        val spos = Layer.Util.layerToParent(ilayer, host.layer, fly.originX, fly.originY)
        host.layer.addAt(fly, spos.x, spos.y)
        val (swidth, sheight) = (host.width, host.height)
        val dpos = Random.nextInt(4) match {
          case 0 => new Point(swidth/2,           -fly.originY)
          case 1 => new Point(swidth/2,           sheight+fly.originY)
          case 2 => new Point(-fly.originX,       sheight/2)
          case 3 => new Point(swidth+fly.originX, sheight/2)
        }
        host.iface.animator.tweenXY(fly).to(dpos).in(500).easeIn.`then`.destroy(fly)
        // now update our underlying layer to show "Gifted"
        upStatus(SlotStatus.GIFTED)
      })
      // TODO: restore original image and reenable on failure
    }
    ilayer.setImage(UI.cardGift)
  }

  protected def upStatus (status :SlotStatus) {
    def dispensed (msg :String) = {
      _card = DISPENSED
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
    else if (_card != DISPENSED) onReveal()
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

  private val DISPENSED = new ThingCard
}
