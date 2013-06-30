//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.JavaConversions._

import playn.core.Keyboard
import playn.core.PlayN._
import tripleplay.game.{Screen, ScreenStack}
import tripleplay.ui._
import tripleplay.ui.layout.{AxisLayout, TableLayout}

import com.threerings.everything.data._

class GiftCardScreen (game :Everything, cache :UI.ImageCache, card :Card,
                      upStatus :SlotStatus => Unit) extends EveryScreen(game) {

  // start our data request immediately
  val giftInfo = game.gameSvc.getGiftCardInfo(card.thing.thingId, card.received)
  val fbox = UI.stretchBox()

  override def createUI () {
    val header = UI.plate(
      UI.icon(UI.cardImage(cache, card.toThingCard)),
      UI.headerLabel(card.thing.name),
      UI.pathLabel(card.categories.map(_.name), 12),
      UI.tipLabel(s"Rarity: ${card.thing.rarity} - E${card.thing.rarity.value}"))
    val buttons = UI.bgroup(
      back("Cancel"),
      UI.button("Sell") {
        maybeSellCard(card.toThingCard) {
          upStatus(SlotStatus.SOLD)
          clearParent() // remove our parent (card) screen from the stack as well
        }
      })
    val note = "Friends that have this card are not shown."
    root.add(header, new Label("Give to:"),
             fbox.set(new Label("Loading...")),
             UI.tipLabel(note), buttons)
  }

  override def showTransitionCompleted () {
    super.showTransitionCompleted()
    giftInfo.onFailure(onFailure).onSuccess(slot { res =>
      if (res.friends.isEmpty) fbox.set(new Label("All of your friends already have this card."))
      else {
        val friends = new Group(new TableLayout(
          TableLayout.COL.fixed, TableLayout.COL.alignLeft,
          TableLayout.COL.fixed, TableLayout.COL.fixed).gaps(5, 5))
        res.friends.sorted.foreach { f =>
          val like = f.like match {
            case null => UI.shim(5, 5)
            case like => UI.icon(UI.getImage(if (like) "like/pos.png" else "like/neg.png"))
          }
          val hasBits = if (f.hasThings == 0) "" else s" (has ${f.hasThings}/${res.things})"
          friends.add(like,
                      UI.wrapLabel(s"${f.friend}$hasBits"),
                      UI.button("Give")(showGivePopup(f.friend)),
                      UI.shim(5, 5))
        }
        fbox.set(UI.vscroll(friends))
      }
    })
  }

  def showGivePopup (friend :PlayerName) :Unit = new Dialog().
    add(UI.hgroup(UI.icon(UI.frameImage(UI.friendImage(friend), 50, 50)),
                  UI.vgroup(UI.headerLabel("Gift Card"),
                            UI.wrapLabel(s"Give ${card.thing.name} to ${friend.name}?")).
                    setConstraint(AxisLayout.stretched))).
    addButton("Cancel", ()).
    addButton("Give", giveCard(friend, "")).
    addButton("+Message", keyboard.getText(
      Keyboard.TextType.DEFAULT, s"Message to ${friend.name}:", "", cb { msg =>
        if (msg != null) giveCard(friend, msg)
        else showGivePopup(friend)
      })).
    display()

  def giveCard (friend :PlayerName, msg :String) {
    upStatus(SlotStatus.GIFTED)
    clearParent()
    pop()
    game.gameSvc.giftCard(card.thing.thingId, card.received, friend.userId, msg).
      // TODO: if gifting fails, display the error back on the main screen
      onFailure(onFailure)
  }

  def clearParent () {
    game.screens.remove(new ScreenStack.Predicate {
      def apply (screen :Screen) = screen.isInstanceOf[CardScreen]
    })
  }
}
