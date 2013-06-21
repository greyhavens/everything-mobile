//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.JavaConversions._

import playn.core.PlayN._
import tripleplay.game.UIScreen
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

class MainMenuScreen (game :Everything) extends EveryScreen(game) {

  // TODO: display some sort of "loading" spinner while we're talking to the server

  override def createUI (root :Root) {
    val buttons = new Group(AxisLayout.vertical.offEqualize).add(
      UI.wideButton("") {
        new OpenGiftsScreen(game).push()
      }.bindText(game.gifts.sizeView.map(rf { size => s"Gifts: $size!" })).
        bindVisible(game.gifts.sizeView.map(rf { _ > 0 })),
      UI.wideButton("Flip Cards!") {
        // TODO: display a spinner over the button while we load the grid data
        val pup :Powerup = Powerup.NOOP // TODO
        val expectHave = false // TODO
        game.gameSvc.getGrid(pup, expectHave).onFailure(onFailure).onSuccess(slot { res =>
          new FlipCardsScreen(game, res.status, res.grid).push()
        })
      },
      UI.wideButton("News") {
        // TODO: new NewsScreen(game).push()
      },
      UI.wideButton("Collection") {
        new CollectionScreen(game, game.self.get).push()
      },
      UI.wideButton("Shop") {
        new ShopScreen(game).push()
      })
    // disable all the buttons until we're authed
    _dbag.add(game.self.connectNotify(slot { self =>
      buttons.foreach { _.setEnabled(self != null) }
    }))
    root.add(UI.stretchShim,
             new Label("The").addStyles(Style.FONT.is(UI.menuFont)),
             new Label("Everything").addStyles(Style.FONT.is(UI.titleFont)),
             new Label("Game").addStyles(Style.FONT.is(UI.menuFont)),
             UI.stretchShim,
             buttons,
             UI.stretchShim)
  }
}
