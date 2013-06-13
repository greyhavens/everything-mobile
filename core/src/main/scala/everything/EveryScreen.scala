//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.game.{ScreenStack, UIScreen}
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.util.DestroyableBag

abstract class EveryScreen (game :Everything) extends UIScreen {

  override def wasAdded () {
    val root = iface.createRoot(AxisLayout.vertical, UI.sheet, layer)
    root.addStyles(Style.BACKGROUND.is(Background.image(UI.getImage("page_repeat.png")).inset(10)))
    createUI(root)
    root.setSize(width, height)
  }

  def createUI (root :Root)

  protected val onFailure = (cause :Throwable) => {
    cause.printStackTrace(System.err) // TODO: display UI
  }

  protected def popTransition :ScreenStack.Transition = game.screens.slide.right
  protected def pop () :Unit = game.screens.remove(this, popTransition)

  protected val _dbag = new DestroyableBag
}
