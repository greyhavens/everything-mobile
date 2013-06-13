//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.util.Date
import react.RFuture

import com.threerings.everything.data._

/** A mocked `GameService` which allows us to test interaction locally. */
object MockGameService extends GameService with Mockery {

  def getCollection (ownerId :Int) :RFuture[PlayerCollection] = {
    RFuture.failure(new Throwable("TODO"))
  }

  def getSeries (ownerId :Int, categoryId :Int) :RFuture[Series] = {
    RFuture.failure(new Throwable("TODO"))
  }

  def getCard (ident :CardIdent) :RFuture[Card] = {
    RFuture.failure(new Throwable("TODO"))
  }

  def getGrid (pup :Powerup, expectHave :Boolean) :RFuture[(Grid, GameStatus)] = {
    val grid = new Grid
    grid.gridId = 1
    grid.status = GridStatus.NORMAL
    grid.slots = Array.fill(Grid.GRID_SIZE)(SlotStatus.UNFLIPPED)
    grid.flipped = Array.fill(Grid.GRID_SIZE)(null :ThingCard)
    grid.unflipped = Array(3, 1, 4, 1, 4, 2, 0, 2, 1, 0)
    grid.expires = new Date(System.currentTimeMillis+24*60*60*1000L)
    RFuture.success((grid, status(42, 2, 0)))
  }

  def flipCard (gridId :Int, pos :Int, expectCost :Int) :RFuture[(CardResult, GameStatus)] = {
    RFuture.success((CardResult(FakeData.yanluoCard, 5, 10, Seq()), status(42, 1, 0)))
  }

  def sellCard (thingId :Int, created :Long) :RFuture[(Int, Boolean)] = {
    RFuture.failure(new Throwable("TODO"))
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
}
