//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import tripleplay.game.UIScreen
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

class MainMenuScreen (game :Everything) extends UIScreen {

  override def wasAdded () {
    val root = iface.createRoot(AxisLayout.vertical, UI.sheet, layer)
    root.addStyles(Style.BACKGROUND.is(Background.image(assets.getImage("images/page_repeat.png"))))
    val btnStyle = Style.FONT.is(UI.menuFont)
    root.add(UI.stretchShim,
             new Label("The").addStyles(Style.FONT.is(UI.menuFont)),
             new Label("Everything").addStyles(Style.FONT.is(UI.titleFont)),
             new Label("Game").addStyles(Style.FONT.is(UI.menuFont)),
             UI.stretchShim,
             new Group(AxisLayout.vertical.offEqualize).add(
               new Button("Flip Cards!").addStyles(btnStyle).onClick(viewGrid _),
               new Button("News").addStyles(btnStyle).onClick(viewNews _),
               new Button("Collection").addStyles(btnStyle).onClick(viewCollection _),
               new Button("Shop").addStyles(btnStyle).onClick(viewShop _)),
             UI.stretchShim)
    root.setSize(width, height)
  }

  protected def viewGrid () {
    game.screens.push(new FlipCardsScreen(game), game.screens.slide)
  }

  protected def viewNews () {
    // TODO: game.screens.push(new NewsScreen(game), game.screens.slide)
  }

  protected def viewCollection () {
    // TODO: game.screens.push(new CollectionScreen(game), game.screens.slide)
  }

  protected def viewShop () {
    // TODO: game.screens.push(new ShopScreen(game), game.screens.slide)
  }
}
