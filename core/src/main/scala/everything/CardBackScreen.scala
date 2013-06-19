//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.{Layer, Pointer}
import pythagoras.f.Point
import tripleplay.ui._
import tripleplay.ui.layout.TableLayout

import com.threerings.everything.data.{Card, Category}

class CardBackScreen (game :Everything, card :Card) extends EveryScreen(game) {

  override def createUI (root :Root) {
    root.setStylesheet(Stylesheet.builder.add(classOf[Label], Style.HALIGN.left).create)
    root.add(UI.headerLabel(card.thing.name),
             UI.tipLabel(Category.getHierarchy(card.categories)),
             UI.tipLabel(s"${card.position+1} of ${card.things}"),
             UI.shim(5, 5),
             new Label(card.thing.descrip).addStyles(Style.TEXT_WRAP.on),
             UI.shim(5, 5),
             new Label("Facts:"),
             formatFacts(card.thing.facts.split("\n")),
             UI.shim(5, 5),
             new Label("Source: " + nameSource(card.thing.source)), // TODO: link
             new Label("Flipped on: " + card.received))

    layer.setHitTester(new Layer.HitTester {
      def hitTest (layer :Layer, p :Point) = layer
    })
    layer.addListener(new Pointer.Adapter {
      override def onPointerStart (event :Pointer.Event) = pop()
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

  override protected def popTransition = game.screens.flip.duration(400)
}
