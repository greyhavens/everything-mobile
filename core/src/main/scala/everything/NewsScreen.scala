//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.ui.Style
import tripleplay.ui.layout.AxisLayout

class NewsScreen (game :Everything) extends EveryScreen(game) {

  override def createUI () {
    val news = game.sess.get.news
    root.add(header("Everything News"),
             AxisLayout.stretch(UI.vscroll(UI.vgroup(
               UI.wrapLabel(news.text),
               UI.tipLabel("- " + game.device.formatDate(news.reported.getTime)).
                 addStyles(Style.HALIGN.right)))))
  }
}
