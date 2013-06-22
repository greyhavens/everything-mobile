//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.RFuture

import com.threerings.everything.data._
import com.threerings.everything.rpc.EveryAPI._

/** Interface for general things between the client and server. */
trait EveryService {

  /** Validates that this client has proper session credentials (starting one if needed). */
  def validateSession (fbToken :String, tzOffset :Int) :RFuture[SessionData]

  /** Returns data on the calling user's friends' activities. */
  def getRecentFeed () :RFuture[Array[FeedItem]]

  /** Returns a list of recent activity for the specified user. */
  def getUserFeed (userId :Int) :RFuture[Array[FeedItem]]

  /** Returns stats on the caller's friends (the caller is included in this set). */
  def getFriends () :RFuture[Array[PlayerStats]]

  /** Returns the data needed for the credits page. */
  def getCredits () :RFuture[CreditsResult]
}
