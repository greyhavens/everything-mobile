//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.Point
import react.UnitSignal

import tripleplay.game.{ScreenStack, UIScreen}
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.util.DestroyableBag

import com.threerings.everything.data.ThingCard

abstract class EveryScreen (game :Everything) extends UIScreen {

  class Dialog {
    val root = iface.createRoot(AxisLayout.vertical.offStretch, UI.sheet, layer)
    val buttons = UI.hgroup()

    def add (elem :Element[_]) = { root.add(elem) ; this }
    def addTitle (text :String) = add(UI.headerLabel(text))
    def addText (text :String) = add(UI.wrapLabel(text))

    def addButton (lbl :String, action : =>Unit) :this.type = addButton(UI.button(lbl)(action))
    def addButton (btn :Button) :this.type = {
      buttons.add(btn.onClick(unitSlot {
        // TODO: animate dismiss
        iface.destroyRoot(root)
      }))
      this
    }

    def background () :Background =
      Background.composite(Background.solid(UI.textColor).inset(1),
                           Background.croppedImage(UI.getImage("page_repeat.png")).inset(9))

    def display () {
      root.layer.setDepth(Short.MaxValue)
      // absorb all clicks below our root layer
      root.layer.setHitTester(UI.absorber)
      root.addStyles(Style.BACKGROUND.is(background()))
      root.add(buttons)
      root.pack(width-20, 0)
      root.layer.setTranslation((width-root.size.width)/2, (height-root.size.height)/2);
      // TODO: animate reveal
    }
  }

  val onFailure = (cause :Throwable) => {
    log.warn("Erm, failure", cause)
    new Dialog().addTitle("Oops").addText(cause.getMessage).addButton("OK", ())
  }

  /** Returns the coordinates of the specified layer in this screen's coordinate system. */
  def pos (layer :Layer) :Point = Layer.Util.layerToParent(layer, this.layer, 0, 0)

  def push () :Unit = game.screens.push(this, pushTransition)
  def replace () :Unit = game.screens.replace(this, pushTransition)
  protected def pushTransition :ScreenStack.Transition = game.screens.slide.duration(300)

  def pop () :Unit = game.screens.remove(this, popTransition)
  protected def popTransition :ScreenStack.Transition = game.screens.slide.right.duration(300)

  def createUI (root :Root)

  override def wasAdded () {
    val root = iface.createRoot(AxisLayout.vertical.offStretch, UI.sheet, layer)
    root.addStyles(Style.BACKGROUND.is(background))
    createUI(root)
    root.setSize(width, height)
  }

  protected def header (title :String) =
    UI.hgroup(UI.button("Back")(pop()), AxisLayout.stretch(UI.headerLabel(title)))

  /** Displays a dialog enabling the sale of `card`. On sale, `onSold` is invoked and this screen is
    * popped. */
  protected def maybeSellCard (card :ThingCard)(onSold : =>Unit) {
    val amount = card.rarity.saleValue
    new Dialog().addTitle("Sell Card").addText(s"Sell ${card.name} for E $amount").
      addButton("No", ()).addButton("Yes", sellCard(card, onSold)).display()
  }

  protected def sellCard (card :ThingCard, onSold : =>Unit) {
    onSold
    pop()
    game.gameSvc.sellCard(card.thingId, card.received).onFailure(onFailure).
      onSuccess(slot { res =>
        game.coins.update(res.coins)
        val catId = card.categoryId
        res.newLike match {
          case null => game.likes.remove(catId)
          case like => game.likes.put(catId, like)
        }
      })
  }

  protected def background () :Background = Background.image(_bgImage).inset(10)

  protected def todo () = new Dialog().addTitle("TODO").addButton("OK", ()).display()

  protected val _dbag = new DestroyableBag
  protected val _bgImage = height match {
    case 568 => assets.getImageSync("../Default-568h.png")
    case 480 => assets.getImageSync("../Default.png")
    case _ => UI.getImage("page_repeat.png")
  }
}
