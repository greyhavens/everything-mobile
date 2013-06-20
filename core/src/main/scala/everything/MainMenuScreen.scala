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

  // TODO: disable things until game.authed is true

  override def createUI (root :Root) {
    val btnStyle = Style.FONT.is(UI.menuFont)
    root.add(UI.stretchShim,
             new Label("The").addStyles(Style.FONT.is(UI.menuFont)),
             new Label("Everything").addStyles(Style.FONT.is(UI.titleFont)),
             new Label("Game").addStyles(Style.FONT.is(UI.menuFont)),
             UI.stretchShim,
             new Group(AxisLayout.vertical.offEqualize).add(
               new Button().addStyles(btnStyle).
                 bindText(game.gifts.sizeView.map(rf { size => s"Gifts: $size!" })).
                 bindVisible(game.gifts.sizeView.map(rf { _ > 0 })).onClick(unitSlot {
                   new OpenGiftsScreen(game).push()
                 }),
               new Button("Flip Cards!").addStyles(btnStyle).onClick(viewGrid _),
               new Button("News").addStyles(btnStyle).onClick(unitSlot {
                 // TODO: new NewsScreen(game).push()
               }),
               new Button("Collection").addStyles(btnStyle).onClick(unitSlot {
                 // TODO: new CollectionScreen(game).push()
               }),
               new Button("Shop").addStyles(btnStyle).onClick(unitSlot {
                 new ShopScreen(game).push()
               })),
             UI.stretchShim)
  }

  protected def viewGrid (flip :Button) {
    // TODO: display a spinner over the button while we load the grid data
    val pup :Powerup = Powerup.NOOP // TODO
    val expectHave = false // TODO
    game.gameSvc.getGrid(pup, expectHave).onFailure(onFailure).onSuccess(slot { res =>
      new FlipCardsScreen(game, res.status, res.grid).push()
    })
  }
}
