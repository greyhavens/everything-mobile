//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.ui._

class ShopScreen (game :Everything) extends EveryScreen(game) {

  override def createUI (root :Root) {
    root.add(new Label("TODO"),
             new Button("Back").onClick(pop _))
  }
}
