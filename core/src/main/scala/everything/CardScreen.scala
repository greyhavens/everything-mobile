//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import playn.core._
import pythagoras.f.{Dimension, IDimension, FloatMath, Point}
import tripleplay.anim.Animation
import tripleplay.shaders.RotateYShader
import tripleplay.ui._
import tripleplay.ui.layout._

import com.threerings.everything.data._

class CardScreen (
  game :Everything, cache :UI.ImageCache, card :Card, counts :Option[(Int,Int)], source :CardButton
) extends EveryScreen(game) {

  def setMessage (msg :String) = {
    _msg = msg
    this
  }
  private var _msg :String = _
  private var _giftLbl :Label = _
  private var _bubble :ImageLayer = _

  val cbox = new Group(new AbsoluteLayout())
  val cardSize = new Dimension(UI.megaCard.width, UI.megaCard.height-4-12)
  val cardPos = new Point((width-cardSize.width)/2, 0)

  abstract class CardGroup extends Group(AxisLayout.vertical().offStretch.gap(0)) {
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
  }

  val cardFront = AbsoluteLayout.at(new CardGroup() {
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

  val cardBack = AbsoluteLayout.at(new CardGroup() {
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

  override def createUI () {
    cardBack.layer.setVisible(false)
    root.add(UI.stretchShim(), cbox.add(cardFront, cardBack), UI.stretchShim())
    if (card.giver != null) {
      _giftLbl = new Label(card.giver.name match {
        case null => "A birthday gift from Everything!"
        case name => s"A gift from ${card.giver}!"
      })
      root.add(_giftLbl)
    }
    // omit the count info if this is a gift and we lack the space for two lines
    if (_giftLbl == null || height > 485) counts match {
      case Some((haveCount, thingsRemaining)) => root.add(status(haveCount, thingsRemaining, card))
      case None => // skip it
    }
    if (!root.childAt(root.childCount-1).isInstanceOf[Shim]) root.add(UI.stretchShim())
    root.add(UI.hgroup(
      UI.shim(5, 5),
      back(),
      UI.stretchShim(),
      UI.button("Sell") { maybeSellCard(card.toThingCard) { source.queueSell() ; pop() }},
      UI.stretchShim(),
      UI.button("Gift") { new GiftCardScreen(game, cache, card, source).push() },
      UI.stretchShim(),
      UI.button("Share") { showShareDialog() },
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

  override def onScreenTap (event :Pointer.Event) {
    // if the player taps on the card, flip it!
    if (event.localY > cbox.y && event.localY < cbox.y + cbox.size.height) {
      if (_bubble != null) {
        iface.animator.tweenScale(_bubble).to(0f).in(200).`then`.destroy(_bubble)
        _bubble = null
      }
      if (cardBack.layer.visible) flip(cardBack.layer, cardFront.layer)
      else flip(cardFront.layer, cardBack.layer)
    }
  }

  override protected def layout () :Layout = AxisLayout.vertical().gap(0).offStretch

  def flip (from :Layer, to :Layer) {
    from.setDepth(1).setVisible(true)
    to.setDepth(-1).setVisible(true)

    // create the rotating shaders
    val yCtr = 0.5f  // TODO
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
          from.setDepth(-1);
          to.setDepth(1)
          _flipped = true
        }
      }
      var _flipped = false
    }).to(1).in(300).`then`.action(new Runnable() {
      def run () {
        from.setShader(null).setDepth(0).setVisible(false)
        to.setShader(null).setDepth(0).setVisible(true)
      }
    })
  }

  def status (have :Int, remain :Int, card :Card) :Element[_] = {
    val cat = card.getSeries
    def link (text :String) = UI.hgroup(
      new Label(text), UI.labelButton(cat.name) {
        new SeriesScreen(game, game.self.get, card.categories.map(_.name), cat.categoryId).push()
      })

    if (have > 1) new Label(s"You already have $have of these cards.")
    else if (have > 0) new Label("You already have this card.")
    else if (remain == 0) link("You completed ")
    else link(s"You have ${cat.things - remain} of ${cat.things}")
  }

  protected def showShareDialog () {
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
