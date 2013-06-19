//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.game.{Screen, ScreenStack}
import tripleplay.ui._
import tripleplay.ui.layout.{AxisLayout, TableLayout}

import com.threerings.everything.data._
import com.threerings.everything.rpc.JSON.GiftCardInfo

class GiftCardScreen (game :Everything, card :ThingCard, upStatus :SlotStatus => Unit)
    extends EveryScreen(game) {

  override def createUI (root :Root) {
    val friends = new Group(new TableLayout(TableLayout.COL.fixed, TableLayout.COL.alignLeft,
                                            TableLayout.COL.fixed).gaps(5, 5))
    friends.add(UI.shim(5, 5), new Label("Loading..."), UI.shim(5, 5))
    val note = "Friends that already have this card are not shown."
    root.add(UI.headerLabel(s"Give ${card.name} to a friend!"),
             AxisLayout.stretch(new Scroller(friends).setBehavior(Scroller.Behavior.VERTICAL)),
             UI.tipLabel(note),
             new Group(AxisLayout.horizontal().gap(15)).add(
               new Button("Cancel").onClick(pop _),
               new Button("Sell").onClick(unitSlot {
                 maybeSellCard(card) {
                   upStatus(SlotStatus.SOLD)
                   clearParent() // remove our parent (card) screen from the stack as well
                 }
               })))

    game.gameSvc.getGiftCardInfo(card.thingId, card.received).
      onFailure(onFailure).
      onSuccess { res :GiftCardInfo =>
        friends.removeAll()
        res.friends.sorted.foreach { f =>
          val like = f.like match {
            case null => null
            case like => Icons.image(UI.getImage(if (like) "like/pos.png" else "like/neg.png"))
          }
          val hasBits = if (f.hasThings == 0) "" else s" (has ${f.hasThings}/${res.things})"
          friends.add(new Label(like),
                      new Label(s"${f.friend}$hasBits").addStyles(
                        Style.TEXT_WRAP.on, Style.HALIGN.left),
                      new Button("Give").onClick(showGivePopup(f.friend) _))
        }
      }
  }

  def showGivePopup (friend :PlayerName)(btn :Button) {
    // val extra = new Group(AxisLayout.vertical).add(
    new Dialog(s"Gift Card", s"Give ${card.name} to ${friend}") {
      override def okLabel = "Yes"
      override def cancelLabel = "No"
    }.onOK(giveCard(friend)).display()
  }

  def giveCard (friend :PlayerName) {
    game.gameSvc.giftCard(card.thingId, card.received, friend.userId, "").
      onFailure(onFailure).
      onSuccess(unitSlot {
        upStatus(SlotStatus.GIFTED)
        clearParent()
        pop()
      })
  }

  def clearParent () {
    game.screens.remove(new ScreenStack.Predicate {
      def apply (screen :Screen) = screen.isInstanceOf[CardScreen]
    })
  }
}
