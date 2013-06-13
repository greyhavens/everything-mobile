//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.util.Date
import scala.util.Random

import com.threerings.everything.data._

trait Mockery {

  def player (name :String, surname :String, userId :Int, fbId :Long) = {
    val nm = PlayerName.create(userId)
    nm.name = name
    nm.surname = surname
    nm.facebookId = fbId
    nm
  }

  def news (message :String) = {
    val news = new News
    news.reported = new Date()
    news.reporter = player("Admin", "Adminerston", 1, 1)
    news.text = message
    news
  }

  def playerStats (name :PlayerName, things :Int, series :Int, compSeries :Int, gifts :Int) = {
    val pstats = new PlayerStats
    pstats.name = name
    pstats.things = things
    pstats.series = series
    pstats.completeSeries = compSeries
    pstats.gifts = gifts
    pstats.lastSession = new Date(System.currentTimeMillis - (Random.nextLong % 5*24*60*60*1000L))
    pstats
  }

  def status (coins :Int, freeFlips :Int, nextFlipCost :Int) = {
    val status = new GameStatus
    status.coins = coins
    status.freeFlips = freeFlips
    status.nextFlipCost = nextFlipCost
    status
  }
}
