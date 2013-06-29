//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.Point
import react.{Value, UnitSignal}

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
      if (buttons.childCount == 0) buttons.add(UI.stretchShim())
      buttons.add(btn.onClick(unitSlot { dispose() }), UI.stretchShim())
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
      val psize = root.preferredSize(width-20, 0)
      root.setSize(psize.width, math.min(psize.height, height-20))
      root.layer.setTranslation((width-root.size.width)/2, (height-root.size.height)/2);
      // TODO: animate reveal
    }

    def dispose () {
      // TODO: animate dismiss
      iface.destroyRoot(root)
    }
  }

  val isVisible = Value.create(false)

  val onFailure = (cause :Throwable) => {
    val msg = cause.getMessage
    if (msg == null || !msg.startsWith("e.")) log.warn("Erm, failure", cause)
    new Dialog().addTitle("Oops!").addText(I18n.xlate(msg)).addButton("OK", ()).display()
  }

  /** Returns the coordinates of the specified layer in this screen's coordinate system. */
  def pos (layer :Layer) :Point = Layer.Util.layerToParent(layer, this.layer, 0, 0)

  def push () :Unit = game.screens.push(this, pushTransition)
  def replace () :Unit = game.screens.replace(this, pushTransition)
  protected def pushTransition :ScreenStack.Transition = game.screens.slide.duration(300)

  def pop () :Unit = game.screens.remove(this, popTransition)
  protected def popTransition :ScreenStack.Transition = game.screens.slide.right.duration(300)

  val root = iface.createRoot(layout(), UI.sheet, layer)
  def createUI () :Unit

  override def wasAdded () {
    root.addStyles(Style.BACKGROUND.is(background))
    createUI()
    root.setSize(width, height)
    // wire up the (hardware) back button handler
    _dbag.add(game.keyDown.connect(slot { k =>
      if (k == Key.BACK) onHardwareBack()
    }))
  }

  override def wasShown () {
    super.wasShown()
    isVisible.update(true)
  }

  override def wasHidden () {
    super.wasHidden()
    isVisible.update(false)
  }

  protected def layout () :Layout = AxisLayout.vertical().offStretch

  protected def header (title :String, right :Element[_]*) = UI.hgroup(
    AxisLayout.stretch(UI.hgroup(back()).addStyles(Style.HALIGN.left)),
    UI.headerLabel(title),
    AxisLayout.stretch(UI.hgroup(right :_*).addStyles(Style.HALIGN.right)))

  protected def back () = noteBack(new Button(Icons.image(UI.backImage)).onClick(unitSlot(pop())))
  protected def back (label :String) = noteBack(UI.button(label)(pop()))
  protected def noteBack (back :Button) = _back match {
    case null => _back = back ; back
    case _ => throw new AssertionError("Already have a back button!")
  }

  protected def showLoading (text :String, value :Value[_]) {
    val lid = new Dialog().addTitle(text)
    lid.display()
    value.connect(unitSlot { lid.dispose() }).once()
  }

  /** Displays a dialog enabling the sale of `card`. On sale, `onSold` is invoked and this screen is
    * popped. */
  protected def maybeSellCard (card :ThingCard)(onSold : =>Unit) {
    val amount = card.rarity.saleValue
    new Dialog().addTitle("Sell Card").
      add(UI.hgroup(new Label(s"Sell ${card.name} for"), UI.moneyIcon(amount), new Label("?"))).
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

  protected def background () :Background = Background.image(_bgImage).inset(5)

  /** Handles a click on the hardware back button. */
  protected def onHardwareBack () {
    // if we're not the top screen, then do nothing (we may be triggered even though we're not the
    // currently showing screen)
    if (game.screens.top == this &&
      // don't try to go back if we're currently transitioning between screens
      !game.screens.isTransiting &&
      // if this screen has a back button, and it's enabled, click  it
      _back != null && _back.isEnabled) _back.click()
  }

  protected def todo () = new Dialog().addTitle("TODO").addButton("OK", ()).display()

  protected var _back :Button = _
  protected val _dbag = new DestroyableBag
  protected val _bgImage = height match {
    case 568 => assets.getImageSync("../Default-568h.png")
    case 480 => assets.getImageSync("../Default.png")
    case _ => UI.getImage("page_repeat.png")
  }
}
