//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import playn.core.util.Clock
import react.Signal
import tripleplay.game.{Screen, ScreenStack}

class Everything extends Game.Default(33) {

  val screens = new ScreenStack
  val keyDown = Signal.create[Key]()

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
    // and start with our main menu
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
