//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import playn.core.util.{Callback, Clock}
import react.{IntValue, Signal, RList, RMap, RSet, Value}
import scala.collection.JavaConversions._
import tripleplay.game.ScreenStack

import com.threerings.everything.data._

class Everything (mock :Boolean, val device :Device, val fb :Facebook) extends Game.Default(33) {

  // revalidate our session if we're paused for > 5 mins
  final val RevalidatePeriod = 5*60*60*1000L

  // some are-we-testing bits
  val isCandidate = platformType == Platform.Type.JAVA
  val herokuId = if (isCandidate) "everything-candidate" else "everything"

  val svcURL = s"http://$herokuId.herokuapp.com/json/"
  val everySvc :EveryService = if (mock) Mockery else new EveryServiceClient(this, svcURL)
  val gameSvc  :GameService  = if (mock) Mockery else new GameServiceClient(this, svcURL)

  val screens = new ScreenStack()
  val keyDown = Signal.create[Key]()
  val self    = Value.create[PlayerName](null)
  val sess    = Value.create[SessionData](null)
  val coins   = new IntValue(0)
  val likes   = RMap.create[Int,JBoolean]
  val wants   = RSet.create[Int]
  val pups    = RMap.create[Powerup,JInteger]
  val gifts   = RList.create[ThingCard]

  val main = new MainMenuScreen(this);
  val notebook = graphics.createImageLayer()

  /** Returns our current auth token, or `None`. */
  def authToken = Option(storage.getItem("authtok"))

  /** Updates our auth token. */
  def updateAuthToken (authTok :String) = storage.setItem("authtok", authTok)

  // TODO: more proper?
  def cardImageURL (hash :String) = s"http://s3.amazonaws.com/everything.threerings.net/${hash}"

  // TODO: trigger revaliation of session if we do an RPC call and it fails due to invalid auth
  // token

  /** Validates our session with the server. */
  def validateSession () {
    fb.authenticate().flatMap(rf { fbToken :String =>
      everySvc.validateSession(fbToken, device.timeZoneOffset)
    }).onFailure(main.onFailure).onSuccess { s :SessionData =>
      self.update(s.name)
      coins.update(s.coins)
      sess.update(s)
      for (id <- s.likes) likes.put(id, true)
      for (id <- s.dislikes) likes.put(id, false)
      pups.putAll(s.powerups)
      gifts.addAll(s.gifts)
    }
  }

  /** Initiates the redemption of an in-app purchase with the server. This is called by the device
    * backends when they learn that a purchase has been completed. The server will check that a
    * purchase with the specified `token` has not already been redeemed, validate the `receipt`
    * with the appropriate `platform` provider and (assuming everything checks out) grant the
    * appropriate number of coins to the user.
    */
  def redeemPurchase (sku :String, platform :String, token :String, receipt :String) {
    everySvc.redeemPurchase(sku, platform, token, receipt).
      onFailure(slot { exn => screens.top.asInstanceOf[EveryScreen].onFailure(exn) }).
      onSuccess(slot { coinbal =>
        coins.update(coinbal)
        device.purchaseRedeemed(sku, token)
      })
  }

  override def init ()  {
    // propagate events so that our scroller can usurp button clicks
    platform.setPropagateEvents(true)
    platform.setLifecycleListener(new PlayN.LifecycleListener() {
      var _paused = System.currentTimeMillis
      override def onPause () {
        _paused = System.currentTimeMillis
      }
      override def onResume () {
        val pauseTime = System.currentTimeMillis - _paused
        if (pauseTime > RevalidatePeriod) {
          log.info(s"Paused for ${pauseTime/60*1000}s, revalidating session.")
          validateSession()
        }
      }
      override def onExit () {} // nada
    })
    // listen for "keyboard" events (used for back button on Android)
    keyboard.setListener(new Keyboard.Adapter {
      override def onKeyDown (event :Keyboard.Event) = keyDown.emit(event.key)
    })
    // display our notebook background behind everything
    val (swidth, sheight) = (graphics.width, graphics.height)
    val cropped = graphics.createImage(swidth, sheight)
    UI.getImage("notebook.jpg").addCallback(new Callback[Image]() {
      def onSuccess (image :Image) =
        cropped.canvas.drawImage(image, (swidth-image.width)/2, (sheight-image.height)/2)
      def onFailure (cause :Throwable) {} // not happening
    })
    graphics.rootLayer.add(notebook.setImage(cropped).setDepth(-Short.MaxValue))
    // if we've not authed (aka, we've never played); push the intro screen
    if (!authToken.isDefined) new IntroScreen(this).push()
    // otherwise head straight to the main menu
    else main.push()
  }

  override def update (delta :Int) {
    _clock.update(delta)
    screens.update(delta)
  }

  override def paint (alpha :Float) {
    _clock.paint(alpha)
    screens.paint(_clock)
  }

  private val _clock = new Clock.Source(33)
}
