//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN._
import react.IntValue
import tripleplay.ui._
import tripleplay.ui.layout.{AxisLayout, TableLayout}

class FlipCardsScreen (game :Everything) extends EveryScreen(game) {

  val freeFlips = new IntValue(0)
  val currency = new IntValue(100)

  override def createUI (root :Root) {
    val btnStyle = Style.FONT.is(UI.menuFont)
    val cards = new Group(new TableLayout(4).gaps(10, 10))
    val header = new Group(AxisLayout.horizontal()).add(
      new Button("Back").onClick(unitSlot { pop() }),
      UI.shim(15, 5),
      new Group(new TableLayout(2).gaps(0, 10)).add(
        new Label("Free flips:"), new ValueLabel(freeFlips),
        new Label("You have:"), UI.moneyIcon(currency)))
    root.add(UI.shim(5, 5),
             header,
             UI.stretchShim,
             cards,
             UI.shim(5, 5),
             new Label("Unflipped: I-6 II=3 III-5 ..."),
             UI.stretchShim)

    val cardBack = assets.getImage("images/card_back.png")
    for (ii <- 0 until 16) cards.add(UI.imageButton(cardBack).onClick(showCard _))
  }

  def showCard () {
    game.screens.push(new CardScreen(game, FakeData.yanluoCard), game.screens.slide)
  }
}
