//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.ui._
import tripleplay.ui.layout._

import com.threerings.everything.data._

class ShopScreen (game :Everything) extends EveryScreen(game) {

  override def createUI (root :Root) {
    def headerLabel (text :String) = new Label(text).addStyles(Style.FONT.is(UI.headerFont))

    val (cd, cl, cr) = (TableLayout.COL.fixed, TableLayout.COL.alignLeft,
                        TableLayout.COL.fixed.alignRight)

    val coins = new Group(new TableLayout(cl, cd, cr, cd).gaps(10, 5)).
      add(TableLayout.colspan(headerLabel("Get Coins"), 4),
          UI.moneyIcon( 5000), new Label("for"), new Label("$0.99"), new Button("BUY"),
          UI.moneyIcon(11000), new Label("for"), new Label("$1.99"), new Button("BUY"),
          UI.moneyIcon(24000), new Label("for"), new Label("$3.99"), new Button("BUY"))

    // COLS: icon ; name+descrip ; cost ; buy
    val pups = new Group(new TableLayout(cd, cl, cr, cd).gaps(10, 5)).
      setStylesheet(Stylesheet.builder.add(classOf[Label], Style.FONT.is(UI.textFont(10))).create)

    pups.add(UI.shim(5, 5), headerLabel("Powerup (Have)"), headerLabel("Cost"), UI.shim(5, 5))

    PupInfo foreach { case (pup, name, descrip) =>
      pups.add(new Label(Icons.image(UI.getImage(s"pup/${pup.name.toLowerCase}.png"))),
               new Group(AxisLayout.vertical, Style.HALIGN.left).add(
                 new Label(name + " (?)"),
                 new Label(descrip).addStyles(Style.HALIGN.left, Style.TEXT_WRAP.on)),
               new Group(AxisLayout.vertical, Style.HALIGN.right).add(
                 UI.moneyIcon(pup.cost),
                 new Label(if (pup.charges > 1) s"for ${pup.charges}" else "")),
               new Button("BUY"))
    }

    val content = new Group(AxisLayout.vertical).add(coins, UI.shim(5, 15), pups)
    root.add(AxisLayout.stretch(new Scroller(content).setBehavior(Scroller.Behavior.VERTICAL)),
             new Group(AxisLayout.horizontal).add(
               new Label("You have:"), UI.moneyIcon(game.coins, _dbag),
               UI.shim(50, 1), new Button("Back").onClick(pop _)))
  }

  val PupInfo = Seq(
    (Powerup.SHOW_CATEGORY, "Reveal Category",
     "Reveals the category of all unflipped cards in your grid."),
    (Powerup.SHOW_SUBCATEGORY, "Reveal Sub-category",
     "Reveals (just) the sub-category of all unflipped cards in your grid."),
    (Powerup.SHOW_SERIES, "Reveal Series",
     "Reveals (just) the series of all unflipped cards in your grid."),
    (Powerup.ENSURE_ONE_VII, "Gimme a Seven",
     "Creates a grid with at least one rarity VII card."),
    (Powerup.ENSURE_ONE_VIII, "Gimme an Eight",
     "Creates a grid with at least one rarity VIII card."),
    (Powerup.ENSURE_ONE_IX, "Gimme a Nine",
     "Creates a grid with at least one rarity IX card."),
    (Powerup.ENSURE_ONE_X, "Gimme a Ten",
     "Creates a grid with at least one rarity X card."),
    (Powerup.ALL_NEW_CARDS, "All News is Good News",
     "Creates a grid with no cards that you already have."),
    (Powerup.ALL_COLLECTED_SERIES, "No Surprises",
     "Creates a grid with only cards from (incomplete) series you have."),
    (Powerup.EXTRA_FLIP, "Free Flip",
     "Grants you one extra free flip per day forever. Limit one per customer.")
  )
}
