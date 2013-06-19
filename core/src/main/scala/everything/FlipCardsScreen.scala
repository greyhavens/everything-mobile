//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.{Functions, IntValue, RMap, Values}
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

  val slots = RMap.create[Int,SlotStatus]
  grid.slots.zipWithIndex.foreach {
    case (status, idx) => slots.put(idx, status)
  }

  val unflipped = RMap.create[Rarity,Int]
  grid.unflipped.zipWithIndex.foreach {
    case (count, idx) => unflipped.put(Rarity.values.apply(idx), count)
  }

  val cache = new UI.ImageCache

  override def createUI (root :Root) {
    val haveFree = freeFlips.map(Functions.greaterThan(0))
    val lackFree = freeFlips.map(Functions.lessThanEqual(0))
    val showNextFree = Values.and(lackFree, nextFlipCost.map(Functions.greaterThan(0)))
    val showNoFlips = Values.and(lackFree, nextFlipCost.map(Functions.lessThanEqual(0)))

    val header = new Group(AxisLayout.horizontal()).add(
      new Button("Back").onClick(unitSlot { pop() }),
      UI.shim(15, 5),
      new Group(new TableLayout(TableLayout.COL.alignRight,
                                TableLayout.COL.alignRight).gaps(0, 10)).add(
        new Label("You have:"), UI.moneyIcon(game.coins, _dbag),
        new Label("Free flips:").bindVisible(haveFree),
        new ValueLabel(freeFlips).bindVisible(haveFree),
        new Label("Next flip:").bindVisible(showNextFree),
        UI.moneyIcon(nextFlipCost, _dbag).bindVisible(showNextFree),
        TableLayout.colspan(new Label("No more flips.").bindVisible(showNoFlips), 2)))

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
    slots.get(ii) match {
      case SlotStatus.UNFLIPPED|SlotStatus.FLIPPED =>
        cards.getView(ii).connectNotify { card :ThingCard =>
          if (card != null) {
            view.icon.update(Icons.image(UI.cardImage(cache, card)))
          }
        }
      case _ => // ignore
    }
    slots.getView(ii).connectNotify { status :SlotStatus => status match {
      case SlotStatus.GIFTED => view.icon.update(Icons.image(UI.statusImage("Gifted!")))
      case   SlotStatus.SOLD => view.icon.update(Icons.image(UI.statusImage("Sold!")))
      case _ => // ignore
    }}
    view
  }

  def flipCard (pos :Int)(btn :Button) {
    slots.get(pos) match {
      case SlotStatus.UNFLIPPED =>
        // TODO: shake the card or display a spinner to indicate that we're loading
        game.gameSvc.flipCard(grid.gridId, pos, nextFlipCost.get).
          onFailure((cause :Throwable) => cause.getMessage match {
            case "e.nsf_for_flip" => new Dialog().
                addTitle("Oops, Out of Coins!").
                addText("Wait 'til tomorrow for more free flips?\n" +
                  "Or get coins now and keep flipping!").
                addButton("Wait", ()).
                addButton("Get Coins!", new ShopScreen(game).push()).
                display()
            case _ => onFailure.apply(cause)
          }).
          onSuccess(slot { res =>
            noteStatus(res.status)
            // TODO: delay this until after reveal animation
            cards.put(pos, res.card.toThingCard)
            slots.put(pos, SlotStatus.FLIPPED)
            val r = res.card.thing.rarity
            unflipped.put(r, unflipped.get(r)-1)
            new CardScreen(game, cache, res, slots.put(pos, _)).push
          })
      case SlotStatus.FLIPPED =>
        val card = cards.get(pos)
        game.gameSvc.getCard(new CardIdent(game.self.get.userId, card.thingId, card.received)).
          onFailure(onFailure).
          onSuccess(slot { card =>
            new CardScreen(game, cache, card, None, slots.put(pos, _)).push
          })
      case _ => // nada, we have already sold or gifted it
    }
  }
}
