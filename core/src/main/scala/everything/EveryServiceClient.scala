//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import com.threerings.everything.data._

/** The client side of an HTTP/JSON implementation of EveryService. */
class EveryServiceClient (game :Everything, url :String) extends GsonService(game, url)
    with EveryService {

  case class ValidateSession (fbId :String, fbToken :String, tzOffset :Int)
  override def validateSession (fbId :String, fbToken :String, tzOffset :Int) = request(
    "validateSession", ValidateSession(fbId, fbToken, tzOffset), classOf[SessionData])

  override def getRecentFeed () = request("getRecentFeed", classOf[FeedResult])

  case class GetUserFeed (userId :Int)
  override def getUserFeed (userId :Int) = request(
    "getUserFeed", GetUserFeed(userId), classOf[Array[FeedItem]])

  override def getFriends () = request("getFriends", classOf[Array[PlayerStats]])

  override def getCredits () = request("getCredits", classOf[CreditsResult])
}
