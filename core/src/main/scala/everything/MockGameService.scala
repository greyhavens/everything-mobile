//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.util.Date
import react.{IntValue, RFuture}

import com.threerings.everything.data._

/** A mocked `GameService` which allows us to test interaction locally. */
object MockGameService extends GameService with Mockery {

  val coins = new IntValue(42)
  val freeFlips = new IntValue(2)

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

  def getCollection (ownerId :Int) :RFuture[PlayerCollection] = {
    RFuture.failure(new Throwable("TODO"))
  }

  def getSeries (ownerId :Int, categoryId :Int) :RFuture[Series] = {
    RFuture.failure(new Throwable("TODO"))
  }

  def getCard (ident :CardIdent) = {
    RFuture.success(FakeData.yanluoCard(ident.received))
  }

  def getGrid (pup :Powerup, expectHave :Boolean) = {
    RFuture.success((grid, status(grid.unflipped)))
  }

  def flipCard (gridId :Int, pos :Int, expectCost :Int) = {
    val card = cards(pos)
    val cardres = CardResult(card, 1, 10, Seq())
    grid.slots(pos) match {
      case SlotStatus.UNFLIPPED =>
        val cost = nextFlipCost(grid.unflipped)
        // charge 'em
        if (freeFlips.get == 0 && coins.get < cost) RFuture.failure(new Exception("NSF!"))
        else {
          if (freeFlips.get > 0) freeFlips.decrementClamp(1, 0)
          else coins.decrementClamp(cost, 0)
          grid.slots(pos) = SlotStatus.FLIPPED
          grid.unflipped(card.thing.rarity.ordinal) -= 1
          grid.flipped(pos) = card.toThingCard
          RFuture.success((cardres, status(grid.unflipped)))
        }

      case SlotStatus.FLIPPED =>
        RFuture.success((cardres, status(grid.unflipped)))

      case _ => RFuture.failure(new Exception(s"Card gone: ${grid.slots(pos)}"))
    }
  }

  def sellCard (thingId :Int, created :Long) = {
    val revenue = (if (thingId == 1) Rarity.III else Rarity.I).saleValue
    val pos = cards.indexWhere(c => c.thing.thingId == thingId && c.received.getTime == created)
    grid.slots(pos) = SlotStatus.SOLD
    RFuture.success((coins.increment(revenue), None))
  }

  def getGiftCardInfo (thingId :Int, created :Long) :RFuture[(Int,Seq[FriendCardInfo])] = {
    RFuture.failure(new Throwable("TODO"))
  }

  def giftCard (thingId :Int, created :Long, friendId :Int, message :String) {
  }

  def setLike (catId :Int, like :Option[Boolean]) {
  }

  def openGift (thingId :Int, created :Long) :RFuture[(CardResult, String)] = {
    RFuture.failure(new Throwable("TODO"))
  }

  def getShopInfo () :RFuture[(Int, Map[Powerup,Int])] = {
    RFuture.failure(new Throwable("TODO"))
  }

  def buyPowerup (pup :Powerup) :RFuture[Unit] = {
    RFuture.failure(new Throwable("TODO"))
  }

  def usePowerup (gridId :Int, pup :Powerup) :RFuture[Grid] = {
    RFuture.failure(new Throwable("TODO"))
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
