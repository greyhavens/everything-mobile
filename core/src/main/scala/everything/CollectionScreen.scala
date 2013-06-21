//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.JavaConversions._

import playn.core.Key
import react.IntValue
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.ui.layout.TableLayout

import com.threerings.everything.data._

class CollectionScreen (game :Everything, who :PlayerName) extends EveryScreen(game) {

  val things = new IntValue(0)
  val series = new IntValue(0)

  override def createUI (root :Root) {
    val cats = new Group(new TableLayout(TableLayout.COL.fixed.alignRight,
                                         TableLayout.COL.fixed,
                                         TableLayout.COL.fixed.alignLeft).gaps(0, 10)).add(
      TableLayout.colspan(new Label("Loading..."), 3))
    root.add(header("View Collection"),
             UI.hgroup(UI.icon(UI.frameImage(UI.friendImage(who), 50, 50)),
                       UI.vgroup(UI.headerLabel(who.toString),
                                 UI.hgroup(new Label("Things:"), new ValueLabel(things),
                                           new Label("Series:"), new ValueLabel(series))).
                         setConstraint(AxisLayout.stretched).addStyles(Style.HALIGN.left)),
             AxisLayout.stretch(UI.vscroll(UI.hgroup(UI.stretchShim, cats, UI.stretchShim))))

    game.gameSvc.getCollection(who.userId).onFailure(onFailure).onSuccess(popCats(cats) _)
  }

  def popCats (cats :Group)(col :PlayerCollection) {
    game.keyDown.connect(slot { key =>
      if (key == Key.R) popCats(cats)(col)
    }).once

    cats.removeAll()
    things.update(col.countCards)
    series.update(col.countSeries)
    for ((cat, subcats) <- col.series) {
      // cats.add(UI.shim(5, 5), new Label(cat), new Button("\u27a8"), UI.shim(5, 5)) // TODO
      subcats.keys.toList.zipWithIndex foreach {
        case (scat, idx) =>
          cats.add(if (idx == 0) new Label(cat) else UI.shim(5, 5)) // TOOD: lines
          cats.add(UI.icon(connector(idx, subcats.size)))
          cats.add(UI.labelButton(scat) {
            val title = s"$cat ${Category.SEP_CHAR} $scat"
            new CategoryScreen(game, who, title, subcats.get(scat)).push()
          })
      }
    }
  }

  def connector (idx :Int, count :Int) =
    if (idx == 0) {
      if (count == 1) line
      else tee
    }
    else if (idx == count-1) elbow
    else turnstile

  val tee = UI.getImage("lines/tee.png")
  val line = UI.getImage("lines/line.png")
  val elbow = UI.getImage("lines/elbow.png")
  val turnstile = UI.getImage("lines/turnstile.png")
}
