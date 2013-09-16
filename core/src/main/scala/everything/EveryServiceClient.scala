//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import com.threerings.everything.data._
import com.threerings.everything.rpc.EveryAPI._
import com.threerings.everything.rpc.JSON._

/** The client side of an HTTP/JSON implementation of EveryService. */
class EveryServiceClient (game :Everything, url :String)
    extends GsonService(game, url + "everything") with EveryService {

  override def validateSession (fbToken :String, tzOffset :Int) = request(
    "validateSession", new ValidateSession(fbToken, tzOffset), classOf[SessionData])

  override def getRecentFeed () = request("getRecentFeed", classOf[Array[FeedItem]])

  override def getUserFeed (userId :Int) = request(
    "getUserFeed", new GetUserFeed(userId), classOf[Array[FeedItem]])

  override def getFriends () = request("getFriends", classOf[Array[PlayerStats]])

  override def getCredits () = request("getCredits", classOf[CreditsResult])

  override def redeemPurchase (sku :String, platform :String, tok :String, sig :String) = request(
    "redeemPurchase", new RedeemPurchase(sku, platform, tok, sig), classOf[JInteger])
}
