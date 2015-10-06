//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.ui._

class IntroScreen (game :Everything) extends EveryScreen(game) {

  override def createUI () {
    val arrow = "\u261F"
    val go = new Button("Let's Go!") {
      override protected def layout () {
        super.layout()
        // move the origin of the button's layer to its center, so we can throb it
        layer.setOrigin(size.width/2, size.height/2)
        layer.transform.translate(size.width/2, size.height/2)
      }
    }.onClick(unitSlot { game.main.replace() })
    root.add(UI.shim(10, 10),
             UI.headerLabel("Welcome to\nThe Everything Game!").addStyles(Style.TEXT_WRAP.on),
             UI.shim(10, 10),
             UI.wrapLabel("What is Everything?\nPlease refer to this handy infographic:").
               addStyles(Style.HALIGN.center),
             UI.stretchShim(),
             UI.headerLabel("Wake up in morning"),
             UI.label(arrow, UI.titleFont),
             UI.headerLabel("Flip cards"),
             UI.label(arrow, UI.titleFont),
             UI.headerLabel("Trade with friends"),
             UI.label(arrow, UI.titleFont),
             UI.headerLabel("Amass awesome collection!"),
             UI.stretchShim(),
             UI.bgroup(go),
             UI.shim(10, 10))
    // give them 15s to read the page, then start throbbing the OK button
    iface.animator.delay(15*1000).`then`.repeat(go.layer).
      tweenScale(go.layer).to(1.05f).in(400).`then`.
      tweenScale(go.layer).to(1).in(400)
  }
}
