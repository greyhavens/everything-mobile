//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.ui.layout.TableLayout

import com.threerings.everything.data._

class CategoryScreen (game :Everything, who :PlayerName, title :String, ss :Seq[SeriesCard])
    extends EveryScreen(game) {

  override def createUI (root :Root) {
    val cats = new Group(new TableLayout(TableLayout.COL.fixed.alignRight,
                                         TableLayout.COL.alignLeft,
                                         TableLayout.COL.fixed.alignLeft).gaps(5, 10),
                         Style.VALIGN.top)
    ss foreach { s =>
      cats.add(new Label(s.glyph),
               (UI.labelButton(s.name) {
                 new SeriesScreen(game, who, title, s).push()
               }).addStyles(Style.HALIGN.left, Style.TEXT_WRAP.on),
               new Label(s"${s.owned} of ${s.things}"))
    }
    root.add(header("View Collection"),
             UI.hgroup(UI.icon(UI.frameImage(UI.friendImage(who), 50, 50)),
                       UI.vgroup(UI.headerLabel(who.toString),
                                 UI.hgroup(new Label(title))).
                         setConstraint(AxisLayout.stretched).addStyles(Style.HALIGN.left)),
             AxisLayout.stretch(cats))
  }
}
