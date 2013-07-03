//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.util.{ArrayList, HashMap}
import scala.collection.JavaConversions._

import com.threerings.everything.data._
import com.threerings.everything.rpc.EveryAPI._

object MockEveryService extends EveryService with Mockery {

  def validateSession (fbToken :String, tzOffset :Int) = {
    val data = new SessionData
    data.name = player("Testy", "Testerson", 2, 2)
    data.coins = 42
    data.powerups = MockGameService.pups
    data.likes = new ArrayList[JInteger]
    data.dislikes = new ArrayList[Integer]
    data.gridsConsumed = 5
    data.gridExpires = System.currentTimeMillis + 24*60*60*1000L
    data.news = news("No gnus is good gnus.")
    data.everythingURL = "http://apps.facebook.com/everythingcandidate"
    data.backendURL = "https://everything-candidate.herokuapp.com/"
    data.facebookAppId = "107211406428"
    data.gifts = List()
    success(data)
  }

  def getRecentFeed () = {
    success(Array())
  }

  def getUserFeed (userId :Int) = {
    success(Array())
  }

  def getFriends () = success(Array(
    playerStats(elvis, 25, 6, 3, 15),
    playerStats(ella,  30, 4, 3, 25),
    playerStats(kurt,  54, 9, 6,  3),
    playerStats(frank, 12, 3, 2, 12)))

  def getCredits () = success({
    val res = new CreditsResult
    res.design =player("Michael", "Bayne", 1, 1)
    res.art = player("Josh", "Gramse", 2, 2)
    res.code = List(player("Michael", "Bayne", 1, 1), player("Ray", "Greenwell", 3, 3))
    res.editors = List(player("Natalie", "Bayne", 4, 4), player("Cody", "Phoenix", 5, 5))
    res
  })
}
