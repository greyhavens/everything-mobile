//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import tripleplay.ui._
import tripleplay.ui.layout._

import com.threerings.everything.data._

class ShopScreen (game :Everything) extends EveryScreen(game) {

  override def createUI () {
    // refresh our powerup and coin state
    game.gameSvc.getShopInfo().onSuccess(slot { res =>
      game.coins.update(res.coins)
      game.pups.putAll(res.powerups)
    })

    // and build our UI
    def headerLabel (text :String) = new Label(text).addStyles(Style.FONT.is(UI.headerFont))

    val (cd, cl, clf, cr) = (TableLayout.COL.fixed, TableLayout.COL.alignLeft,
                             TableLayout.COL.fixed.alignLeft, TableLayout.COL.fixed.alignRight)

    def money (amount :Int) = UI.moneyIcon(amount).addStyles(Style.FONT.is(UI.writingFont(24)))
    val coins = UI.hgroup()
    game.device.getProducts.onSuccess(slot { ps =>
      coins.add(UI.stretchShim())
      for (p <- ps) coins.add(UI.vgroup0(money(p.coins), UI.button(p.price) { buyProduct(p) }),
                              UI.stretchShim())
    }).onFailure(slot { cause => coins.add(UI.wrapLabel(cause.getMessage)) })

    // COLS: icon ; name+descrip ; cost+charges
    val pups = new Group(new TableLayout(cd, cl, cd).gaps(5, 5))
    val descFont = UI.writingFont(14)
    PupInfo foreach { case (pup, name, descrip) =>
      val descVal = game.pups.getView(pup).map(rf { (_ :JInteger) match {
        case null  => descrip
        case count => s"$descrip\n(Have ${count})"
      }})
      val descLbl = UI.wrapLabel("").addStyles(Style.FONT.is(descFont))
      _dbag.add(descVal.connectNotify(descLbl.text.slot))
      val buy = UI.moneyButton(pup.cost) { btn =>
        game.gameSvc.buyPowerup(pup).
          bindComplete(btn.enabledSlot).
          onFailure(onFailure).
          onSuccess(unitSlot {
            val have = game.pups.get(pup) match {
              case null => 0
              case v    => v.intValue
            }
            game.pups.put(pup, have + pup.charges)
            game.coins.decrementClamp(pup.cost, 0)
            val bought = UI.statusCfg.toLayer("Purchased!")
            bought.setOrigin(bought.width/2, bought.height)
            iface.animator.addAt(btn.layer, bought, btn.size.width/2, btn.size.height).`then`.
              tweenY(bought).to(0).in(500).`then`.
              tweenAlpha(bought).to(0).in(500).`then`.
              destroy(bought)
          })
      }
      if (pup.isPermanent) {
        _dbag.add(game.pups.getView(pup).map(rf { c => new JBoolean(c == null || c == 0) }).
          connectNotify(buy.enabledSlot))
      }
      pups.add(UI.pupIcon(pup),
               new Group(AxisLayout.vertical.gap(0), Style.HALIGN.left).add(
                 UI.label(name, UI.notesHeaderFont), descLbl),
               if (pup.charges > 1) UI.vgroup0(buy, new Label(s"for ${pup.charges}"))
               else buy)
    }

    root.add(header("Get Coins", purseLabel, UI.shim(1, 5)),
             new Label("Get coins and flip every card in your grid!"), coins,
             AxisLayout.stretch(UI.vscroll(UI.vgroup(
               UI.shim(5, 10),
               UI.headerLabel("Get Powerups"),
               new Label("Flip smarter with powerups!"),
               pups))))
  }

  override protected def background () = parchmentBG()

  protected def buyProduct (prod :Product) {
    game.device.buyProduct(prod.sku).onSuccess(unitSlot {
      val d = new Dialog().addTitle("Processing purchase").addText("One moment please...")
      d.display()
      iface.animator.delay(1000).`then`.action(new Runnable() {
        def run = d.dismiss()
      })
    }).onFailure(onFailure)
  }

  val PupInfo = Seq(
    (Powerup.SHOW_CATEGORY, "Reveal Category",
     "Reveals the category of all unflipped cards in your grid."),
    (Powerup.SHOW_SUBCATEGORY, "Reveal Sub-category",
     "Reveals (just) the sub-category of all unflipped cards in your grid."),
    (Powerup.SHOW_SERIES, "Reveal Series",
     "Reveals (just) the series of all unflipped cards in your grid."),
    // (Powerup.ENSURE_ONE_VII, "Gimme a Seven",
    //  "Creates a grid with at least one rarity VII card."),
    // (Powerup.ENSURE_ONE_VIII, "Gimme an Eight",
    //  "Creates a grid with at least one rarity VIII card."),
    // (Powerup.ENSURE_ONE_IX, "Gimme a Nine",
    //  "Creates a grid with at least one rarity IX card."),
    // (Powerup.ENSURE_ONE_X, "Gimme a Ten",
    //  "Creates a grid with at least one rarity X card."),
    // (Powerup.ALL_NEW_CARDS, "All News is Good News",
    //  "Creates a grid with no cards that you already have."),
    // (Powerup.ALL_COLLECTED_SERIES, "No Surprises",
    //  "Creates a grid with only cards from (incomplete) series you have."),
    (Powerup.EXTRA_FLIP, "Free Flip",
     "Grants you one extra free flip per day forever.")
  )
}
