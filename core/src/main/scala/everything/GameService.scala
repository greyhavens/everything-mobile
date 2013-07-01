//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.RFuture

import com.threerings.everything.data._
import com.threerings.everything.rpc.GameAPI._

/** Interface for game related things between the client and server. */
trait GameService {

  /** Fetches the specified player's collection. */
  def getCollection (ownerId :Int) :RFuture[PlayerCollection]

  /** Fetches info on the specified series. */
  def getSeries (ownerId :Int, categoryId :Int) :RFuture[Series]

  /** Fetches info on the specified card. */
  def getCard (ident :CardIdent) :RFuture[Card]

  /** Fetches the player's current grid and game status. */
  def getGrid (pup :Powerup, expectHave :Boolean) :RFuture[GridResult]

  /** Flips the specified card. Returns the player's new game status. */
  def flipCard (gridId :Int, pos :Int, expectCost :Int) :RFuture[FlipResult]

  /** Sells the specified card. */
  def sellCard (thingId :Int, created :Long) :RFuture[SellResult]

  /** Fetches info on `thingId` for gifting. */
  def getGiftCardInfo (thingId :Int, created :Long) :RFuture[GiftInfoResult]

  /** Gifts the specified card to the specified friend. */
  def giftCard (thingId :Int, created :Long, friendId :Int, message :String) :RFuture[Unit]

  /** Configures our 'like' setting for `catId`. */
  def setLike (catId :Int, like :JBoolean) :RFuture[Unit]

  /** Opens the specified gift. */
  def openGift (thingId :Int, created :Long) :RFuture[GiftResult]

  /** Fetches info for this player's shop. */
  def getShopInfo () :RFuture[ShopResult]

  /** Buys the specified powerup. */
  def buyPowerup (pup :Powerup) :RFuture[Unit]

  /** Uses the specified powerup on the specified grid. */
  def usePowerup (gridId :Int, pup :Powerup) :RFuture[Grid]
}
