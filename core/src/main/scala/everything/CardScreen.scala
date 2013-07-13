//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.{Dimension, IDimension, IPoint, FloatMath, Point}
import react.UnitSignal
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

  private var _card :Card = _
  private var _counts :Option[(Int,Int)] = _
  private var _source :CardButton = _
  private var _cardFront :CardGroup = _
  private var _cardBack :CardGroup = _

  val cbox = new Group(new AbsoluteLayout()) {
    layer.setDepth(1) // render above everything else; this makes things look better when we slide
                      // cards on and off the screen
  }
  val cardSize = new Dimension(UI.megaCard.width, UI.megaCard.height-4-12)
  val cardPos = new Point((width-cardSize.width)/2, 0)
  val info = UI.vgroup0().setConstraint(AxisLayout.stretched(2))

  abstract class CardGroup (card :Card) extends Group(AxisLayout.vertical().offStretch.gap(0)) {
    def background () = new Background() {
      override protected def instantiate (size :IDimension) =
        new LayerInstance(size, graphics.createImageLayer(UI.megaCard).setTranslation(-3, -4))
    }
    def addCats () {
      add(UI.pathLabel(card.categories.map(_.name)),
          UI.tipLabel(s"${card.position+1} of ${card.things}"))
    }
    def addContents ()
    addStyles(Style.BACKGROUND.is(background().inset(6, 27, 6, 14)))
    add(UI.shim(1, 2),
        UI.hgroup(UI.shim(17, 1), AxisLayout.stretch(UI.headerLabel(card.thing.name)),
                  UI.shim(17, 1)))
    addContents()
    add(UI.shim(1, 5))

    val onLayout = new UnitSignal()
    override protected def layout () {
      super.layout()
      onLayout.emit()
    }
  }

  def update (card :Card, counts :Option[(Int,Int)], source :CardButton,
              dir :Swipe.Dir) :this.type = {
    _card = card
    _counts = counts
    _source = source

    // create and position our new card views
    _cardFront = AbsoluteLayout.at(new CardGroup(card) {
      def addContents () {
        val image = UI.frameImage(
          cache(card.thing.image), Thing.MAX_IMAGE_WIDTH/2, Thing.MAX_IMAGE_HEIGHT/2)
        add(UI.hgroup(UI.shim(10, 5),
                      likeButton(card.thing.categoryId, false),
                      UI.stretchShim(),
                      UI.subHeaderLabel(s"Rarity: ${card.thing.rarity}"), UI.shim(15, 5),
                      UI.moneyIcon(card.thing.rarity.value),
                      UI.stretchShim(),
                      likeButton(card.thing.categoryId, true),
                      UI.shim(10, 5)),
            UI.stretchShim(),
            UI.icon(image).addStyles(Style.ICON_POS.above),
            UI.stretchShim())
        addCats()
      }
    }, cardPos, cardSize)

    _cardBack = AbsoluteLayout.at(new CardGroup(card) {
      def addContents () {
        addCats()
        add(UI.stretchShim(),
            UI.wrapLabel(card.thing.descrip).addStyles(Style.FONT.is(UI.factsFont)),
            UI.stretchShim(),
            new Label("Notes").addStyles(Style.FONT.is(UI.notesHeaderFont)),
            formatFacts(card.thing.facts.split("\n")),
            UI.stretchShim(),
            UI.hgroup(
              UI.subHeaderLabel("Source:"),
              UI.labelButton(nameSource(card.thing.source)) {
                PlayN.openURL(card.thing.source)
              }),
            UI.hgroup(UI.subHeaderLabel("Flipped on:"),
                      new Label(game.device.formatDate(card.received))))
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
    }, cardPos, cardSize)
    // TODO: make the whole back card invisible (avoid layout); then layout, validate and then
    // animate on flip
    _cardBack.layer.setVisible(false)

    // add the new cards to the UI
    cbox.add(_cardFront, _cardBack)

    // if this update originated from a swipe, slide the card in, otherwise just add it
    if (dir != null) {
      // position the top card off the screen (the bottom card is already invisible)
      _cardFront.layer.setOrigin(0, if (dir == Swipe.Down) height else -height)
      // start the animation once the top card is validated
      _cardFront.onLayout.connect(unitSlot {
        // slide the new card front onto the screen
        iface.animator.tweenOrigin(_cardFront.layer).to(0, 0).in(300).easeOutBack
      }).once
    }

    // update our info displays (TODO: fade the old one out and the new one in?)
    info.removeAll()
    if (card.giver != null) {
      _giftLbl.text.update(card.giver.name match {
        case null => "A birthday gift from Everything!"
        case name => s"A gift from ${card.giver}!"
      })
      info.add(_giftLbl)
    }
    // omit the count info if this is a gift and we lack the space for two lines
    if (card.giver == null || height > 485) info.add(countLabel(card, counts))

    this
  }

  override def createUI () {
    if (height > 485) root.add(UI.stretchShim())
    root.add(cbox.setConstraint(Constraints.fixedSize(cardSize.width, cardSize.height)), info,
             UI.hgroup(
               UI.shim(5, 5),
               back(),
               UI.stretchShim(),
               UI.button("Sell") { maybeSellCard(_card.toThingCard) { _source.queueSell() ; pop() }},
               UI.stretchShim(),
               UI.button("Gift") { new GiftCardScreen(game, cache, _card, _source).push() },
               UI.stretchShim(),
               UI.button("Share") { showShareDialog(_card, _counts) },
               UI.stretchShim()))
  }

  override def showTransitionCompleted () {
    super.showTransitionCompleted()

    // if we have a message from this giver, show it here
    if (_msg != null && _bubble == null) {
      _bubble = new Bubble(_msg, 210).above().tail(-75, 30).at(width-70, _giftLbl.y).toLayer(this)
      _bubble.setScale(0.1f)
      iface.animator.tweenScale(_bubble).to(1f).in(200)
      layer.add(_bubble)
    }
  }

  override protected def layout () :Layout = AxisLayout.vertical().gap(0).offStretch

  override protected def onGestureStart (startedOnChild :Boolean) = new Interaction(startedOnChild) {
    override def onDrag (event :Pointer.Event) {
      super.onDrag(event)
      if (_maxDist > Swipe.MaxTapDist && _source.canViewNext) {
        val dy = _start.y - event.y
        val layer = if (_cardFront.layer.visible) _cardFront.layer else _cardBack.layer
        layer.setOrigin(layer.originX, dy)
      }
    }
    override def onSwipe (dir :Swipe.Dir) = dir match {
      case Swipe.Up|Swipe.Down => swipe(dir, true)
      case _                   => super.onSwipe(dir)
    }
    override def onFizzle (event :Pointer.Event) {
      val top = if (_cardFront.layer.visible) _cardFront else _cardBack
      // if we moved more than half the card distance, then just call it a swipe
      val dy = _start.y - event.y
      if (math.abs(dy) > top.size.height/3) swipe(if (dy < 0) Swipe.Down else Swipe.Up, false)
      // otherwise ease the card back into position
      else iface.animator.tweenOrigin(top.layer).to(top.layer.originX, 0).in(300).easeInOut
    }
    override def onTap (event :Pointer.Event) {
      // if the player taps on the card, flip it!
      if (event.localY > cbox.y && event.localY < cbox.y + cbox.size.height) {
        if (_bubble != null) {
          iface.animator.tweenScale(_bubble).to(0f).in(200).`then`.destroy(_bubble)
          _bubble = null
        }
        if (_cardBack.layer.visible) flip(_cardBack.layer, _cardFront.layer)
        else flip(_cardFront.layer, _cardBack.layer)
      }
    }
  }

  protected def countLabel (card :Card, counts :Option[(Int,Int)]) :Element[_] = {
    val cat = card.getSeries
    def link (text :String) = UI.hgroup(
      new Label(text), UI.labelButton(cat.name) {
        new SeriesScreen(game, card.owner, card.categories.map(_.name), cat.categoryId).replace()
      })
    counts match {
      case Some((have, remain)) =>
        if (have > 1) new Label(s"You already have $have of these cards.")
        else if (have > 0) new Label("You already have this card.")
        else if (remain == 0) link("You completed ")
        else link(s"You have ${cat.things - remain} of ${cat.things}")
      case None =>
        if (_source.showSeriesLink) link(s"View") else new Label("")
    }
  }

  protected def swipe (dir :Swipe.Dir, fast :Boolean) {
    if (_source.canViewNext) {
      val (oviz, ohid) = if (_cardFront.layer.visible) (_cardFront, _cardBack)
      else (_cardBack, _cardFront)
      // the old hidden card face can be destroyed immediately
      cbox.destroy(ohid)

      // slide the old vizible card face off the screen and then destroy it
      val oheight = if (dir == Swipe.Down) -height else height
      val interp = if (fast) Interpolator.EASE_OUT else Interpolator.EASE_INOUT
      iface.animator.tweenOrigin(oviz.layer).to(0, oheight).in(300).using(interp).`then`.
        action(new Runnable { def run () = { cbox.destroy(oviz) }})

      // trigger the download and display of the next card
      _source.viewNext(CardScreen.this, dir)
    }
  }

  protected def flip (from :Layer, to :Layer) {
    // create the rotating shaders
    val yCtr = 0.5f // TODO
    val topShader = new RotateYShader(graphics.ctx, 0.5f, yCtr, 1)
    from.setShader(topShader)
    val botShader = new RotateYShader(graphics.ctx, 0.5f, yCtr, 1)
    to.setShader(botShader)

    // run up our flipping animation
    iface.animator.tween(new Animation.Value() {
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
    }).to(1).in(300).`then`.action(new Runnable() {
      def run () {
        from.setShader(null)
        to.setShader(null)
      }
    })
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
    val toArgs = if (tgtId != null) Array("to", tgtId) else Array[String]()
    game.fb.showDialog("feed", toArgs ++ Array(
      "name", thing,
      "caption", msg,
      "description", card.thing.descrip,
      "link", everyURL,
      "picture", s"${game.sess.get.backendURL}cardimg?thing=${card.thing.thingId}",
      "actions", s"[ { 'name': 'Collect Everything!', 'link': '${everyURL}' } ]",
      "ref", ref)).onFailure(onFailure).onSuccess(slot { id =>
        PlayN.log.info(s"Shared on FB $id.")
      })
  }
}
