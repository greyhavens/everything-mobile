//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import playn.core.util.{Callback, Clock}
import react.{IntValue, Signal, RMap, Value}
import scala.collection.JavaConversions._
import tripleplay.game.ScreenStack

import com.threerings.everything.data._

class Everything (fb :Facebook) extends Game.Default(33) {

  val screens = new ScreenStack
  val keyDown = Signal.create[Key]()

  val svcURL = "http://localhost:8080/json/" // TODO
  val everySvc :EveryService = new EveryServiceClient(this, svcURL + "everything")
  val gameSvc  :GameService  = MockGameService

  val authed = Value.create(false)
  val coins = new IntValue(0)
  val likes = RMap.create[Int,Boolean]
  val pups = RMap.create[Powerup,JInteger]

  /** Returns our current auth token, or `None`. */
  def authToken = Option(storage.getItem("authtok"))

  /** Updates our auth token. */
  def updateAuthToken (authTok :String) = storage.setItem("authtok", authTok)

  override def init ()  {
    keyboard.setListener(new Keyboard.Adapter {
      override def onKeyDown (event :Keyboard.Event) = keyDown.emit(event.key)
    })

    // display our main menu
    val main = new MainMenuScreen(this);
    main.push()

    auth()

    keyDown.connect(slot[Key] {
      case key if (key == Key.A) => auth()
    })
  }

  protected def auth () {
    println("Authing!")
    // make sure we're authed with Facebook and then auth with the Everything server
    fb.authenticate().flatMap(rf { fbId :String =>
      val tzOffset = 0 // TODO
      everySvc.validateSession(fbId, fb.authToken, tzOffset)
    }).onFailure(onFailure).onSuccess { s :SessionData =>
      coins.update(s.coins)
      for (id <- s.likes) likes.put(id, true)
      for (id <- s.dislikes) likes.put(id, false)
      pups.putAll(s.powerups)
    }
  }

  override def update (delta :Int) {
    _clock.update(delta)
    screens.update(delta)
  }

  override def paint (alpha :Float) {
    _clock.paint(alpha)
    screens.paint(_clock)
  }

  protected val onFailure = (cause :Throwable) => {
    cause.printStackTrace(System.err) // TODO: display UI
  }

  private val _clock = new Clock.Source(33)
}
