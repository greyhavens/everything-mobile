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

  override def wasAdded () {
    super.wasAdded()

    // if we've authed with Facebook at least once already, then just go
    if (game.fb.isAuthed) {
      // display a modal dialog until we're authed
      showLoading("Logging in...", game.self)
      game.validateSession()
    }
    // otherwise pop up a little notice saying this is a FB game and we're going to auth
    else new Dialog().addTitle("Welcome!").addText(
      "The Everything Game is played with your Facebook friends. " +
        "Click 'OK' to connect to The Everything Game on Facebook and start playing!").
      addButton("OK", game.validateSession()).
      display()
  }

  override def createUI (root :Root) {
    val buttons = new Group(AxisLayout.vertical.offEqualize).add(
      UI.wideButton("") { new OpenGiftsScreen(game).push() }.
        bindText(game.gifts.sizeView.map(rf { size => s"Gifts: $size!" })).
        bindVisible(game.gifts.sizeView.map(rf { _ > 0 })),
      UI.wideButton("Flip Cards!") { new FlipCardsScreen(game).push() },
      UI.wideButton("News") { /* new NewsScreen(game).push() */ },
      UI.wideButton("Collection") { new CollectionScreen(game, game.self.get).push() },
      UI.wideButton("Shop") { new ShopScreen(game).push() })
    root.add(UI.stretchShim,
             new Label("The").addStyles(Style.FONT.is(UI.menuFont)),
             new Label("Everything").addStyles(Style.FONT.is(UI.titleFont)),
             new Label("Game").addStyles(Style.FONT.is(UI.menuFont)),
             UI.stretchShim,
             buttons,
             UI.stretchShim)
    // disable all the buttons until we're authed
    _dbag.add(game.self.connectNotify(slot { self =>
      buttons.foreach { _.setEnabled(self != null) }
    }))
  }
}
