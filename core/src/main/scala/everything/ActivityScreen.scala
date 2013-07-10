//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout

import com.threerings.everything.data._

class ActivityScreen (game :Everything) extends EveryScreen(game) {

  // start our feed request ASAP
  val feed = game.everySvc.getRecentFeed()
  val contents = UI.stretchBox()

  override def createUI () {
    root.add(header("Goings On"), contents.set(new Label("Loading...")))
  }

  // defer the grindy grindy creation of a giant list until our show transition is completed
  override def showTransitionCompleted () {
    super.showTransitionCompleted()
    feed.onFailure(onFailure).onSuccess(slot { items =>
      val news = UI.vsgroup()
      for (ii <- 0 until items.length) {
        // formatting an item might suck in later items in the array, in which case they'll be
        // nulled out and we skip them
        if (items(ii) != null) news.add(itemWidget(items, ii))
      }
      contents.set(UI.vscroll(news))
    })
  }

  protected def itemWidget (items :Array[FeedItem], ii :Int) :Element[_] = {
    UI.plate(UI.icon(UI.frameImage(UI.friendImage(items(ii).actor), 50, 50)),
             UI.wrapLabel(formatItem(items, ii)),
             UI.tipLabel(game.device.formatDate(items(ii).when.getTime))).
      addStyles(Style.VALIGN.top)
  }

  protected def formatItem (items :Array[FeedItem], ii :Int) = {
    def getName (name :PlayerName, capital :Boolean) =
      if (game.self.get == name) if (capital) "You" else "you"
      else name.toString

    val item = items(ii)
    val buf = new StringBuilder
    buf.append(getName(item.actor, true))

    def addItems (what :String, pwhat :String) = {
      val objs = item.objects
      val haveMult = objs.size > 1
      for (ii <- 0 until objs.size) {
        if (ii > 0) buf.append(", ")
        if (ii == objs.size-1 && haveMult) buf.append("and ") // yay for English!
        buf.append(objs.get(ii))
      }
      buf.append(" ").append(if (haveMult) pwhat else what)
    }

    import FeedItem.Type._
    item.`type` match {
      case FLIPPED =>
        buf.append(" flipped the ")
        addItems("card", "cards")
        buf.append(".")

      case GOTGIFT =>
        def addGift (item :FeedItem) {
          buf.append("the ")
          addItems("card", "cards")
          buf.append(" from ").append(getName(item.target, false))
        }
        buf.append(" got ")
        addGift(item)
// TODO
//             int idate = DateUtil.getDayOfMonth(item.when);
//             for (int ii = 0; ii < items.size(); ii++) {
//                 FeedItem eitem = items.get(ii);
//                 if (eitem.actor.equals(item.actor) && eitem.type == item.type &&
//                     DateUtil.getDayOfMonth(eitem.when) == idate) {
//                     action.add(Widgets.newInlineLabel(", "));
//                     addGift(action, eitem);
//                     items.remove(ii--);
//                 }
//             }
        buf.append(".")

      // case COMMENT =>
      //   buf.append(" commented on your category ").append(item.objects.get(0))

      case COMPLETED =>
        buf.append(" completed the ")
        addItems("series", "series")
        buf.append("!")

      case NEW_SERIES =>
        buf.append(" added the ")
        addItems("series", "series")
        buf.append(".")

      case BIRTHDAY =>
        buf.append(" got the ")
        addItems("card", "cards")
        buf.append(" as a birthday present.")

      case JOINED =>
        buf.append(" started playing Everything. Yay!")

      case TROPHY =>
        buf.append(" earned the ")
        addItems("trophy", "trophies")
        buf.append("!")

      case _ =>
        buf.append(" did something mysterious.")
    }

    buf.toString
  }
}
