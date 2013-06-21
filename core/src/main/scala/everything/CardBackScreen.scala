//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.{Layer, Pointer, PlayN}
import pythagoras.f.Point
import tripleplay.ui._
import tripleplay.ui.layout.TableLayout

import com.threerings.everything.data._

class CardBackScreen (
  game :Everything, cache :UI.ImageCache, card :Card, counts :Option[(Int,Int)],
  upStatus :SlotStatus => Unit
) extends CardScreen(game, cache, card, upStatus) {

  override def createUI (root :Root) {
    root.add(UI.shim(10, 10),
             UI.headerLabel(card.thing.name),
             UI.tipLabel(Category.getHierarchy(card.categories)),
             UI.tipLabel(s"${card.position+1} of ${card.things}"),
             UI.stretchShim(),
             UI.wrapLabel(card.thing.descrip),
             UI.shim(5, 5),
             UI.subHeaderLabel("Facts"),
             formatFacts(card.thing.facts.split("\n")),
             UI.shim(5, 5),
             UI.hgroup(UI.subHeaderLabel("Source:"),
                       UI.labelButton(nameSource(card.thing.source)) {
                         PlayN.openURL(card.thing.source)
                       }),
             UI.hgroup(UI.subHeaderLabel("Flipped on:"), new Label(""+card.received)),
             UI.stretchShim(),
             buttons())

    root.layer.setHitTester(UI.absorber)
    root.layer.addListener(new Pointer.Adapter {
      override def onPointerStart (event :Pointer.Event) {
        game.screens.replace(new CardFrontScreen(game, cache, card, counts, upStatus),
                             pushTransition)
      }
    })
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
    val lay = new TableLayout(TableLayout.COL.fixed, TableLayout.COL.stretch).alignTop.gaps(5, 5)
    (new Group(lay) /: facts)((g, f) => g.add(new Label("•"), UI.wrapLabel(f)))
  }

  override protected def pushTransition = game.screens.flip.duration(300)
}
