//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.RFuture

import com.threerings.everything.data._

/** Provides info on a flipped card. */
case class CardResult (
  /** The card in question. */
  card :Card,
  /** Number of this thing already held by this player (not including this one). */
  haveCount :Int,
  /** Number of things remaining in this set not held by this player. */
  thingsRemaining :Int,
  /** Trophies newly earned, or Nil. */
  trophies :Seq[TrophyData])

/** Interface for game related things between the client and server. */
trait GameService {

  /** Fetches the specified player's collection. */
  def getCollection (ownerId :Int) :RFuture[PlayerCollection]

  /** Fetches info on the specified series. */
  def getSeries (ownerId :Int, categoryId :Int) :RFuture[Series]

  /** Fetches info on the specified card. */
  def getCard (ident :CardIdent) :RFuture[Card]

  /** Fetches the player's current grid and game status. */
  def getGrid (pup :Powerup, expectHave :Boolean) :RFuture[(Grid, GameStatus)]

  /** Flips the specified card. Returns the player's new game status.
    * @return (info for the flipped card, the player's new game status) */
  def flipCard (gridId :Int, pos :Int, expectCost :Int) :RFuture[(CardResult, GameStatus)]

  /** Sells the specified card.
    * @return (player's new coin balance, new 'like' value for this category). */
  def sellCard (thingId :Int, created :Long) :RFuture[(Int, Option[Boolean])]

  /** Fetches info on `thingId` for gifting.
    * @return (number of things in `thingId`'s series, friend status of non-card-havers). */
  def getGiftCardInfo (thingId :Int, created :Long) :RFuture[(Int,Seq[FriendCardInfo])]

  /** Gifts the specified card to the specified friend. */
  def giftCard (thingId :Int, created :Long, friendId :Int, message :String) :Unit

  /** Configures our 'like' setting for `catId`. */
  def setLike (catId :Int, like :Option[Boolean]) :Unit

  /** Opens the specified gift.
    * @return (info on the flipped card, any message associated with the gift) */
  def openGift (thingId :Int, created :Long) :RFuture[(CardResult, String)]

  /** Fetches info for this player's shop.
    * @return (player's current coin balance, player's current powerup counts) */
  def getShopInfo () :RFuture[(Int, java.util.Map[Powerup,JInteger])]

  /** Buys the specified powerup. */
  def buyPowerup (pup :Powerup) :RFuture[Unit]

  /** Uses the specified powerup on the specified grid. */
  def usePowerup (gridId :Int, pup :Powerup) :RFuture[Grid]
}
