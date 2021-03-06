//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.{Dimension, IDimension, IPoint, FloatMath, Point}
import react.{Value, UnitSignal}
import scala.util.Random

import tripleplay.anim.Animation
import tripleplay.shaders.RotateYShader
import tripleplay.ui._
import tripleplay.ui.layout._
import tripleplay.util.{Interpolator, StyledText, TextStyle}

import com.threerings.everything.data._

class CardScreen (game :Everything, cache :UI.ImageCache) extends EveryScreen(game) {

  def setMessage (msg :String) = {
    _msg = msg
    this
  }
  private var _msg :String = _
  private var _bubble :ImageLayer = _
  private val _giftLbl :Label = new Label()

  private var _cardp :ThingCardPlus = _
  private var _source :CardButton = _
  private var _cardViz :CardViz = _

  val cbox = new Group(new AbsoluteLayout()) {
    layer.setDepth(1) // render above everything else; this makes things look better when we slide
                      // cards on and off the screen
  }
  val cardSize = new Dimension(UI.megaCard.width, UI.megaCard.height-4-12)
  val cardPos = new Point((width-cardSize.width)/2, 0)
  val info = UI.vgroup0().setConstraint(AxisLayout.stretched(2))

  // DEBUG: key to trigger series complete animation
  // _dbag.add(game.keyDown.connect(slot { k =>
  //   if (k == Key.C) displayCompleteAnim()
  // }))

  abstract class CardGroup (cardp :ThingCardPlus)
      extends Group(AxisLayout.vertical().offStretch.gap(0)) {
    def background () = new Background() {
      override protected def instantiate (size :IDimension) =
        new LayerInstance(size, graphics.createImageLayer(UI.megaCard).setTranslation(-3, -4))
    }
    def addCats () {
      add(UI.pathLabel(cardp.path), UI.tipLabel(s"${cardp.pos+1} of ${cardp.things}"))
    }
    def addContents ()
    addStyles(Style.BACKGROUND.is(background().inset(6, 27, 6, 14)))
    add(UI.shim(1, 2),
        UI.hgroup(UI.shim(17, 1), AxisLayout.stretch(UI.headerLabel(cardp.name)),
                  UI.shim(17, 1)))
    addContents()
    add(UI.shim(1, 5))

    val onLayout = new UnitSignal()
    override protected def layout () {
      super.layout()
      onLayout.emit()
    }
  }

  class CardViz (cardp :ThingCardPlus, source :CardButton) {
    def viz = if (front.layer.visible) front.layer else back.layer

    val front = new CardGroup(cardp) {
      def addContents () {
        val image = UI.frameImage(cache(cardp.image),
                                  Thing.MAX_IMAGE_WIDTH/2, Thing.MAX_IMAGE_HEIGHT/2)
        add(UI.hgroup(UI.shim(10, 5),
                      likeButton(cardp.categoryId, true),
                      UI.stretchShim(),
                      UI.subHeaderLabel(s"Rarity: ${cardp.rarity}"), UI.shim(15, 5),
                      UI.moneyIcon(cardp.rarity.value),
                      UI.stretchShim(),
                      tradeButton(cardp.categoryId),
                      UI.shim(10, 5)),
            UI.stretchShim(),
            UI.icon(image).addStyles(Style.ICON_POS.above),
            UI.stretchShim())
        addCats()
      }
    }
    cbox.add(AbsoluteLayout.at(front, cardPos, cardSize))

    var back :CardGroup = _
    def mkBack (thing :Thing) = new CardGroup(cardp) {
      def addContents () {
        addCats()
        add(UI.stretchShim(),
            UI.wrapLabel(thing.descrip).addStyles(Style.FONT.is(UI.factsFont)),
            UI.stretchShim(),
            new Label("Notes").addStyles(Style.FONT.is(UI.notesHeaderFont)),
            formatFacts(thing.facts.split("\n")),
            UI.stretchShim(),
            UI.hgroup(
              UI.subHeaderLabel("Source:"),
              UI.labelButton(nameSource(thing.source)) {
                PlayN.openURL(thing.source)
              }),
            UI.hgroup(UI.subHeaderLabel("Flipped on:"),
                      new Label(game.device.formatDate(cardp.received))))
      }
      override protected def wasAdded () {
        super.wasAdded()
        // set ourself to the proper size immediately upon being added, then force a validation;
        // this ensures that we're fully laid out (and all of our grindy grindy is done) before we
        // start the flip animation (for great smoothness)
        setSize(cardSize.width, cardSize.height)
        validate()
      }
    }

