//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core._
import pythagoras.f.Point
import tripleplay.ui._
import tripleplay.ui.layout.TableLayout

import com.threerings.everything.data._

class CardBackScreen (
  game :Everything, cache :UI.ImageCache, card :Card, counts :Option[(Int,Int)], source :CardButton
) extends CardScreen(game, cache, card, counts, source) {

  override def createCardUI () {
    root.add(UI.wrapLabel(card.thing.descrip).addStyles(Style.FONT.is(UI.factsFont)),
             UI.stretchShim(),
             new Label("Notes").addStyles(Style.FONT.is(UI.notesHeaderFont)),
             formatFacts(card.thing.facts.split("\n")),
             UI.stretchShim(),
             UI.hgroup(UI.subHeaderLabel("Source:"),
                       UI.labelButton(nameSource(card.thing.source)) {
                         PlayN.openURL(card.thing.source)
                       }),
             UI.hgroup(UI.subHeaderLabel("Flipped on:"),
                       new Label(game.device.formatDate(card.received))))
  }

  override def onCardTap () {
    game.screens.replace(new CardFrontScreen(game, cache, card, counts, source), pushTransition)
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
    val ffont = Style.FONT.is(UI.factsFont)
    val lay = new TableLayout(TableLayout.COL.fixed, TableLayout.COL.stretch).alignTop.gaps(5, 5)
    (new Group(lay) /: facts)((g, f) => g.add(UI.glyphLabel("•").addStyles(ffont),
                                              UI.wrapLabel(f).addStyles(ffont)))
  }

  override protected def background = Background.image(UI.getImage("page_repeat.png"))

  override protected def pushTransition = game.screens.flip.duration(300)
}
