//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.ui.layout.TableLayout

import com.threerings.everything.data._

class SeriesScreen (game :Everything, who :PlayerName, path :Array[String], scard :SeriesCard)
    extends EveryScreen(game) {

  val cache = new UI.ImageCache

  override def createUI (root :Root) {
    val cards = new Group(new TableLayout(4).gaps(10, 10), Style.VALIGN.top)
    cards.add(TableLayout.colspan(new Label("Loading..."), 4))
    root.add(header(path(0)),
             UI.plate(UI.icon(UI.frameImage(UI.friendImage(who), 50, 50)),
                      UI.headerLabel(who.toString), UI.pathLabel(path.drop(1) :+ scard.name)),
             AxisLayout.stretch(UI.vscroll(cards)))

    game.gameSvc.getSeries(who.userId, scard.categoryId).onFailure(onFailure).
      onSuccess(slot { series =>
        cards.removeAll()
        series.things.foreach(tc => {
          val status = if (tc == null) SlotStatus.UNFLIPPED else SlotStatus.FLIPPED
          cards.add(new CardButton(game, cache).update(status, tc))
        })
      })
  }
}
