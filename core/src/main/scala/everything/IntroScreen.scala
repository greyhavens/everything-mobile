//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.ui._

class IntroScreen (game :Everything) extends EveryScreen(game) {

  override def createUI () {
    val arrow = "\u2b07"
    root.add(UI.headerLabel("Welcome to\nThe Everything Game!").addStyles(Style.TEXT_WRAP.on),
             UI.shim(10, 10),
             UI.wrapLabel("What is Everything?\nPlease refer to this handy infographic:").
               addStyles(Style.HALIGN.center),
             UI.stretchShim(),
             UI.headerLabel("Wake up in morning"),
             UI.glyphLabel(arrow),
             UI.headerLabel("Flip cards"),
             UI.glyphLabel(arrow),
             UI.headerLabel("Trade with friends"),
             UI.glyphLabel(arrow),
             UI.headerLabel("Amass awesome collection!"),
             UI.stretchShim(),
             UI.bgroup(UI.button("Let's Go!") {
               game.main.replace()
             }))
  }
}
