//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.collection.JavaConversions._

import playn.core.Layer
import react.{Functions, IntValue, RMap, Value, Values}
import tripleplay.ui._
import tripleplay.ui.layout.{AxisLayout, TableLayout}

import com.threerings.everything.data._

class FlipCardsScreen (game :Everything) extends EveryScreen(game) {

  val freeFlips = new IntValue(0)
  val nextFlipCost = new IntValue(0)
  val unflipped = RMap.create[Rarity,Int]
  val haveUnflipped = Value.create(true :JBoolean)
  val cache = new UI.ImageCache(game)
  val selfId = game.self.get.userId
  var gridId = 0

  val cardGap = 2
  val cardCols = 4
  val cbox = new Box().setConstraint(Constraints.fixedSize(
    UI.card.size.width*cardCols + cardGap*(cardCols-1),
    UI.card.size.height*cardCols + cardGap*(cardCols-1)))
  val cardsEnabled = Value.create(true :JBoolean)

  // start the request for our cards immediately
  val getGrid = game.gameSvc.getGrid(Powerup.NOOP, false)

  val pupsBtn = new ImageButton(UI.getImage("pupbtn_up.png"), UI.getImage("pupbtn_down.png")) {
    override def setEnabled (enabled :Boolean) = {
      // fade our layer out when we're disabled
      if (!enabled && layer.alpha > 0) iface.animator.tweenAlpha(layer).to(0).in(500)
      else if (enabled && layer.alpha < 1) iface.animator.tweenAlpha(layer).to(1).in(500)
      super.setEnabled(enabled)
    }
  }

  // once our show transition is complete, create our cards and animate them into view
  onShown.connect(unitSlot {
    getGrid.onFailure(onFailure).onSuccess(slot { res =>
      noteStatus(res.status)
      gridId = res.grid.gridId
      res.grid.unflipped.zipWithIndex.foreach {
        case (count, idx) => unflipped.put(Rarity.values.apply(idx), count)
      }
      haveUnflipped.update(unflipped.values.sum > 0)

      val entree = CardButton.randomEntree()
      val cards = new Group(new TableLayout(cardCols).gaps(cardGap, cardGap))
      for (ii <- 0 until 16) {
        val card = cardWidget(ii)
        cards.add(card.update(res.grid.slots(ii), selfId, res.grid.flipped(ii)))
        // if the card is sold/gifted, just fade in the label, otherwise use a fancy entree
        res.grid.slots(ii) match {
          case SlotStatus.UNFLIPPED|SlotStatus.FLIPPED => card.entree(entree)
          case _ => // leave it as fadein
        }
      }
      cbox.set(cards)
    })
  }).once()

  override def createUI () {
    val haveFree = freeFlips.map(Functions.greaterThan(0))
    val showNextFree = Values.and(haveFree, haveUnflipped)
    val lackFree = freeFlips.map(Functions.lessThanEqual(0))
    val showNextCost = Values.and(lackFree, nextFlipCost.map(Functions.greaterThan(0)))
    val lackUnflipped = haveUnflipped.map(Functions.NOT)

    val uflabels = new Group(AxisLayout.horizontal).
      setStylesheet(Stylesheet.builder.add(
        classOf[Label], Style.FONT.is(UI.machineFont(12))).create).
      add(new Label("Unflipped cards:").bindVisible(haveUnflipped),
          new Label("Come back tomorrow for a fresh new grid!").bindVisible(lackUnflipped))
    Rarity.values foreach { r =>
      val label = new Label()
        unflipped.getView(r).connectNotify { count :Int =>
        label.text.update(s"$r-$count")
        label.setVisible(count > 0)
      }
      uflabels.add(label)
    }

    val status = UI.hgroup(
      AxisLayout.stretch(UI.hgroup(
        new Label("Free flips:").bindVisible(showNextFree),
        new ValueLabel(freeFlips).bindVisible(showNextFree),
        new Label("Next flip:").bindVisible(showNextCost),
        UI.moneyIcon(nextFlipCost, _dbag).bindVisible(showNextCost),
        new Label("No more flips.").bindVisible(lackUnflipped))),
      AxisLayout.stretch(UI.hgroup(
        pupsBtn.bindEnabled(haveUnflipped).onClick(unitSlot { showPupMenu() }))))

    // fade our extra bits in once we have our cards
    uflabels.layer.setAlpha(0)
    status.layer.setAlpha(0)
    getGrid.onSuccess(unitSlot {
      iface.animator.tweenAlpha(uflabels.layer).to(1).in(500).easeIn
      iface.animator.tweenAlpha(status.layer).to(1).in(500).easeIn
    })

    root.add(header("Flip Your Cards", purseLabel, UI.shim(1, 5)),
             status,
             UI.stretchShim,
             cbox.set(new Label("Getting cards...")),
             UI.shim(5, 2),
             uflabels,
             UI.stretchShim)
  }