    // used to debounce clicks when loading/flipping
    val flipping = Value.create(false)

    def slideIn (dir :Swipe.Dir) {
      // position the card front off the screen (the bottom card is already invisible)
      front.layer.setOrigin(0, if (dir == Swipe.Down) height else -height)
      // start the slide in animation once the card front is validated
      front.onLayout.connect(unitSlot {
        iface.animator.tweenOrigin(front.layer).to(0, 0).in(300).easeOutBack
      }).once
    }

    def flip () {
      // debounce, in case player tap tap taps while we're loading or animating
      if (!flipping.get) {
        flipping.update(true)
        // TODO: shake, since this may require a server call?
        source.cardF.onSuccess(slot { card =>
          if (back == null) {
            back = mkBack(card.thing)
            back.layer.setVisible(false) // we'll be made visible by flip()
            cbox.add(AbsoluteLayout.at(back, cardPos, cardSize))
          }
          if (front.layer.visible) flip(front.layer, back.layer)
          else flip(back.layer, front.layer)
        })
      }
    }

    def slideOut (dir :Swipe.Dir, fast :Boolean) {
      val (oviz, ohid) = if (front.layer.visible) (front, back)
      else (back, front)
      // the old hidden card face can be destroyed immediately
      if (ohid != null) cbox.destroy(ohid)

      // slide the old vizible card face off the screen and then destroy it
      val oheight = if (dir == Swipe.Down) -height else height
      val interp = if (fast) Interpolator.EASE_OUT else Interpolator.EASE_INOUT
      iface.animator.tweenOrigin(oviz.layer).to(0, oheight).in(300).using(interp).`then`.
        action(new Runnable { def run () = { cbox.destroy(oviz) }})
    }

    protected def flip (from :Layer, to :Layer) {
      // create the rotating shaders
      val yCtr = 0.5f // TODO
      val topShader = new RotateYShader(graphics.ctx, 0.5f, yCtr, 1)
      from.setShader(topShader)
      val botShader = new RotateYShader(graphics.ctx, 0.5f, yCtr, 1)
      to.setShader(botShader)

      // run up our flipping animation
      iface.animator.setValue(flipping, true).
        `then`.tween(new Animation.Value() {
          def initial = 0
          def set (pct :Float) = {
            topShader.angle = FloatMath.PI * pct;
            botShader.angle = FloatMath.PI * (pct - 1);
            if (pct >= 0.5f && !_flipped) {
              from.setVisible(false)
              to.setVisible(true)
              _flipped = true
            }
          }
          var _flipped = false
        }).to(1).in(300).
        `then`.action(new Runnable() {
          def run () {
            from.setShader(null)
            to.setShader(null)
          }
        }).
        `then`.setValue(flipping, false)
    }

    def nameSource (source :String) = {
      if (source.indexOf("wikipedia.org") != -1) "Wikipedia"
      else {
        val ssidx = source.indexOf("//")
        val eidx = source.indexOf("/", ssidx+2)
        if (ssidx == -1) source
        else if (eidx == -1) source.substring(ssidx+2);
        else source.substring(ssidx+2, eidx);
      }
    }

    def formatFacts (facts :Array[String]) = {
      val ffont = Style.FONT.is(UI.notesFont)
      val lay = new TableLayout(TableLayout.COL.fixed, TableLayout.COL.stretch).alignTop.gaps(5, 5)
      (new Group(lay) /: facts)((g, f) => g.add(UI.glyphLabel("•").addStyles(ffont),
                                                UI.wrapLabel(f).addStyles(ffont)))
    }
  }

