//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.{MathUtil, Rectangle, Point}
import react.{Value, Signal, UnitSignal}

import tripleplay.game.{ScreenStack, UIScreen}
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.util.DestroyableBag

import com.threerings.everything.data.ThingCard

abstract class EveryScreen (game :Everything) extends UIScreen {

  class Dialog {
    val root = iface.createRoot(layout(), UI.sheet).addStyles(Style.BACKGROUND.is(background()))
    // absorb all clicks below our root layer, and render above everything else
    root.layer.setHitTester(UI.absorber)
    root.layer.setDepth(Short.MaxValue)

    val buttons = UI.hgroup()

    def add (elem :Element[_]) = { root.add(elem) ; this }
    def addTitle (text :String) = add(UI.headerLabel(text))
    def addText (text :String) = add(UI.wrapLabel(text))

    def addButton (lbl :String, action : =>Unit) :this.type = addButton(UI.button(lbl)(action))
    def addButton (btn :Button) :this.type = {
      if (buttons.childCount == 0) buttons.add(UI.stretchShim())
      buttons.add(btn.onClick(unitSlot { dismiss() }), UI.stretchShim())
      this
    }

    def autoDismiss :this.type = {
      root.layer.addListener(new Pointer.Adapter() {
        override def onPointerStart (event :Pointer.Event) {
          val (lx, ly) = (event.localX, event.localY)
          if (lx < 0 || ly < 0 || lx > root.size.width || ly > root.size.height) dismiss()
        }
      })
      this
    }

    def layout () :Layout = AxisLayout.vertical.offStretch
    def background () :Background = Background.composite(
      Background.solid(UI.textColor).inset(1),
      Background.croppedImage(UI.getImage("page_repeat.png")).inset(9))

    def display () {
      if (buttons.childCount > 0) root.add(buttons)
      // compute our preferred size, then restrict it to fit inside the screen
      val psize = root.preferredSize(width-20, 0)
      root.setSize(psize.width, math.min(psize.height, height-20))
      root.layer.setOrigin(root.size.width/2, root.size.height/2)
      root.layer.setTranslation(width/2, height/2);
      // animate reveal
      root.layer.setScale(MathUtil.EPSILON)
      iface.animator.add(layer, root.layer).`then`.
        tweenScale(root.layer).to(1).in(500).easeOutBack
    }

    def displayAt (x :Float, y :Float) {
      if (buttons.childCount > 0) root.add(buttons)
      // compute our preferred size, then restrict it to fit inside the screen
      val psize = root.preferredSize(width-x-10, 0)
      root.setSize(psize.width, math.min(psize.height, height-y-10))
      root.layer.setTranslation(x, y);
      // animate reveal vertically
      root.layer.setAlpha(0)
      iface.animator.add(layer, root.layer).`then`.
        tweenAlpha(root.layer).to(1).in(500)
    }

    def dismiss () {
      val hide = if (root.layer.originX == 0) iface.animator.tweenAlpha(root.layer).to(0).in(500)
                 // avoid scaling down to zero because that can cause freakoutery on mouse-based
                 // backends as motion events come in and the backend tries to do an inverse
                 // transform on a matrix with zero scale; meh
                 else iface.animator.tweenScale(root.layer).to(MathUtil.EPSILON).in(500).easeIn
      hide.`then`.action(new Runnable {
        def run = iface.destroyRoot(root)
      })
    }
  }

  /** A value updated in `wasHidden`/`wasShown`. */
  val isVisible = Value.create(false)

  /** A signal emitted in `showTransitionCompleted`. */
  val onShown = Signal.create[EveryScreen]()

  /** Handles service failure by popping up a dialog. */
  val onFailure = (cause :Throwable) => {
    val msg = cause.getMessage
    if (msg == null || !msg.startsWith("e.")) log.warn("Erm, failure", cause)
    new Dialog().addTitle("Oops!").addText(I18n.xlate(msg)).addButton("OK", ()).display()
  }

  /** A purse label, which may or may not get incorporated into the UI. It's broken out here so that
    * we can fling coins at it when we create the card sold animation. */
  lazy val purseLabel = UI.moneyIcon(game.coins, _dbag)

  /** Returns the coordinates of the specified layer in this screen's coordinate system. */
  def pos (layer :Layer) :Point = Layer.Util.layerToParent(layer, this.layer, 0, 0)

  def push () :Unit = game.screens.push(this, pushTransition)
  def replace () :Unit = game.screens.replace(this, pushTransition)
  protected def pushTransition :ScreenStack.Transition = game.screens.slide.duration(500)

  def pop () :Unit = game.screens.remove(this, popTransition)
  protected def popTransition :ScreenStack.Transition = game.screens.slide.right.duration(500)

  val root = iface.createRoot(layout(), UI.sheet, layer)
  def createUI () :Unit

