//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.JavaConversions._

import react.{Functions, IntValue, RMap, Values}
import tripleplay.ui._
import tripleplay.ui.layout.{AxisLayout, TableLayout}

import com.threerings.everything.data._

class FlipCardsScreen (game :Everything) extends EveryScreen(game) {

  val freeFlips = new IntValue(0)
  val nextFlipCost = new IntValue(0)
  val unflipped = RMap.create[Rarity,Int]
  val cache = new UI.ImageCache
  val cards = new Group(new TableLayout(4).gaps(10, 10))
  var grid :Grid = _

  def ctor () {
    // TODO: display a spinner over the button while we load the grid data
    val pup :Powerup = Powerup.NOOP // TODO
    val expectHave = false // TODO
    val load = new Dialog().addTitle("Getting cards...")
    load.display()
    game.gameSvc.getGrid(pup, expectHave).onFailure(onFailure).onSuccess(slot { res =>
      noteStatus(res.status)
      grid = res.grid
      res.grid.unflipped.zipWithIndex.foreach {
        case (count, idx) => unflipped.put(Rarity.values.apply(idx), count)
      }
      cards.zipWithIndex foreach {
        case (cb :CardButton, ii) => cb.update(grid.slots(ii), grid.flipped(ii))
      }
      load.dispose()
    })
  }
  ctor()

  override def createUI (root :Root) {
    val haveFree = freeFlips.map(Functions.greaterThan(0))
    val lackFree = freeFlips.map(Functions.lessThanEqual(0))
    val showNextFree = Values.and(lackFree, nextFlipCost.map(Functions.greaterThan(0)))
    val showNoFlips = Values.and(lackFree, nextFlipCost.map(Functions.lessThanEqual(0)))

    for (ii <- 0 until 16) cards.add(cardWidget(ii))

    val uflabels = new Group(AxisLayout.horizontal).
      setStylesheet(Stylesheet.builder.add(
        classOf[Label], Style.FONT.is(UI.writingFont(12))).create).
      add(new Label("Unflipped cards:"))
    Rarity.values foreach { r =>
      val label = new Label()
        unflipped.getView(r).connectNotify { count :Int =>
        label.text.update(s"$r-$count")
        label.setVisible(count > 0)
      }
      uflabels.add(label)
    }

    root.add(header("Flip Your Cards"),
             UI.shim(5, 5),
             UI.hgroup(
               new Label("You have:"), UI.moneyIcon(game.coins, _dbag),
               UI.shim(25, 5),
               new Label("Free flips:").bindVisible(haveFree),
               new ValueLabel(freeFlips).bindVisible(haveFree),
               new Label("Next flip:").bindVisible(showNextFree),
               UI.moneyIcon(nextFlipCost, _dbag).bindVisible(showNextFree),
               new Label("No more flips.").bindVisible(showNoFlips)),
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

  def cardWidget (ii :Int) = new CardButton(game, this, cache) {
    override protected def onReveal () {
      shaking.update(true)
      game.gameSvc.flipCard(grid.gridId, ii, nextFlipCost.get).
        bindComplete(enabledSlot). // disable while req is in-flight
        onFailure(onFlipFailure).
        onSuccess(slot { res =>
          noteStatus(res.status)
          val r = res.card.thing.rarity
          unflipped.put(r, unflipped.get(r)-1)
          reveal(res)
        })
    }
  }

  protected val onFlipFailure = (cause :Throwable) => cause.getMessage match {
    case "e.nsf_for_flip" => new Dialog().
        addTitle("Oops, Out of Coins!").
        addText("Wait 'til tomorrow for more free flips?\n" +
          "Or get coins now and keep flipping!").
        addButton("Wait", ()).
        addButton("Get Coins!", new ShopScreen(game).push()).
        display()
    case _ => onFailure.apply(cause)
  }
}