  def update (cardp :ThingCardPlus, source :CardButton, dir :Swipe.Dir) :this.type = {
    _cardp = cardp
    _source = source

    // create our card visualization (this will add the front viz to the UI)
    _cardViz = new CardViz(cardp, source)

    // if this update originated from a swipe, slide the card in, otherwise just add it
    if (dir != null) _cardViz.slideIn(dir)

    // update our info displays (TODO: fade the old one out and the new one in?)
    info.removeAll()

    // TODO: giver is always null when viewing from series screen, what to do?
    if (cardp.giver != null) {
      _giftLbl.text.update(cardp.giver.name match {
        case null => "A birthday gift from Everything!"
        case name => s"A gift from ${cardp.giver}!"
      })
      info.add(_giftLbl)
    }

    // omit the count info if this is a gift and we lack the space for two lines
    if (cardp.giver == null || height > 485) { // TODO
      info.add(countLabel(cardp, source.counts))
    }

    // if they just completed this series, show some fanfare
    source.counts match {
      case Some((0, 0)) => displayCompleteAnim()
      case _ => // nevermind
    }

    this
  }

  override def createUI () {
    if (height > 485) root.add(UI.stretchShim())
    root.add(cbox.setConstraint(Constraints.fixedSize(cardSize.width, cardSize.height)), info,
             UI.hgroup(
               UI.shim(5, 5),
               back(),
               UI.stretchShim(),
               UI.button("Sell") { maybeSellCard(_cardp.thing) { _source.queueSell() ; pop() }},
               UI.stretchShim(),
               UI.button("Gift") { new GiftCardScreen(game, cache, _cardp, _source).push() },
               // UI.stretchShim(),
               // UI.shareButton { showShareMenu() },
               UI.stretchShim()))
  }

  override def showTransitionCompleted () {
    super.showTransitionCompleted()

    // if we have a message from this giver, show it here
    if (_msg != null && _bubble == null) {
      _bubble = new Bubble(_msg, 210).above().tail(-75, 30).
        at(width-70, pos(_giftLbl.layer).y).toLayer(this)
      _bubble.setScale(0.1f)
      iface.animator.tweenScale(_bubble).to(1f).in(200)
      layer.add(_bubble)
    }
  }

  override protected def layout () = AxisLayout.vertical().gap(0).offStretch
  override protected def insets () = super.insets().adjust(0, 0, -5, 0)

  override protected def onGestureStart (startedOnChild :Boolean) = new Interaction(startedOnChild) {
    override def onDrag (event :Pointer.Event) {
      super.onDrag(event)
      if (_maxDist > Swipe.MaxTapDist && _source.canViewNext) {
        val dy = _start.y - event.y
        val viz = _cardViz.viz
        viz.setOrigin(viz.originX, dy)
      }
    }
    override def onSwipe (dir :Swipe.Dir) = dir match {
      case Swipe.Up|Swipe.Down => swipe(dir, true)
      case _                   => super.onSwipe(dir)
    }
    override def onFizzle (event :Pointer.Event) {
      val viz = _cardViz.viz
      // if we moved more than half the card distance, then just call it a swipe
      val dy = _start.y - event.y
      if (math.abs(dy) > cardSize.height/3) swipe(if (dy < 0) Swipe.Down else Swipe.Up, false)
      // otherwise ease the card back into position
      else iface.animator.tweenOrigin(viz).to(viz.originX, 0).in(300).easeInOut
    }
    override def onTap (event :Pointer.Event) {
      // if the player taps on the card, flip it!
      if (event.localY > cbox.y && event.localY < cbox.y + cbox.size.height) {
        if (_bubble != null) {
          iface.animator.tweenScale(_bubble).to(0f).in(200).`then`.destroy(_bubble)
          _bubble = null
        }
        _cardViz.flip()
      }
    }
  }

  protected def countLabel (card :ThingCardPlus, counts :Option[(Int,Int)]) :Element[_] = {
    def link (text :String) = UI.hgroup(
      new Label(text), UI.labelButton(card.categoryName) {
        new SeriesScreen(game, card.owner, card.path, card.categoryId).replace()
      })
    counts match {
      case Some((have, remain)) =>
        if (have > 1) new Label(s"You already have $have of these cards.")
        else if (have > 0) new Label("You already have this card.")
        else if (remain == 0) link("You completed ")
        else link(s"You have ${card.things - remain} of ${card.things}")
      case None =>
        if (_source.showSeriesLink) link(s"View") else new Label("")
    }
  }

