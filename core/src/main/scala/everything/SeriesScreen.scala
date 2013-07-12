//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.Value
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.ui.layout.TableLayout

import com.threerings.everything.data._

class SeriesScreen (game :Everything, who :PlayerName, path :Seq[String], catId :Int)
    extends EveryScreen(game) {

  def this (game :Everything, who :PlayerName, path :Seq[String], scard :SeriesCard) =
    this(game, who, path :+ scard.name, scard.categoryId)

  val cbox = UI.stretchBox()
  val getSeries = game.gameSvc.getSeries(who.userId, catId)
  onShown.connect(unitSlot {
    getSeries.onFailure(onFailure).onSuccess(slot { series =>
      val cache = new UI.ImageCache(game)
      val cardsEnabled = Value.create(true :JBoolean)
      val cards = new Group(new TableLayout(3).gaps(2, 2), Style.VALIGN.top)
      series.things.zipWithIndex.foreach {
        case (tc, idx) =>
          val status = if (tc == null) SlotStatus.UNFLIPPED else SlotStatus.FLIPPED
          cards.add(new CardButton(game, this, cache, UI.bigCard, cardsEnabled) {
            override def showSeriesLink = false
            override def viewNext (target :CardScreen, dir :Swipe.Dir) {
              val dc = if (dir == Swipe.Up) 1 else -1
              def next (ii :Int, dc :Int) = {
                val count = series.things.length
                (ii+count+dc)%count
              }
              def findAndView (ii :Int) {
                if (series.things(ii) == null) findAndView(next(ii, dc))
                else cards.childAt(ii).asInstanceOf[CardButton].view(target, dir)
              }
              findAndView(next(idx, dc))
            }
          }.update(status, who.userId, tc))
      }

      val footer = UI.hgroup(
        UI.shim(5, 5), likeButton(catId, false),
        UI.shim(5, 5), likeButton(catId, true),
        UI.stretchShim(),
        UI.tipLabel("Editor:"), UI.labelButton(series.creator.toString) {
          new CollectionScreen(game, series.creator).push()
        }, UI.shim(5, 5))
      cbox.set(UI.vscroll(UI.vsgroup(cards, footer, UI.shim(1, 1))))
    })
  }).once()

  override def createUI () {
    root.add(headerPlate(UI.icon(UI.frameImage(UI.friendImage(who), 36, 36)),
                         UI.headerLabel(who.toString), UI.pathLabel(path.dropRight(1))),
             UI.headerLabel(path.last),
             cbox.set(new Label("Loading...")))
  }

  // allow our UI to bump right up against the bottom of the screen
  override protected def insets () = super.insets().adjust(0, 0, -5, 0)
}
