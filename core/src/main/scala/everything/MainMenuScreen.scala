//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.JavaConversions._
import scala.util.Random

import playn.core.Layer
import playn.core.PlayN._
import react.Value
import tripleplay.game.UIScreen
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

import tripleplay.ui.layout.TableLayout

class MainMenuScreen (game :Everything) extends EveryScreen(game) {

  final val FreshnessPeriod = 5*24*60*60*1000L // news is stale after 5 days

  val bbox = new Box()
  val newsIsFresh = game.sess.map(rf { sess =>
    java.lang.Boolean.valueOf(sess != null && sess.news != null &&
      (System.currentTimeMillis - sess.news.reported.getTime < FreshnessPeriod))
  })
  val welcome = UI.tipLabel("")

  def displayNotices (notices :List[Notice]) :Unit = notices match {
    case n :: t => I18n.xlate(n) match {
      case None => displayNotices(t)
      case Some(msg) => {
        val title = if (n.coins > 0) "Reward Time!" else "Notice!"
        val d = new Dialog().addTitle(title).addText(msg)
        if (n.coins > 0) d.add(UI.hgroup(UI.tipLabel("Reward:"), UI.moneyIcon(n.coins)))
        d.addButton("Yay!", displayNotices(t)).display()
      }
    }
    case Nil => // done!
  }

  override def wasAdded () {
    super.wasAdded()

    // if we've authed with Facebook at least once already, then just go
    if (game.fb.isAuthed) game.validateSession(false)
    // otherwise pop up a little notice saying this is a FB game and we're going to auth
    else new Dialog().addTitle("Welcome!").addText(
      "The Everything Game is played with your Facebook friends. " +
        "Click 'OK' to connect to The Everything Game on Facebook and start playing!").
      addButton("OK", game.validateSession(true)).
      display()

    // add our buttons once we're authed
    game.sess.connect(slot { sess =>
      val entree = entrees(Random.nextInt(entrees.size))
      bbox.set(new Group(AxisLayout.vertical.offEqualize).add(
        menuButton("Gifts", entree) { openGifts() }.
          bindText(game.gifts.sizeView.map(rf { size => s"Gifts: $size!" })).
          bindVisible(game.gifts.sizeView.map(rf { _ > 0 })),
        menuButton("Flip Cards!", entree) { new FlipCardsScreen(game).push() },
        menuButton("News", entree) { new NewsScreen(game).push() }.
          bindVisible(newsIsFresh),
        menuButton("Collection", entree) { new CollectionScreen(game, game.self.get).push() },
        menuButton("Goings On", entree) { new ActivityScreen(game).push() },
        menuButton("Shop", entree) { new ShopScreen(game).push() }))
      welcome.text.update(greeting(sess))
    }).once()
  }

  override def createUI () {
    root.add(UI.shim(5, 30),
             new Label("The").addStyles(Style.FONT.is(UI.menuFont)),
             new Label("Everything").addStyles(Style.FONT.is(UI.titleFont)),
             new Label("Game").addStyles(Style.FONT.is(UI.menuFont)),
             UI.stretchShim,
             bbox.set(new Label("Logging in...")),
             UI.stretchShim,
             welcome)
  }

  protected def menuButton (label :String, entree :Entree)(action : =>Unit) :Button = {
    new Button(label) {
      var firstLayout = entree
      override protected def layout () {
        super.layout()
        if (firstLayout != null && _tglyph.layer != null) {
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
    val cardsEnabled = Value.create(true :JBoolean)
    game.gifts.foreach { card =>
      cards.add(new CardButton(game, this, cache, UI.card, cardsEnabled) {
        override protected def isGift = true
        override protected def onReveal () {
          shaking.update(true)
          game.gameSvc.openGift(card.thingId, card.received).
            bindComplete(cardsEnabled.slot). // disable cards while req in flight
            onFailure(onFailure).
            onSuccess(slot { res =>
              game.gifts.remove(card)
              // TODO: run "wrap up with bow" animation in reverse to unwrap, then flip it
              reveal(res)
            })
        }
      })
    }
    new Dialog().addTitle("Open Your Gifts!").
      add(AxisLayout.stretch(UI.vscroll(cards))).
      addButton("Done", ()).display()
  }

  protected def greeting (sess :SessionData) = {
    val name = sess.name.name
    game.device.hourOfDay match {
      case h if (h < 4)  => s"Hi $name! Burning the midnight oil?"
      case h if (h < 9)  => s"Good morning $name!"
      case h if (h < 17) => if (sess.gridsConsumed < 4) s"Hi $name!" else s"Welcome back $name!"
      case _             => s"Good evening $name!"
    }
  }

  override def pop () {} // don't pop, we got nowhere to go

  type Entree = Layer.HasSize => Unit
  protected val entrees = Seq[Entree](
    new FX(this, _).fadeIn(500),
    new FX(this, _).fadeIn(1).flyIn(500),
    new FX(this, _).fadeIn(1).dropIn(2, 500),
    new FX(this, _).fadeIn(1).popIn(500)
  )
  protected val buttonFont = UI.writingFont(28)
}