  protected def swipe (dir :Swipe.Dir, fast :Boolean) {
    if (_source.canViewNext) {
      // slide the current card off the screen
      _cardViz.slideOut(dir, false)
      // trigger the download and display of the next card
      _source.viewNext(CardScreen.this, dir)
    }
  }

  protected def displayCompleteAnim () {
    val bigStyle = TextStyle.normal(UI.machineFont(46), UI.textColor).withOutline(0xFFFFFFFF, 2)
    val biggerStyle = bigStyle.withFont(UI.machineFont(72))

    def splashIn (preDelay :Long, text :ImageLayer, y :Float, midDelay :Long,
                  destX :Float, destY :Float) {
      text.setOrigin(text.width/2, text.height/2).setScale(0.01f).setDepth(Short.MaxValue)
      iface.animator.delay(preDelay).`then`.
        addAt(layer, text, width/2, y).`then`.
        tweenScale(text).easeOut.to(3).in(200).`then`.
        tweenScale(text).easeIn.to(1).in(300).`then`.
        delay(midDelay).`then`.
        tweenXY(text).easeOut.to(destX, destY).in(500).`then`.
        destroy(text)
    }

    val series = StyledText.span("SERIES", biggerStyle).toLayer()
    val sy = height/2-series.height/2
    splashIn(500, series, sy, 2000, -series.width/2, sy)

    val complete = StyledText.span("COMPLETE!", bigStyle).toLayer()
    val cy = height/2+complete.height/2
    splashIn(1000, complete, cy, 1500, width+complete.width/2, cy)

    val wtxt = Random.shuffle(Seq("YAY!", "WOO!", "YIPPEE!", "WHEE!", "HOORAY!")).head
    val woo = StyledText.span(wtxt, bigStyle).toLayer()
    woo.setOrigin(woo.width/2, woo.height/2).setDepth(Short.MaxValue)
    iface.animator.delay(4000).`then`.
      addAt(layer, woo, width/2, -woo.height/2).`then`.
      tweenY(woo).easeOut.to(height/2).in(300).`then`.
      delay(300).`then`.
      tweenY(woo).easeOut.to(height+woo.height/2).in(300).`then`.
      destroy(woo)
  }

  // protected def showShareMenu () {
  //   // "force" the loading of the card (which we'll need if we actually share); this allows the
  //   // loading to happen while the user is choosing a network; hide that latency!
  //   _source.cardF.onSuccess(unitSlot { /*noop!*/ })

  //   val dialog = new Dialog() {
  //     override def layout () = AxisLayout.vertical // no off-stretch
  //   }
  //   dialog.addTitle("Share to...").
  //     add(new Button("Facebook", Icons.image(UI.getImage("socks/facebook.png"))).onClick(unitSlot {
  //       dialog.dismiss()
  //       _source.cardF.onSuccess(slot { card => shareToFacebook(card, _source.counts) })
  //     })).
  //     addButton("Cancel", { dialog.dismiss() }).
  //     display()
  // }

  // protected def shareToFacebook (card :Card, counts :Option[(Int,Int)]) {
  //   val (ref, tgtId) =
  //     if (counts.map(_ == (0, 0)).getOrElse(false))
  //       // (s"$me got the $thing card and completed the ${card.getSeries} series!", "got_comp", null)
  //       ("got_comp", null) // TODO: how to express series completion as open graph object?
  //     else if (card.giver == null)
  //       // (s"$me got the $thing card", "got_card", null)
  //       ("got_card", null)
  //     else if (card.giver.userId == Card.BIRTHDAY_GIVER_ID)
  //       // (s"$me got the $thing card as a birthday present!", "got_bgift", null)
  //       ("got_bgift", null)
  //     else
  //       // (s"$me got the $thing from ${card.giver}.", "got_gift", card.giver.facebookId.toString)
  //       ("got_gift", card.giver.facebookId.toString)

  //   val url = game.sess.get.backendURL +
  //     s"#CARD~${card.owner.userId}~${card.thing.thingId}~${card.received}"
  //   game.fb.shareGotCard(card.thing.name, card.thing.descrip, game.cardImageURL(card.thing.image),
  //                        url, Category.getHierarchy(card.categories),
  //                        card.getSeries.toString, tgtId, ref).
  //     onFailure(onFailure).
  //     onSuccess(slot { id => PlayN.log.info(s"Shared on FB $id.") })
  // }
}
