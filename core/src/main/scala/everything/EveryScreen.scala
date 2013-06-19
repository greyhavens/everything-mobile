//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.Layer
import pythagoras.f.Point
import react.UnitSignal

import tripleplay.game.{ScreenStack, UIScreen}
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.util.DestroyableBag

import com.threerings.everything.data.ThingCard

abstract class EveryScreen (game :Everything) extends UIScreen {

  class Dialog (title :String, text :String) {
    val ok = new UnitSignal
    def onOK (action : =>Unit) = { ok.connect(unitSlot(action)) ; this }
    def okLabel = "OK"

    val cancel = new UnitSignal
    def onCancel (action : =>Unit) = { cancel.connect(unitSlot(action)) ; this }
    def cancelLabel = "Cancel"

    def display () :Unit = display(null)

    def display (extra :Group) {
      val root = iface.createRoot(AxisLayout.vertical, UI.sheet, layer)
      root.layer.setDepth(Short.MaxValue)
      // absorb all clicks below our root layer
      root.layer.setHitTester(new Layer.HitTester {
        def hitTest (layer :Layer, p :Point) = {
          val l = layer.hitTestDefault(p)
          if (l == null) root.layer else l
        }
      })
      root.addStyles(Style.BACKGROUND.is(Background.composite(
        Background.solid(UI.textColor).inset(1),
        Background.croppedImage(_pageRepeat).inset(10))))
      root.add(new Label(title).addStyles(Style.FONT.is(UI.headerFont)),
               new Label(text).addStyles(Style.TEXT_WRAP.on, Style.HALIGN.left))
      if (extra != null) root.add(extra)
      root.add(new Group(AxisLayout.horizontal.gap(25)).add(
                 new Button(cancelLabel).onClick(cancel.slot),
                 new Button(okLabel).onClick(ok.slot)))
      root.pack(width-20, 0)
      root.layer.setTranslation((width-root.size.width)/2, (height-root.size.height)/2);
      // TODO: animate reveal and dismiss
      onOK(iface.destroyRoot(root))
      onCancel(iface.destroyRoot(root))
    }
  }

  def push () :Unit = game.screens.push(this, pushTransition)
  protected def pushTransition :ScreenStack.Transition = game.screens.slide

  def pop () :Unit = game.screens.remove(this, popTransition)
  protected def popTransition :ScreenStack.Transition = game.screens.slide.right

  def createUI (root :Root)

  override def wasAdded () {
    val root = iface.createRoot(AxisLayout.vertical, UI.sheet, layer)
    root.addStyles(Style.BACKGROUND.is(background))
    createUI(root)
    root.setSize(width, height)
  }

  /** Displays a dialog enabling the sale of `card`. On sale, `onSold` is invoked and this screen is
    * popped. */
  protected def maybeSellCard (card :ThingCard)(onSold : =>Unit) {
    val amount = card.rarity.saleValue
    new Dialog(s"Sell Card", s"Sell ${card.name} for E $amount") {
      override def okLabel = "Yes"
      override def cancelLabel = "No"
    }.onOK(sellCard(card, onSold)).display()
  }

  protected def sellCard (card :ThingCard, onSold : =>Unit) {
    game.gameSvc.sellCard(card.thingId, card.received).onFailure(onFailure).
      onSuccess(slot { res =>
        game.coins.update(res.coins)
        val catId = card.categoryId
        res.newLike match {
          case null => game.likes.remove(catId)
          case like => game.likes.put(catId, like)
        }
        onSold
        pop()
      })
  }

  protected def background :Background = Background.image(_pageRepeat).inset(10)

  protected val onFailure = (cause :Throwable) => {
    cause.printStackTrace(System.err) // TODO: display UI
  }

  protected val _dbag = new DestroyableBag
  protected val _pageRepeat = UI.getImage("page_repeat.png")
}
