//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.Image
import react.{Slot, Value}
import tripleplay.ui.{Behavior, ImageButton}

/** An image button with two states (true and false), with separate up/down images for the two
  * states. Clicking the button toggles between the states.
  */
class TwoStateButton (
  upT :Image, downT :Image, upF :Image, downF :Image
) extends ImageButton(upF, downF) {

  /** The current state of the button. */
  val state = Value.create(false)
  state.connect(new Slot[Boolean]() {
    def onEmit (state :Boolean) = {
      if (state) setUp(upT).setDown(downT)
      else setUp(upF).setDown(downF)
    }
  })

  override protected def createBehavior = new Behavior.Click[ImageButton](this) {
    override def click () {
      state.update(!state.get())
      super.click()
    }
  }
}
