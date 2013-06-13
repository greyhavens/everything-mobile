//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.{IntValue, RMap}
import tripleplay.ui._
import tripleplay.ui.layout.{AxisLayout, TableLayout}

import com.threerings.everything.data._

class FlipCardsScreen (game :Everything, status :GameStatus, grid :Grid) extends EveryScreen(game) {

  val freeFlips = new IntValue(0)
  val nextFlipCost = new IntValue(0)
  noteStatus(status)

  val cards = RMap.create[Int,ThingCard]
  grid.flipped.zipWithIndex.foreach {
    case (card, idx) => cards.put(idx, card)
  }

  val unflipped = RMap.create[Rarity,Int]
  grid.unflipped.zipWithIndex.foreach {
    case (count, idx) => unflipped.put(Rarity.values.apply(idx), count)
  }

  override def createUI (root :Root) {
    val header = new Group(AxisLayout.horizontal()).add(
      new Button("Back").onClick(unitSlot { pop() }),
      UI.shim(15, 5),
      new Group(new TableLayout(TableLayout.COL, TableLayout.COL.alignRight).gaps(0, 10)).add(
        // TODO: display next flip cost here instead of free flips when out of free flips
        //   if (status.freeFlips > 0) {
        //     _info.at(0, 0).setText("Free flips left: " + status.freeFlips);
        //   } else if (status.nextFlipCost > 0) {
        //     _info.at(0, 0).setWidget(new CoinLabel("Next flip costs ", status.nextFlipCost));
        //   } else {
        //     _info.at(0, 0).setText("No more flips.");
        //   }
        //   _info.at(0, 1).setWidget(new CoinLabel("You have ", _ctx.getCoins()), "right");
        new Label("Free flips:"), new ValueLabel(freeFlips),
        new Label("You have:"), UI.moneyIcon(game.coins, _dbag)))

    val cards = new Group(new TableLayout(4).gaps(10, 10))
    for (ii <- 0 until 16) cards.add(cardWidget(ii))

    val uflabels = new Group(AxisLayout.horizontal).
      setStylesheet(Stylesheet.builder.add(classOf[Label], Style.FONT.is(UI.textFont(12))).create).
      add(new Label("Unflipped cards:"))
    Rarity.values foreach { r =>
      val label = new Label()
        unflipped.getView(r).connectNotify { count :Int =>
        label.text.update(s"$r-$count")
        label.setVisible(count > 0)
      }
      uflabels.add(label)
    }

    root.add(UI.shim(5, 5),
             header,
             UI.stretchShim,
             cards,
             UI.shim(5, 5),
             uflabels,
             UI.stretchShim)
  }

  def noteStatus (status :GameStatus) {
    game.coins.update(status.coins)
    freeFlips.update(status.freeFlips)
    nextFlipCost.update(status.nextFlipCost)
  }

  def cardWidget (ii :Int) = {
    val view = UI.imageButton(UI.cardBack).onClick(flipCard(ii) _)
    cards.getView(ii).connectNotify { card :ThingCard =>
      if (card != null) {
        view.icon.update(Icons.image(UI.cardImage(card)))
      }
    }
    view
  }

  def flipCard (pos :Int)(btn :Button) {
    // TODO: shake the card or display a spinner to indicate that we're loading
    game.gameSvc.flipCard(grid.gridId, pos, nextFlipCost.get).
      onFailure(onFailure).onSuccess(slot[(CardResult, GameStatus)] {
        case (result, status) =>
          noteStatus(status)
          // TODO: delay this until after reveal animation
          cards.put(pos, result.card.toThingCard)
          grid.slots(pos) = SlotStatus.FLIPPED // TODO reactify?
          val r = result.card.thing.rarity
          unflipped.put(r, unflipped.get(r)-1)
          game.screens.push(new CardScreen(game, result), game.screens.slide)
      })
  }
}
