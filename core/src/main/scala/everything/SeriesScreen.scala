//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.Value
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.ui.layout.TableLayout

import com.threerings.everything.data._

class SeriesScreen (game :Everything, who :PlayerName, path :Array[String], catId :Int)
    extends EveryScreen(game) {

  def this (game :Everything, who :PlayerName, path :Array[String], scard :SeriesCard) =
    this(game, who, path :+ scard.name, scard.categoryId)

  val cbox = UI.stretchBox()
  val crbox = new Box()
  val getSeries = game.gameSvc.getSeries(who.userId, catId)
  onShown.connect(unitSlot {
    getSeries.onFailure(onFailure).onSuccess(slot { series =>
      val cache = new UI.ImageCache(game)
      val cardsEnabled = Value.create(true :JBoolean)
      val cards = new Group(new TableLayout(4).gaps(2, 2), Style.VALIGN.top)
      series.things.foreach(tc => {
        val status = if (tc == null) SlotStatus.UNFLIPPED else SlotStatus.FLIPPED
        cards.add(new CardButton(game, this, cache, cardsEnabled).update(status, tc))
      })
      cbox.set(UI.vscroll(cards))
      crbox.set(UI.hgroup(UI.shim(5, 5),
                          likeButton(catId, false),
                          UI.shim(5, 5),
                          likeButton(catId, true),
                          UI.stretchShim(),
                          UI.tipLabel("Editor:"),
                          UI.labelButton(series.creator.toString) {
                            new CollectionScreen(game, series.creator).push()
                          },
                          UI.shim(5, 5)))
    })
  }).once()

  override def createUI () {
    root.add(headerPlate(UI.icon(UI.frameImage(UI.friendImage(who), 36, 36)),
                         UI.headerLabel(who.toString), UI.pathLabel(path.dropRight(1))),
             UI.headerLabel(path.last),
             cbox.set(new Label("Loading...")),
             crbox)
  }
}