  override def wasAdded () {
    root.addStyles(Style.BACKGROUND.is(background().inset(game.device.statusBarHeight, 5, 5, 5)))
    createUI()
    root.setSize(width, height)
    // wire up the (hardware) back button handler
    _dbag.add(game.keyDown.connect(slot { k =>
      if (k == Key.BACK) onHardwareBack()
    }))
    // create a swipe right gesture which means "go back"
    root.layer.setHitTester(UI.absorber)
    root.layer.addListener(new Pointer.Adapter {
      override def onPointerStart (event :Pointer.Event) = {
        _start.set(event.x, event.y)
        _lastMove = event.time
        _maxDist = 0
        _startedOnChild = (event.hit != root.layer)
      }
      override def onPointerDrag (event :Pointer.Event) {
        // TODO: if we stop for a while and then start dragging again, reset _start?
        _lastMove = event.time
        _maxDist = math.max(_maxDist, _start.distance(event.x, event.y))
      }
      override def onPointerEnd (event :Pointer.Event) = {
        val duration = event.time - _lastMove
        val (xdist, ydist) = (math.abs(event.x - _start.x), math.abs(event.y - _start.y))
        val isRight = event.x > _start.x
        // if we are clearly swiping left or right, do our swipe action
        // TODO: be more lenient about y direction as long as we're mostly moving right?
        if (duration < 500 && xdist > width/3 && ydist < height/8) {
          if (isRight) onSwipeRight() else onSwipeLeft()
        }
        // if we seem to be tapping and didn't start on an existing UI element, do our tap action
        else if (_maxDist < 10 && !_startedOnChild) onScreenTap(event)
        else log.info(s"Nah $duration $xdist $ydist ${_maxDist}")
      }
      val _start = new Point()
      var _startedOnChild = false
      var _lastMove = 0d
      var _maxDist = 0f
    })
  }

  override def wasShown () {
    super.wasShown()
    isVisible.update(true)
  }

  override def showTransitionCompleted () {
    super.showTransitionCompleted()
    onShown.emit(this)
  }

  override def wasHidden () {
    super.wasHidden()
    isVisible.update(false)
  }

  override def wasRemoved () {
    super.wasRemoved()
    _dbag.clear()
  }

  protected def layout () :Layout = AxisLayout.vertical().offStretch

  protected def header (title :String, right :Element[_]*) = UI.hgroup(
    AxisLayout.stretch(UI.hgroup(back()).addStyles(Style.HALIGN.left)),
    UI.headerLabel(title),
    AxisLayout.stretch(UI.hgroup(right :_*).addStyles(Style.HALIGN.right)))

  protected def headerPlate (image :Element[_], elems :Element[_]*) = UI.hgroup(
    back(), UI.shim(0, 5), image, UI.vgroup0(elems :_*).addStyles(Style.HALIGN.left),
    UI.stretchShim())

  protected def back () = noteBack(new Button(Icons.image(UI.backImage)).onClick(unitSlot(pop())))
  protected def back (label :String) = noteBack(UI.button(label)(pop()))
  protected def noteBack (back :Button) = _back match {
    case null => _back = back ; back
    case _ => throw new AssertionError("Already have a back button!")
  }

  protected def showLoading (text :String, value :Value[_]) {
    val lid = new Dialog().addTitle(text)
    lid.display()
    value.connect(unitSlot { lid.dismiss() }).once()
  }

  /** Displays a dialog enabling the sale of `card`. */
  protected def maybeSellCard (card :ThingCard)(onSell : =>Unit) {
    val amount = card.rarity.saleValue
    new Dialog().addTitle("Sell Card").
      add(UI.hgroup(new Label(s"Sell ${card.name} for"), UI.moneyIcon(amount), new Label("?"))).
      addButton("No", ()).addButton("Yes", onSell).display()
  }

  protected def likeButton (catId :Int, like :Boolean) = {
    val which = if (like) UI.like else UI.hate
    val state = game.likes.getView(catId)
    val cb = new CheckBox(Icons.image(which._2)) {
      override def select (selected :Boolean) {
        val newLike = if (selected) new JBoolean(like) else null
        game.gameSvc.setLike(catId, newLike).onFailure(onFailure).onSuccess(unitSlot {
          game.likes.put(catId, newLike)
        })
      }
    }.addStyles(Style.BACKGROUND.is(Background.image(which._1)))
    _dbag.add(state.map(rf {
      lk => java.lang.Boolean.valueOf(lk != null && lk.booleanValue == like)
    }).connectNotify(cb.checked.slot))
    cb
  }

  protected def background () :Background = Background.image(UI.pageBG)

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

  /** Called if the user swipes left across the screen. Defaults to NOOP. */
  protected def onSwipeLeft () {}

  /** Called if the user swipes right across the screen. Defaults to [pop]. */
  protected def onSwipeRight () { pop() }

  /** Called if a tap occurs on the screen where there are no interactive elements. */
  protected def onScreenTap (event :Pointer.Event) {} // noop

  protected def todo () = new Dialog().addTitle("TODO").addButton("OK", ()).display()

  protected var _back :Button = _
  protected val _dbag = new DestroyableBag
}
