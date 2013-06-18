//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.util.{Date, HashMap}
import react.{IntValue, RFuture}

import com.threerings.everything.data._
import com.threerings.everything.rpc.JSON._

/** A mocked `GameService` which allows us to test interaction locally. */
object MockGameService extends GameService with Mockery {

  val coins = new IntValue(42)
  val freeFlips = new IntValue(2)
  val pups = new HashMap[Powerup,JInteger]

  val grid = {
    val grid = new Grid
    grid.gridId = 1
    grid.status = GridStatus.NORMAL
    grid.slots = Array.fill(Grid.GRID_SIZE)(SlotStatus.UNFLIPPED)
    grid.flipped = Array.fill(Grid.GRID_SIZE)(null :ThingCard)
    grid.unflipped = Array(8, 0, 8, 0, 0, 0, 0, 0, 0, 0)
    grid.expires = new Date(System.currentTimeMillis+24*60*60*1000L)
    grid
  }

  val start = System.currentTimeMillis
  val cards = Array.tabulate(Grid.GRID_SIZE) { pos =>
    if (pos % 2 == 0) FakeData.yanluoCard(start+pos) else FakeData.maltesersCard(start+pos)
  }

  def getCollection (ownerId :Int) = {
    RFuture.failure(new Throwable("TODO"))
  }

  def getSeries (ownerId :Int, categoryId :Int) = {
    RFuture.failure(new Throwable("TODO"))
  }

  def getCard (ident :CardIdent) = {
    RFuture.success(FakeData.yanluoCard(ident.received))
  }

  def getGrid (pup :Powerup, expectHave :Boolean) = {
    RFuture.success(new GridResult(grid, status(grid.unflipped)))
  }

  def flipCard (gridId :Int, pos :Int, expectCost :Int) = {
    val card = cards(pos)
    def cardres = new FlipCardResult(card, 1, 10, Array(), status(grid.unflipped))
    grid.slots(pos) match {
      case SlotStatus.UNFLIPPED =>
        val cost = nextFlipCost(grid.unflipped)
        // charge 'em
        if (freeFlips.get == 0 && coins.get < cost) RFuture.failure(new Exception("e.nsf_for_flip"))
        else {
          if (freeFlips.get > 0) freeFlips.decrementClamp(1, 0)
          else coins.decrementClamp(cost, 0)
          grid.slots(pos) = SlotStatus.FLIPPED
          grid.unflipped(card.thing.rarity.ordinal) -= 1
          grid.flipped(pos) = card.toThingCard
          RFuture.success(cardres)
        }

      case SlotStatus.FLIPPED =>
        RFuture.success(cardres)

      case _ => RFuture.failure(new Exception(s"Card gone: ${grid.slots(pos)}"))
    }
  }

  def sellCard (thingId :Int, created :Long) = {
    val revenue = (if (thingId == 1) Rarity.III else Rarity.I).saleValue
    val pos = cards.indexWhere(c => c.thing.thingId == thingId && c.received == created)
    grid.slots(pos) = SlotStatus.SOLD
    RFuture.success(new SellCardResult(coins.increment(revenue), null))
  }

  def getGiftCardInfo (thingId :Int, created :Long) = {
    RFuture.failure(new Throwable("TODO"))
  }

  def giftCard (thingId :Int, created :Long, friendId :Int, message :String) = {
    RFuture.failure(new Throwable("TODO"))
  }

  def setLike (catId :Int, like :Option[Boolean]) = {
    RFuture.failure(new Throwable("TODO"))
  }

  def openGift (thingId :Int, created :Long) = {
    RFuture.failure(new Throwable("TODO"))
  }

  def getShopInfo () = RFuture.success(new ShopInfo(coins.get, pups))

  def buyPowerup (pup :Powerup) = {
    RFuture.failure(new Throwable("TODO"))
  }

  def usePowerup (gridId :Int, pup :Powerup) = {
    def unbox (count :JInteger) = if (count == null) 0 else count.intValue
    unbox(pups.get(pup)) match {
      case 0 => RFuture.failure(new Throwable("Lack powerup"))
      case n =>
        pups.put(pup, n-1)
        // TODO: apply powerup to grid?
        RFuture.success(grid)
    }
  }

  protected def status (unflipped :Array[Int]) :GameStatus = {
    status(coins.get, freeFlips.get, nextFlipCost(unflipped))
  }

  protected def nextFlipCost (unflipped :Array[Int]) = {
    // the cost of a flip is the expected value of the card
    var (total, count) = (0, 0)
    for (rarity <- Rarity.values()) {
      val cards = unflipped(rarity.ordinal)
      total += cards * rarity.value
      count += cards
    }
    if (count == 0) 0 else  (total / count)
  }
}
