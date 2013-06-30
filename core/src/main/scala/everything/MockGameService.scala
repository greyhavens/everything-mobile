//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.util.{Date, HashMap}
import react.IntValue
import scala.collection.JavaConversions._

import com.threerings.everything.data._
import com.threerings.everything.rpc.GameAPI._

/** A mocked `GameService` which allows us to test interaction locally. */
object MockGameService extends GameService with Mockery {

  val coins = new IntValue(4239)
  val freeFlips = new IntValue(2)
  val pups = new HashMap[Powerup,JInteger]

  val grid = {
    val grid = new Grid
    grid.gridId = 1
    grid.status = GridStatus.NORMAL
    grid.slots = Array.fill(Grid.SIZE)(SlotStatus.UNFLIPPED)
    grid.flipped = Array.fill(Grid.SIZE)(null :ThingCard)
    grid.unflipped = Array(8, 0, 8, 0, 0, 0, 0, 0, 0, 0)
    grid.expires = new Date(System.currentTimeMillis+24*60*60*1000L)
    grid
  }

  val start = System.currentTimeMillis
  val cards = Array.tabulate(Grid.SIZE) { pos =>
    if (pos % 2 == 0) FakeData.yanluoCard(start+pos) else FakeData.maltesersCard(start+pos)
  }

  def getCollection (ownerId :Int) = {
    failure("TODO")
  }

  def getSeries (ownerId :Int, categoryId :Int) = {
    failure("TODO")
  }

  def getCard (ident :CardIdent) = {
    success(FakeData.yanluoCard(ident.received))
  }

  def getGrid (pup :Powerup, expectHave :Boolean) = success({
    val r = new GridResult
    r.grid = grid
    r.status = status(grid.unflipped)
    r
  })

  def flipCard (gridId :Int, pos :Int, expectCost :Int) = {
    val card = cards(pos)
    def cardres = {
      val r = new FlipResult
      r.card = card
      r.haveCount = 1
      r.thingsRemaining = 10
      r.trophies = List()
      r.status = status(grid.unflipped)
      r
    }
    grid.slots(pos) match {
      case SlotStatus.UNFLIPPED =>
        val cost = nextFlipCost(grid.unflipped)
        // charge 'em
        if (freeFlips.get == 0 && coins.get < cost) failure("e.nsf_for_flip")
        else {
          if (freeFlips.get > 0) freeFlips.decrementClamp(1, 0)
          else coins.decrementClamp(cost, 0)
          grid.slots(pos) = SlotStatus.FLIPPED
          grid.unflipped(card.thing.rarity.ordinal) -= 1
          grid.flipped(pos) = card.toThingCard
          success(cardres)
        }

      case SlotStatus.FLIPPED =>
        success(cardres)

      case _ => failure(s"Card gone: ${grid.slots(pos)}")
    }
  }

  def sellCard (thingId :Int, created :Long) = {
    val revenue = (if (thingId == 1) Rarity.III else Rarity.I).saleValue
    val pos = cards.indexWhere(c => c.thing.thingId == thingId && c.received == created)
    grid.slots(pos) = SlotStatus.SOLD
    success({
      val r = new SellResult
      r.coins = coins.increment(revenue)
      r.newLike = null
      r
    })
  }

  def getGiftCardInfo (thingId :Int, created :Long) = {
    val result = new GiftInfoResult
    result.things = 10
    def friend (name :PlayerName, has :Int) = {
      val f = new FriendCardInfo()
      f.friend = name
      f.hasThings = has
      f
    }
    result.friends = List(friend(frank, 5), friend(kurt, 3), friend(ella, 7))
    success(result)
  }

  def giftCard (thingId :Int, created :Long, friendId :Int, message :String) = {
    success(())
  }

  def setLike (catId :Int, like :Option[Boolean]) = {
    failure("TODO")
  }

  def openGift (thingId :Int, created :Long) = {
    failure("TODO")
  }

  def getShopInfo () = success({
    val r = new ShopResult
    r.coins = coins.get
    r.powerups = pups
    r
  })

  def buyPowerup (pup :Powerup) = {
    failure("TODO")
  }

  def usePowerup (gridId :Int, pup :Powerup) = {
    def unbox (count :JInteger) = if (count == null) 0 else count.intValue
    unbox(pups.get(pup)) match {
      case 0 => failure("Lack powerup")
      case n =>
        pups.put(pup, n-1)
        // TODO: apply powerup to grid?
        success(grid)
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
