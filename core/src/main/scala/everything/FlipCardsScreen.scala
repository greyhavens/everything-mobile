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
  val cache = new UI.ImageCache(game)
  var gridId = 0

  val cardGap = 10
  val cardCols = 4
  val cbox = new Box().setConstraint(Constraints.fixedSize(
    UI.cardSize.width*cardCols + cardGap*(cardCols-1),
    UI.cardSize.height*cardCols + cardGap*(cardCols-1)))
  val cards = new Group(new TableLayout(cardCols).gaps(cardGap, cardGap))

  // start the request for our cards immediately
  val getGrid = {
    val pup :Powerup = Powerup.NOOP // TODO
    val expectHave = false // TODO
    game.gameSvc.getGrid(pup, expectHave)
  }

  override def createUI () {
    val haveFree = freeFlips.map(Functions.greaterThan(0))
    val lackFree = freeFlips.map(Functions.lessThanEqual(0))
    val showNextFree = Values.and(lackFree, nextFlipCost.map(Functions.greaterThan(0)))
    val showNoFlips = Values.and(lackFree, nextFlipCost.map(Functions.lessThanEqual(0)))

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

    root.add(header("Flip Your Cards").add(UI.moneyIcon(game.coins, _dbag), UI.shim(5, 5)),
             UI.shim(5, 5),
             UI.hgroup(
               new Label("Free flips:").bindVisible(haveFree),
               new ValueLabel(freeFlips).bindVisible(haveFree),
               new Label("Next flip:").bindVisible(showNextFree),
               UI.moneyIcon(nextFlipCost, _dbag).bindVisible(showNextFree),
               new Label("No more flips.").bindVisible(showNoFlips),
               UI.shim(25, 5),
               UI.imageButton(UI.getImage("pupbtn_up.png"), UI.getImage("pupbtn_down.png")) {
                 todo()
               }),
             UI.stretchShim,
             cbox.set(new Label("Getting cards...")),
             UI.shim(5, 5),
             uflabels,
             UI.stretchShim)
  }

  override def showTransitionCompleted () {
    super.showTransitionCompleted()
    // now that our show transition is complete, create our cards and animate them into view
    getGrid.onFailure(onFailure).onSuccess(slot { res =>
      // this gets called when we come *back* to this screen, so don't rebuild things then
      if (cbox.contents != cards) {
        noteStatus(res.status)
        gridId = res.grid.gridId
        res.grid.unflipped.zipWithIndex.foreach {
          case (count, idx) => unflipped.put(Rarity.values.apply(idx), count)
        }
        val entree = CardButton.randomEntree()
        for (ii <- 0 until 16) {
          val card = cardWidget(ii)
          cards.add(card.update(res.grid.slots(ii), res.grid.flipped(ii)))
          // if the card is sold/gifted, just fade in the label, otherwise use a fancy entree
          res.grid.slots(ii) match {
            case SlotStatus.UNFLIPPED|SlotStatus.FLIPPED => card.entree(entree)
            case _ => // leave it as fadein
          }
        }
        cbox.set(cards)
      }
    })
  }

  def noteStatus (status :GameStatus) {
    game.coins.update(status.coins)
    freeFlips.update(status.freeFlips)
    nextFlipCost.update(status.nextFlipCost)
  }

  def cardWidget (ii :Int) = new CardButton(game, this, cache) {
    override protected def onReveal () {
      shaking.update(true)
      game.gameSvc.flipCard(gridId, ii, nextFlipCost.get).
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
