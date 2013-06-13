//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import playn.core.util.Clock
import react.{IntValue, Signal}
import tripleplay.game.{Screen, ScreenStack}

import com.threerings.everything.data._

class Everything extends Game.Default(33) {

  val screens = new ScreenStack
  val keyDown = Signal.create[Key]()

  val everySvc :EveryService = MockEveryService
  val gameSvc :GameService = MockGameService

  val coins = new IntValue(0)

  override def init ()  {
    keyboard.setListener(new Keyboard.Adapter {
      override def onKeyDown (event :Keyboard.Event) = keyDown.emit(event.key)
    })
    // wire up a 'reboot' of the UI on pressing 'r'
    keyDown.connect(slot[Key] {
      case key if (key == Key.R) =>
        screens.remove(new ScreenStack.Predicate {
          def apply (screen :Screen) = true
        })
        screens.push(new MainMenuScreen(this))
    })

    val tzOffset = 0 // TODO
    everySvc.validateSession(tzOffset).onSuccess { s :SessionData =>
      coins.update(s.coins)
    }

    // TODO: push "connecting" screen while waiting for session validation?
    screens.push(new MainMenuScreen(this))
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
