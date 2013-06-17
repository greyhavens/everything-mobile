//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.RFuture

import com.threerings.everything.data._

/** Provides info on a player's feed. */
case class FeedResult (
  /** Gifts awaiting this player, if any. */
  gifts    :Array[ThingCard],
  /** Comments on this user's series. */
  comments :Array[CategoryComment],
  /** This user's recent feed. */
  items    :Array[FeedItem])

/** Returns info on the game credits. */
case class CreditsResult (
  design  :PlayerName,
  art     :PlayerName,
  code    :Array[PlayerName],
  editors :Array[PlayerName])

/** Interface for general things between the client and server. */
trait EveryService {

  /** Validates that this client has proper session credentials (starting one if needed). */
  def validateSession (fbId :String, fbToken :String, tzOffset :Int) :RFuture[SessionData]

  /** Returns the calling user's pending gifts and data on their friends' activities. */
  def getRecentFeed () :RFuture[FeedResult]

  /** Returns a list of recent activity for the specified user. */
  def getUserFeed (userId :Int) :RFuture[Array[FeedItem]]

  /** Returns stats on the caller's friends (the caller is included in this set). */
  def getFriends () :RFuture[Array[PlayerStats]]

  /** Returns the data needed for the credits page. */
  def getCredits () :RFuture[CreditsResult]
}
