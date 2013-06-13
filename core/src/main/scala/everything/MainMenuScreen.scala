//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import tripleplay.game.UIScreen
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

class MainMenuScreen (game :Everything) extends EveryScreen(game) {

  override def createUI (root :Root) {
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
  }

  protected def viewGrid (flip :Button) {
    // TODO: display a spinner over the button while we load the grid data
    val pup :Powerup = null // TODO
    val expectHave = false // TODO
    game.gameSvc.getGrid(pup, expectHave).onFailure(onFailure).onSuccess(slot[(Grid,GameStatus)] {
      case (grid, status) =>
        game.screens.push(new FlipCardsScreen(game, status, grid), game.screens.slide)
    })
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
