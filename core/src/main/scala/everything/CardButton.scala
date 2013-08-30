//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.{FloatMath, Point}
import react.{RFuture, Value, Slot}
import scala.util.Random
import tripleplay.anim.Animation
import tripleplay.shaders.RotateYShader
import tripleplay.ui._

import com.threerings.everything.data._
import com.threerings.everything.rpc.GameAPI

class CardButton (
  game :Everything, host :EveryScreen, cache :UI.ImageCache, rendo :UI.CardRenderer,
  enabled :Value[JBoolean]
) extends SizableWidget[CardButton](rendo.size) {
  import CardButton._

  /** The layer that contains our card image. */
  val ilayer = graphics.createImageLayer()
  ilayer.setOrigin(rendo.ctr.x, rendo.ctr.y)
  ilayer.setAlpha(0) // start out invisible
  layer.addAt(ilayer, rendo.ctr.x, rendo.ctr.y)

  /** Whether or not this card is currently jiggling. We jiggle the card while waiting for data after
    * the user has requested to flip it. */
  val shaking = Value.create(false)
  shaking.connect(new Slot[Boolean]() {
    def onEmit (isShaking :Boolean) {
      if (isShaking && _shaker == null) {
        _shaker = host.iface.animator.
          shake(ilayer).bounds(-1, 1, -1, 1).cycleTime(50).in(60000).handle
      } else if (!isShaking && _shaker != null) {
        ilayer.setTranslation(rendo.ctr.x, rendo.ctr.y)
        _shaker.cancel()
        _shaker = null
      }
    }
    var _shaker :Animation.Handle = _
  })

  /** Provides access to our card. */
  lazy val cardF :RFuture[Card] = _card match {
    case null => game.gameSvc.getCard(new CardIdent(_ownerId, _tcard.thingId, _tcard.received)).
        bindComplete(enabled.slot). // disable cards while req is in-flight
        onFailure(host.onFailure)
    case card => RFuture.success(card)
  }

  bindEnabled(enabled)
  upStatus(SlotStatus.UNFLIPPED)

  var counts :Option[(Int,Int)] = None

  protected var _ownerId = 0
  protected var _card :Card = _
  protected var _tcard :ThingCard = _
  protected var _tcardp :ThingCardPlus = _
  protected var _msg :String = null
  protected var _entree :Entree = CardButton.fadeIn

  /** Whether or not to show a link to the series page. */
  def showSeriesLink = true

  /** Whether or not we'll respond to a [viewNext] call. */
  def canViewNext = false

  def update (status :SlotStatus, ownerId :Int, cardp :ThingCardPlus) :this.type = {
    _tcardp = cardp
    update(status, ownerId, cardp.thing)
  }

  def update (status :SlotStatus, ownerId :Int, card :ThingCard) :this.type = {
    _ownerId = ownerId
    _tcard = card
    upStatus(status)
    this
  }

  def view (target :CardScreen, dir :Swipe.Dir) {
    if (_tcardp != null) viewCard(target, dir)
    else cardF.onSuccess(slot { c => viewCard(c, target, dir) })
  }

  /** Requests that the next (or previous depending on `dir`) card in the series be shown. */
  def viewNext (target :CardScreen, dir :Swipe.Dir) {} // noop!

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

  /** Whether or not to use the gift card background. */
  protected def isGift = false

  /** Called if an unflipped card is clicked. */
  protected def onReveal () {
    // default to NOOP
  }

  /** Called if an already-flipped card is clicked. */
  protected def onView () {
    view(null, null)
  }

  protected def reveal (res :GameAPI.GiftResult) {
    _msg = res.message
    reveal(res :GameAPI.CardResult)
  }

  protected def reveal (res :GameAPI.CardResult) {
    // disable card interaction during the reveal animation
    enabled.update(false)
    // note that we got all of our card data in reveal
    _card = res.card
    // wait until our thing image is ready, then start the flip
    cache(res.card.thing.image).addCallback(cb { thing =>
      shaking.update(false) // we can stop shaking now
      // flip the card over (use the current image as the old image for the flip)
      animateFlip(ilayer.image)(viewCard(res.card, null, null))
      // update our image to be the new card (this will be flipped in)
      update(SlotStatus.FLIPPED, res.card.owner.userId, new ThingCardPlus(res.card))
      this.counts = Some((res.haveCount, res.thingsRemaining))
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
    val pos = Layer.Util.layerToScreen(layer, rendo.ctr.x, rendo.ctr.y)
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

  protected def viewCard (card :Card, target :CardScreen, dir :Swipe.Dir) {
    _tcardp = new ThingCardPlus(card)
    viewCard(target, dir)
  }

  protected def viewCard (target :CardScreen, dir :Swipe.Dir) {
    enabled.update(true) // reenable card interaction
    val screen = if (target == null) new CardScreen(game, cache) else target
    screen.update(_tcardp, this, dir).setMessage(_msg)
    if (target == null) screen.push()
  }

  protected def sell () {
    shaking.update(true)
    game.gameSvc.sellCard(_tcard.thingId, _tcard.received).onFailure(host.onFailure).
      onSuccess(slot { res =>
        shaking.update(false)
        val catId = _tcard.categoryId
        res.newLike match {
          case null => game.likes.remove(catId)
          case like => game.likes.put(catId, like)
        }

        // shatter the old image into pieces, crossfade those into coins and then fly them up to
        // the money label
        val pl = host.purseLabel
        val tgtPos = if (pl.isAdded()) host.pos(pl.layer).addLocal(pl.size.width/2, pl.size.height/2)
                     else new Point(host.width/2, -10)
        val cimage = ilayer.image
        val (xparts, yparts) = saleFrags(_tcard.rarity.ordinal)
        val (fwidth, fheight) = (cimage.width/xparts, cimage.height/yparts)
        val cardPos = Layer.Util.layerToParent(ilayer, host.layer, 0, 0)
        for (yy <- 0 until yparts ; xx <- 0 until xparts) {
          val (xoff, yoff) = (xx*fwidth, yy*fheight)
          val fimage = cimage.subImage(xoff, yoff, fwidth, fheight)
          val flayer = graphics.createImageLayer(fimage)
          flayer.setOrigin(fwidth/2, fheight/2)
          val clayer = graphics.createImageLayer(UI.coinsIcon)
          clayer.setOrigin(clayer.width/2, clayer.height/2)
          clayer.setAlpha(0)
          val center = new Point(xoff + fwidth/2, yoff + fheight/2)
          val startPos = cardPos.add(center.x, center.y)
          val delta = center.add(-cimage.width/2, -cimage.height/2)
          val expPos = startPos.subtract(delta.x/4, // + Random.nextFloat()*delta.x/8,
                                         delta.y/4) // + Random.nextFloat()*delta.y/8)
          host.layer.addAt(flayer, startPos.x, startPos.y)
          host.layer.addAt(clayer, startPos.x, startPos.y)
          val expTime = 200
          // move the fragment to the "exploded" position, while fading it out
          host.iface.animator.tweenAlpha(flayer).to(0).in(expTime)
          host.iface.animator.tweenXY(flayer).to(expPos).in(expTime).easeOutBack.
            `then`.destroy(flayer)
          // move the coin to the exploded position while fading it in
          host.iface.animator.tweenAlpha(clayer).to(1).in(expTime)
          host.iface.animator.tweenXY(clayer).to(expPos).in(expTime).easeOutBack.
            `then`.delay(200+Random.nextInt(200)).
            `then`.tweenXY(clayer).to(tgtPos).in(500).easeIn.
            `then`.destroy(clayer)
          // host.iface.animator.tweenRotation(flayer).to(4*FloatMath.PI).in(1500)
        }

        // wait for the sell card animation to complete, then update our coin coint
        host.iface.animator.addBarrier()
        host.iface.animator.action(new Runnable() {
          def run = game.coins.update(res.coins)
        })

        // TODO: shatter old image into pieces, turn those into coins, then fly the coins up to
        // the money label on our host screen (or ??? if it has no money label?)
        upStatus(SlotStatus.SOLD)
      })
  }

  protected def gift (friend :PlayerName, msg :String) {
    setEnabled(false)
    // issue our service call to gift the card, but don't process the result just yet
    val result = game.gameSvc.giftCard(_tcard.thingId, _tcard.received, friend.userId, msg)
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
    ilayer.setImage(rendo.gift)
  }

  protected def upStatus (status :SlotStatus) {
    def dispensed (msg :String) = {
      _tcard = DISPENSED
      rendo.statusImage(msg)
    }
    import SlotStatus._
    ilayer.setImage(status match {
      case        GIFTED|
          RECRUIT_GIFTED => dispensed("Gifted!")
      case          SOLD => dispensed("Sold!")
      case     UNFLIPPED => if (_tcard != null && _tcard.name != null) rendo.partImage(_tcard)
                            else if (isGift) rendo.gift
                            else rendo.back
      case       FLIPPED => rendo.image(cache, _tcard)
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

  override protected def createBehavior = new Behavior.Select[CardButton](this) {
    override protected def onClick (event :Pointer.Event) {
      if (_tcard != null && _tcard.image != null) onView()
      else if (_tcard != DISPENSED) onReveal()
    }
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

  /** The number of fragments into which to break a card, when selling, based on rarity. */
  val saleFrags = Seq((2, 2), (2, 3), (3, 3), (3, 4), (3, 5), (4, 4), (4, 5), (5, 6), (6, 8), (8, 8))

  private val DISPENSED = new ThingCard
}
