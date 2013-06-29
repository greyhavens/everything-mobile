//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.JavaConversions._
import scala.util.Random

import playn.core.Layer
import playn.core.PlayN._
import tripleplay.game.UIScreen
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

import tripleplay.ui.layout.TableLayout

class MainMenuScreen (game :Everything) extends EveryScreen(game) {

  val bbox = new Box()

  override def wasAdded () {
    super.wasAdded()

    // if we've authed with Facebook at least once already, then just go
    if (game.fb.isAuthed) game.validateSession()
    // otherwise pop up a little notice saying this is a FB game and we're going to auth
    else new Dialog().addTitle("Welcome!").addText(
      "The Everything Game is played with your Facebook friends. " +
        "Click 'OK' to connect to The Everything Game on Facebook and start playing!").
      addButton("OK", game.validateSession()).
      display()

    // add our buttons once we're authed
    game.self.connect(unitSlot {
      val entree = entrees(Random.nextInt(entrees.size))
      bbox.set(new Group(AxisLayout.vertical.offEqualize).add(
        menuButton("Gifts", entree) { openGifts() }.
          bindText(game.gifts.sizeView.map(rf { size => s"Gifts: $size!" })).
          bindVisible(game.gifts.sizeView.map(rf { _ > 0 })),
        menuButton("Flip Cards!", entree) { new FlipCardsScreen(game).push() },
        menuButton("News", entree) { new NewsScreen(game).push() },
        menuButton("Collection", entree) { new CollectionScreen(game, game.self.get).push() },
        menuButton("Shop", entree) { new ShopScreen(game).push() }))
    }).once()
  }

  override def createUI () {
    root.add(UI.shim(5, 30),
             new Label("The").addStyles(Style.FONT.is(UI.menuFont)),
             new Label("Everything").addStyles(Style.FONT.is(UI.titleFont)),
             new Label("Game").addStyles(Style.FONT.is(UI.menuFont)),
             UI.stretchShim,
             bbox.set(new Label("Logging in...")),
             UI.stretchShim)
  }

  protected def menuButton (label :String, entree :Entree)(action : =>Unit) :Button = {
    new Button(label) {
      var firstLayout = entree
      override protected def layout () {
        super.layout()
        if (firstLayout != null) {
          _tglyph.layer.setAlpha(0)
          firstLayout(_tglyph.layer)
          firstLayout = null
        }
      }
    }.addStyles(Style.FONT.is(buttonFont)).onClick(unitSlot(action))
  }

  protected def openGifts () {
    val cache = new UI.ImageCache(game)
    val cards = new Group(new TableLayout(3).gaps(10, 10))
    game.gifts.foreach { card =>
      cards.add(new CardButton(game, this, cache) {
        override protected def isGift = true
        override protected def onReveal () {
          shaking.update(true)
          game.gameSvc.openGift(card.thingId, card.received).onFailure(onFailure).
            onSuccess(slot { res =>
              game.gifts.remove(card)
              reveal(res)
            })
        }
      })
    }
    new Dialog().addTitle("Open Your Gifts!").
      add(AxisLayout.stretch(UI.vscroll(cards))).
      addButton("Done", ()).display()
  }

  type Entree = Layer.HasSize => Unit
  protected val entrees = Seq[Entree](
    new FX(this, _).fadeIn(500),
    new FX(this, _).fadeIn(1).flyIn(500)/*,
    new FX(this, _).fadeIn(1).dropIn(2, 500)*/ // TODO: reenable when origin is munged
  )
  protected val buttonFont = UI.writingFont(28)
}