  def noteStatus (status :GameStatus) {
    game.coins.update(status.coins)
    freeFlips.update(status.freeFlips)
    nextFlipCost.update(status.nextFlipCost)
  }

  def cardWidget (ii :Int) = new CardButton(game, this, cache, UI.card, cardsEnabled) {
    override protected def onReveal () {
      shaking.update(true)
      game.gameSvc.flipCard(gridId, ii, nextFlipCost.get).
        bindComplete(cardsEnabled.slot). // disable cards, while req is in-flight
        onFailure(onFlipFailure).
        onFailure(unitSlot { shaking.update(false) }).
        onSuccess(slot { res =>
          noteStatus(res.status)
          val r = res.card.thing.rarity
          unflipped.put(r, unflipped.get(r)-1)
          haveUnflipped.update(unflipped.values.sum > 0)
          reveal(res)
        })
    }
  }

  override protected def layout () :Layout = AxisLayout.vertical().offStretch.gap(0)

  def showPupMenu () {
    val dialog = new Dialog() {
      override def layout = AxisLayout.vertical.offStretch.gap(0)
      override def background = Background.blank()
    }.autoDismiss
    val itembg = UI.getImage("pupmenu/item.png")
    def item (elems :Element[_]*) = UI.hgroup(2).add(elems :_*).
      addStyles(Style.BACKGROUND.is(Background.image(itembg))).
      setConstraint(Constraints.fixedSize(itembg.width, itembg.height))
    val topimg = UI.getImage("pupmenu/top.png")
    dialog.add(UI.imageButton(topimg, topimg) { dialog.dismiss() })
    val zero = (v :JInteger) => if (v == null) java.lang.Integer.valueOf(0) else v
    for ((pup, name) <- PupInfo) {
      val action = UI.labelButton(name) {
        dialog.dismiss()
        game.gameSvc.usePowerup(gridId, pup).onFailure(onFailure).onSuccess(slot { grid =>
          game.pups.update(pup, game.pups.get(pup)-1)
          val cards = cbox.contents.asInstanceOf[Group]
          for (ii <- 0 until grid.flipped.size) {
            val card = cards.childAt(ii).asInstanceOf[CardButton]
            grid.slots(ii) match {
              case s @ SlotStatus.UNFLIPPED =>
                // unveil in a sweep from upper left to lower right; TODO: smoke puff over label?
                val (row, col) = (ii / 4, ii % 4)
                iface.animator.delay(50*row+50*col).`then`.action(new Runnable() {
                  def run () = card.update(s, selfId, grid.flipped(ii))
                })
              case _ => // nada
            }
          }
        })
      }.addStyles(Style.FONT.is(UI.machineFont(9)), Style.TEXT_WRAP.on, Style.UNDERLINE.off,
                  Style.HALIGN.left, Style.VALIGN.top)
      val have = game.pups.getView(pup).map(rf(zero))
      action.bindEnabled(have.map(Functions.greaterThan(0)))
      dialog.add(item(UI.pupIcon(pup),
                      action.setConstraint(Constraints.fixedSize(itembg.width/2, itembg.height-6)),
                      new ValueLabel(have).addStyles(Style.FONT.is(UI.machineFont(10)))))
    }
    dialog.add(UI.icon(UI.getImage("pupmenu/bot.png")))
    val spos = Layer.Util.layerToParent(pupsBtn.layer, layer, 0, 0)
    dialog.displayAt(spos.x, spos.y)
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

  // TODO: consolidate this with info from ShopScreen
  protected val PupInfo = Seq(
    (Powerup.SHOW_CATEGORY, "Reveal Category"),
    (Powerup.SHOW_SUBCATEGORY, "Reveal Sub-category"),
    (Powerup.SHOW_SERIES, "Reveal Series")
  )
}
