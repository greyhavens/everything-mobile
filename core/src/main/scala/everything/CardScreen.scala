//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.{Dimension, IDimension, IPoint, FloatMath, Point}
import react.{Value, UnitSignal}
import tripleplay.anim.Animation
import tripleplay.shaders.RotateYShader
import tripleplay.ui._
import tripleplay.ui.layout._
import tripleplay.util.Interpolator

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
               UI.stretchShim(),
               UI.button("Share") {
                 // TODO: shake the button or something if we have to wait...
                 _source.cardF.onSuccess(slot { card => showShareDialog(card, _source.counts) })
               },
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

  protected def showShareDialog (card :Card, counts :Option[(Int,Int)]) {
    val (me, thing, everyURL) = (game.self.get.name, card.thing.name, game.sess.get.everythingURL)
    val (msg, ref, tgtId) =
      if (counts.map(_._2 == 0).getOrElse(false)) //  series completed
        (s"$me got the $thing card and completed the ${card.getSeries} series!", "got_comp", null)
      else if (card.giver == null)
        (s"$me got the $thing card", "got_card", null)
      else if (card.giver.userId == Card.BIRTHDAY_GIVER_ID)
        (s"$me got the $thing card as a birthday present!", "got_bgift", null)
      else
        (s"$me got the $thing from ${card.giver}.", "got_gift", card.giver.facebookId.toString)
    val toArgs = if (tgtId != null) Map("to" -> tgtId) else Map[String,String]()
    game.fb.showDialog("feed", toArgs ++ Map(
      "name" -> thing,
      "caption" -> msg,
      "description" -> card.thing.descrip,
      "link" -> everyURL,
      "picture" -> s"${game.sess.get.backendURL}cardimg?thing=${card.thing.thingId}",
      "actions" -> s"[ { 'name': 'Collect Everything!', 'link': '${everyURL}' } ]",
      "ref" -> ref)).onFailure(onFailure).onSuccess(slot { id =>
        PlayN.log.info(s"Shared on FB $id.")
      })
  }
}
