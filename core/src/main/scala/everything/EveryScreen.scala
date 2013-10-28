//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.{IDimension, IPoint, MathUtil, Rectangle, Point}
import react.{Value, Signal, UnitSignal}

import tripleplay.game.{ScreenStack, UIScreen}
import tripleplay.ui._
import tripleplay.ui.bgs.BlankBackground
import tripleplay.ui.layout.AxisLayout
import tripleplay.ui.util.Insets
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
      Background.croppedImage(UI.pageBG).inset(4, 9, 9, 9))

    def display () {
      if (buttons.childCount > 0) root.add(buttons)
      // compute our preferred size, then confine it to fit inside the screen
      val psize = root.preferredSize(width-20, 0)
      root.setSize(MathUtil.clamp(psize.width, 200, width-20), math.min(psize.height, height-20))
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

  // handles custom gesture stuffs
  class Interaction (startedOnChild :Boolean) {
    def onStart (event :Pointer.Event) {
      _start.set(event.x, event.y)
      _lastMove = event.time
      _maxDist = 0
    }

    def onDrag (event :Pointer.Event) {
      // TODO: if we stop for a while and then start dragging again, reset _start?
      _lastMove = event.time
      _maxDist = math.max(_maxDist, _start.distance(event.x, event.y))
    }

    def onEnd (event :Pointer.Event) {
      val duration = event.time - _lastMove
      val (xdist, ydist) = (math.abs(event.x - _start.x), math.abs(event.y - _start.y))
      // TODO: be more lenient about y direction as long as we're mostly moving horiz?
      val horizSwipe = xdist > width/3 && ydist < height/8
      // TODO: be more lenient about x direction as long as we're mostly moving vert?
      val vertSwipe = ydist > width/3 && xdist < height/8
      // if we seem to be swiping, handle it
      if (duration < Swipe.MaxSwipeTime && (horizSwipe || vertSwipe)) {
        if (horizSwipe) onSwipe(if (event.x > _start.x) Swipe.Right else Swipe.Left)
        else if (vertSwipe) onSwipe(if (event.y > _start.y) Swipe.Down else Swipe.Up)
      }
      // if we seem to be tapping and didn't start on an existing UI element, do our tap action
      else if (_maxDist < Swipe.MaxTapDist && !startedOnChild) onTap(event)
      else onFizzle(event)
    }

    // TODO: move all the interaction tracking in here, base swipe detection on velocity vectory,
    // add flick animation which moves a layer based on velocity (and friction?) and stops after it
    // reachs zero velocity or passes a predefined point

    /** Called if we recognize a swipe gesture in the specified direction. */
    def onSwipe (dir :Swipe.Dir) :Unit = dir match {
      case Swipe.Right => pop()
      case _ => // nada
    }

    /** Called if we recognize a tap gesture. */
    def onTap (event :Pointer.Event) {} // noop!

    /** Called if we recognize no particular gesture. */
    def onFizzle (event :Pointer.Event) {} // noop!

    val _start = new Point()
    var _lastMove = 0d
    var _maxDist = 0f
  }

  /** A value updated in `wasHidden`/`wasShown`. */
  val isVisible = Value.create(false)

  /** A signal emitted in `showTransitionCompleted`. */
  val onShown = Signal.create[EveryScreen]()

  /** Handles service failure by popping up a dialog. */
  val onFailure = (cause :Throwable) => failureDialog(cause, "OK", ()).display()

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

  def failureDialog (cause :Throwable, button :String, action : =>Unit) :Dialog = {
    val msg = cause.getMessage
    if (msg == null || !msg.startsWith("e.")) log.warn("Erm, failure", cause)
    new Dialog().addTitle("Oops!").addText(I18n.xlate(msg)).addButton(button, action)
  }

  override def wasAdded () {
    root.addStyles(Style.BACKGROUND.is(_background.insets(insets())))
    createUI()
    root.setSize(width, height)
    // wire up the (hardware) back button handler
    _dbag.add(game.keyDown.connect(slot { k =>
      if (k == Key.BACK) onHardwareBack()
    }))
    // react to pointer events to detect swipes
    root.layer.setHitTester(UI.absorber)
    root.layer.addListener(new Pointer.Adapter {
      override def onPointerStart (event :Pointer.Event) = {
        _interact = onGestureStart(event.hit != root.layer)
        _interact.onStart(event)
      }
      override def onPointerDrag (event :Pointer.Event) {
        _interact.onDrag(event)
      }
      override def onPointerEnd (event :Pointer.Event) = {
        _interact.onEnd(event)
      }
      var _interact :Interaction = _
    })
  }

  override def wasShown () {
    super.wasShown()
    isVisible.update(true)
  }

  override def hideTransitionStarted () {
    super.hideTransitionStarted()
    if (haveBackground) game.notebook.setVisible(true)
  }

  override def showTransitionCompleted () {
    super.showTransitionCompleted()
    onShown.emit(this)
    if (haveBackground) game.notebook.setVisible(false)
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
      add(UI.hgroup(UI.wrapLabel(s"Sell ${card.name}?"))).
      add(UI.hgroup(UI.label("Proceeds:", UI.moneyFont), UI.moneyIcon(amount))).
      addButton("No", ()).addButton("Yes", onSell).display()
  }

  protected def likeButton (catId :Int, like :Boolean) = {
    val btn = new TwoStateButton(UI.likeImage(0), UI.likeImage(1), UI.likeImage(2), UI.likeImage(3))
    btn.state.connect(slot { like =>
      val jlike = if (like) new JBoolean(true) else null
      game.gameSvc.setLike(catId, jlike).onFailure(onFailure).onSuccess(unitSlot {
        game.likes.put(catId, jlike)
      })
    })
    _dbag.add(game.likes.getView(catId).map(rf { lk => lk != null && lk.booleanValue == like }).
      connectNotify(btn.state.slot))
    btn
  }

  protected def tradeButton (catId :Int) = {
    UI.shim(UI.tradeImage(0).width, UI.tradeImage(0).height)
    // val btn = new TwoStateButton(UI.tradeImage(2), UI.tradeImage(3),
    //                              UI.tradeImage(0), UI.tradeImage(1))
    // btn.state.connect(slot { want =>
    //   game.gameSvc.setWant(catId, want).onFailure(onFailure).onSuccess(unitSlot {
    //     if (want) game.wants.add(catId)
    //     else game.wants.remove(catId)
    //   })
    // })
    // _dbag.add(game.wants.containsView(catId).map(rf { _.booleanValue } ).
    //   connectNotify(btn.state.slot))
    // btn
  }

  protected def background () :Background = Background.blank()
  protected def haveBackground = !_background.isInstanceOf[BlankBackground]
  protected def insets () :Insets = new Insets(game.device.statusBarHeight, 5, 5, 5)

  protected def parchmentBG () :Background = new Background() {
    override protected def instantiate (size :IDimension) = {
      val scale = width / UI.pageBG.width
      new LayerInstance(size, new ImmediateLayer.Renderer() {
        def render (surf :Surface) {
          if (alpha != null) surf.setAlpha(alpha)
          var y = 0f
          val scaledHeight = UI.pageBG.height*scale
          while (y < size.height) {
            surf.drawImage(UI.pageBG, 0, y, width, scaledHeight)
            y += scaledHeight
          }
        }
      })
    }
  }

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

  /** Called when a pointer interaction begins. A screen can return an [Interaction] instance if it
    * wishes to participate in the gesture. */
  protected def onGestureStart (startedOnChild :Boolean) :Interaction = DefaultInteraction

  protected def todo () = new Dialog().addTitle("TODO").addButton("OK", ()).display()

  protected lazy val _background = background()
  protected var _back :Button = _
  protected val _dbag = new DestroyableBag

  protected final val DefaultInteraction = new Interaction(false)
}
