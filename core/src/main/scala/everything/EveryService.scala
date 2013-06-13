//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.RFuture

import com.threerings.everything.data._

/** Interface for general things between the client and server. */
trait EveryService {

  /** Validates that this client has proper session credentials. */
  def validateSession (tzOffset :Int) :RFuture[SessionData]

  /** Returns the calling user's pending gifts and data on their friends' activities. */
  def getRecentFeed () :RFuture[FeedResult]
  case class FeedResult (
    /** Gifts awaiting this player, if any. */
    gifts :Seq[ThingCard],
    /** Comments on this user's series. */
    comments :Seq[CategoryComment],
    /** This user's recent feed. */
    items :Seq[FeedItem])

  /** Returns a list of recent activity for the specified user. */
  def getUserFeed (userId :Int) :RFuture[Seq[FeedItem]]

  /** Returns stats on the caller's friends (the caller is included in this set). */
  def getFriends () :RFuture[Seq[PlayerStats]]

  /** Returns the data needed for the credits page. */
  def getCredits () :RFuture[CreditsResult]
  case class CreditsResult (design :PlayerName, art :PlayerName,
                            code :Seq[PlayerName], editors :Seq[PlayerName])
}
