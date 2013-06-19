//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.Keyboard
import playn.core.PlayN._

import tripleplay.game.{Screen, ScreenStack}
import tripleplay.ui._
import tripleplay.ui.layout.{AxisLayout, TableLayout}

import com.threerings.everything.data._
import com.threerings.everything.rpc.JSON.GiftCardInfo

class GiftCardScreen (game :Everything, cache :UI.ImageCache, card :Card,
                      upStatus :SlotStatus => Unit) extends EveryScreen(game) {

  override def createUI (root :Root) {
    val header = UI.hgroup().add(
      UI.icon(UI.cardImage(cache, card.toThingCard)),
      UI.vgroup().add(
        UI.headerLabel(card.thing.name),
        UI.tipLabel(Category.getHierarchy(card.categories)),
        UI.tipLabel(s"Rarity: ${card.thing.rarity} - E${card.thing.rarity.value}")))
    val friends = new Group(new TableLayout(TableLayout.COL.fixed, TableLayout.COL.alignLeft,
                                            TableLayout.COL.fixed).gaps(5, 5)).add(
      UI.shim(5, 5), new Label("Loading..."), UI.shim(5, 5))
    val buttons = UI.hgroup(gap=25).add(
      UI.button("Cancel")(pop()),
      UI.button("Sell") {
        maybeSellCard(card.toThingCard) {
          upStatus(SlotStatus.SOLD)
          clearParent() // remove our parent (card) screen from the stack as well
        }
      })
    val note = "Friends that already have this card are not shown."
    root.add(header,
             new Label("Give to:"),
             AxisLayout.stretch(UI.vscroll(friends)),
             UI.tipLabel(note),
             buttons)

    game.gameSvc.getGiftCardInfo(card.thing.thingId, card.received).
      onFailure(onFailure).
      onSuccess { res :GiftCardInfo =>
        friends.removeAll()
        res.friends.sorted.foreach { f =>
          val like = f.like match {
            case null => UI.shim(5, 5)
            case like => UI.icon(UI.getImage(if (like) "like/pos.png" else "like/neg.png"))
          }
          val hasBits = if (f.hasThings == 0) "" else s" (has ${f.hasThings}/${res.things})"
          friends.add(like,
                      UI.wrapLabel(s"${f.friend}$hasBits"),
                      UI.button("Give")(showGivePopup(f.friend)))
        }
      }
  }

  def showGivePopup (friend :PlayerName) {
    // val extra = new Group(AxisLayout.vertical).add(
    new Dialog(s"Gift Card", s"Give ${card.thing.name} to ${friend.name}").
      addButton("Cancel", ()).
      addButton("Give", giveCard(friend, "")).
      addButton("Give + Message", keyboard.getText(
        Keyboard.TextType.DEFAULT, s"Message to ${friend.name}:", "", cb { msg =>
          if (msg != null) giveCard(friend, msg)
          else showGivePopup(friend)
        })).
      display()
  }

  def giveCard (friend :PlayerName, msg :String) {
    game.gameSvc.giftCard(card.thing.thingId, card.received, friend.userId, msg).
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
