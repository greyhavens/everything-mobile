//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.lang.{Integer => JInteger}
import java.util.{ArrayList, HashMap}

import react.RFuture

import com.threerings.everything.data._

object MockEveryService extends EveryService with Mockery {

  def validateSession (tzOffset :Int) :RFuture[SessionData] = {
    val data = new SessionData
    data.name = player("Testy", "Testerson", 2, 2)
    data.coins = 42
    data.powerups = new HashMap[Powerup,JInteger]
    data.likes = new ArrayList[JInteger]
    data.dislikes = new ArrayList[Integer]
    data.gridsConsumed = 5
    data.gridExpires = System.currentTimeMillis + 24*60*60*1000L
    data.news = news("No gnus is good gnus.")
    data.everythingURL = "http://apps.facebook.com/everythingcandidate"
    data.backendURL = "https://everything-candidate.herokuapp.com/"
    data.facebookAppId = "107211406428"
    RFuture.success(data)
  }

  def getRecentFeed () :RFuture[FeedResult] = {
    RFuture.success(FeedResult(Seq(), Seq(), Seq()))
  }

  def getUserFeed (userId :Int) :RFuture[Seq[FeedItem]] = {
    RFuture.success(Seq())
  }

  def getFriends () :RFuture[Seq[PlayerStats]] = {
    RFuture.success(Seq(
      playerStats(player("Elvis", "Presley",    3, 3), 25, 6, 3, 15),
      playerStats(player("Ella",  "Fitzgerald", 4, 4), 30, 4, 3, 25),
      playerStats(player("Kurt",  "Kobain",     5, 5), 54, 9, 6,  3),
      playerStats(player("Frank", "Sinatra",    6, 6), 12, 3, 2, 12)))
  }

  def getCredits () :RFuture[CreditsResult] = {
    RFuture.success(CreditsResult(player("Michael", "Bayne", 1, 1),
                                  player("Josh", "Gramse", 2, 2),
                                  Seq(player("Michael", "Bayne", 1, 1),
                                      player("Ray", "Greenwell", 3, 3)),
                                  Seq(player("Natalie", "Bayne", 4, 4),
                                      player("Cody", "Phoenix", 5, 5))))
  }
}