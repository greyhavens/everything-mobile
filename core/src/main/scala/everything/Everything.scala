//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import playn.core.util.Clock
import react.{IntValue, Signal, RMap}
import scala.collection.JavaConversions._
import tripleplay.game.ScreenStack

import com.threerings.everything.data._

class Everything extends Game.Default(33) {

  val screens = new ScreenStack
  val keyDown = Signal.create[Key]()

  val everySvc :EveryService = MockEveryService
  val gameSvc :GameService = MockGameService

  val coins = new IntValue(0)
  val likes = RMap.create[Int,Boolean]
  val pups = RMap.create[Powerup,JInteger]

  override def init ()  {
    keyboard.setListener(new Keyboard.Adapter {
      override def onKeyDown (event :Keyboard.Event) = keyDown.emit(event.key)
    })

    val tzOffset = 0 // TODO
    everySvc.validateSession(tzOffset).onSuccess { s :SessionData =>
      coins.update(s.coins)
      for (id <- s.likes) likes.put(id, true)
      for (id <- s.dislikes) likes.put(id, false)
      pups.putAll(s.powerups)
    }

    // TODO: push "connecting" screen while waiting for session validation?
    new MainMenuScreen(this).push()
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
