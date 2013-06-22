//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import playn.core.util.Clock
import react.{IntValue, Signal, RList, RMap, Value}
import scala.collection.JavaConversions._
import tripleplay.game.ScreenStack

import com.threerings.everything.data._

class Everything (device :Device, fb :Facebook) extends Game.Default(33) {

  val screens = new ScreenStack
  val keyDown = Signal.create[Key]()

  val mock = false
  val svcURL = "http://everything-candidate.herokuapp.com/json/" // TODO
  val everySvc :EveryService = if (mock) MockEveryService else new EveryServiceClient(this, svcURL)
  val gameSvc  :GameService  = if (mock) MockGameService  else new GameServiceClient(this, svcURL)

  val self  = Value.create[PlayerName](null)
  val coins = new IntValue(0)
  val likes = RMap.create[Int,Boolean]
  val pups  = RMap.create[Powerup,JInteger]
  val gifts = RList.create[ThingCard]

  val main = new MainMenuScreen(this);

  /** Returns our current auth token, or `None`. */
  def authToken = Option(storage.getItem("authtok"))

  /** Updates our auth token. */
  def updateAuthToken (authTok :String) = storage.setItem("authtok", authTok)

  // TODO: trigger revalidation of session if we close the app and return to it after more than ~10
  // minutes

  // TODO: trigger revaliation of session if we do an RPC call and it fails due to invalid auth
  // token

  override def init ()  {
    keyboard.setListener(new Keyboard.Adapter {
      override def onKeyDown (event :Keyboard.Event) = keyDown.emit(event.key)
    })
    // display our main menu
    main.push()
    // make sure we're authed with Facebook and then auth with the Everything server
    validateSession()
  }

  override def update (delta :Int) {
    _clock.update(delta)
    screens.update(delta)
  }

  override def paint (alpha :Float) {
    _clock.paint(alpha)
    screens.paint(_clock)
  }

  protected def validateSession () {
    fb.authenticate().flatMap(rf { fbToken :String =>
      everySvc.validateSession(fbToken, device.timeZoneOffset)
    }).onFailure(main.onFailure).onSuccess { s :SessionData =>
      self.update(s.name)
      coins.update(s.coins)
      for (id <- s.likes) likes.put(id, true)
      for (id <- s.dislikes) likes.put(id, false)
      pups.putAll(s.powerups)
      gifts.addAll(s.gifts)
    }
  }

  private val _clock = new Clock.Source(33)
}
