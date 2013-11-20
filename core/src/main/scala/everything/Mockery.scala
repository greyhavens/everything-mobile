//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.util.{ArrayList, Date, HashMap}
import scala.collection.JavaConversions._
import scala.util.Random

import com.google.gson.{GsonBuilder, LongSerializationPolicy}
import react.{IntValue, RFuture}

import com.threerings.everything.data._
import com.threerings.everything.rpc.EveryAPI._
import com.threerings.everything.rpc.GameAPI._
import com.threerings.everything.rpc.JSON._

object Mockery extends EveryService with GameService {

  // helper methods for mocking things
  val elvis = player("Elvis", "Presley",    3, 3)
  val ella  = player("Ella",  "Fitzgerald", 4, 4)
  val kurt  = player("Kurt",  "Kobain",     5, 5)
  val frank = player("Frank", "Sinatra",    6, 6)

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

  def success[T] (result :T) :RFuture[T] = {
    val p = new DeferredPromise[T]()
    p.onSuccess(result)
    p
  }

  def failure[T] (msg :String) :RFuture[T] = {
    val p = new DeferredPromise[T]()
    p.onFailure(new Throwable(msg))
    p
  }

  // the player's "state"
  val coins = new IntValue(4239)
  val freeFlips = new IntValue(2)
  val pups = {
    val map = new HashMap[Powerup,JInteger]
    map.put(Powerup.SHOW_CATEGORY, 1)
    map.put(Powerup.SHOW_SUBCATEGORY, 3)
    map.put(Powerup.SHOW_SERIES, 1)
    map
  }

  val grid = {
    val grid = new Grid
    grid.gridId = 1
    grid.status = GridStatus.NORMAL
    grid.slots = Array.fill(Grid.SIZE)(SlotStatus.UNFLIPPED)
    grid.flipped = Array.fill(Grid.SIZE)(null :ThingCard)
    grid.unflipped = Array(8, 0, 8, 0, 0, 0, 0, 0, 0, 0)
    grid.expires = new Date(System.currentTimeMillis+24*60*60*1000L)
    grid
  }

  val start = System.currentTimeMillis
  val cards = Array.tabulate(Grid.SIZE) { pos =>
    if (pos % 2 == 0) FakeData.yanluoCard(start+pos) else FakeData.maltesersCard(start+pos)
  }

  // EveryService methods
  def validateSession (fbToken :String, tzOffset :Int) = {
    val data = new SessionData
    data.name = player("Testy", "Testerson", 2, 2)
    data.coins = coins.get
    data.powerups = pups
    data.likes = new ArrayList[JInteger]
    data.dislikes = new ArrayList[Integer]
    data.gridsConsumed = 5
    data.gridExpires = System.currentTimeMillis + 24*60*60*1000L
    data.news = news("No gnus is good gnus.")
    data.everythingURL = "http://apps.facebook.com/everythingcandidate"
    data.backendURL = "https://everything-candidate.herokuapp.com/"
    data.facebookAppId = "107211406428"
    data.gifts = List()
    data.notices = Seq(new Notice(Notice.Kind.FRIEND_JOINED, "Testy Testerson", 500),
                       new Notice(Notice.Kind.FRIEND_JOINED, "Dolly Pardon\tMahatma Ghandi", 1000),
                       new Notice(Notice.Kind.PLAYED_MOBILE, null, 2000))
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

  def redeemPurchase (sku :String, platform :String, tok :String, rcpt :String) = sku match {
    case "coins_24000" =>
      failure("e.server_fail")
    case "coins_5000" =>
      coins.increment(42)
      success(coins.get)
    case _ =>
      failure("e.unknown_sku")
  }

  // GameService methods
  def getCollection (ownerId :Int) = {
    success(_gson.fromJson(MockData.collection, classOf[PlayerCollection]))
  }

  def getSeries (ownerId :Int, categoryId :Int) = {
    val s = new Series
    s.categoryId = categoryId
    s.name = "Mock Series"
    s.creator = elvis
    val now = System.currentTimeMillis
    s.things = Array(null, FakeData.yanluo.toCard(now), null,
                     FakeData.maltesers.toCard(now), null, null,
                     null, null, null, null, null, null, null, null, null, null, null, null)
    success(s)
  }

  def getCard (ident :CardIdent) = {
    success(FakeData.yanluoCard(ident.received))
  }

  def getGrid (pup :Powerup, expectHave :Boolean) = success({
    val r = new GridResult
    r.grid = grid
    r.status = status(grid.unflipped)
    r
  })

  def flipCard (gridId :Int, pos :Int, expectCost :Int) = {
    val card = cards(pos)
    def cardres = {
      val r = new FlipResult
      r.card = card
      r.haveCount = 1
      r.thingsRemaining = 10
      r.trophies = List()
      r.status = status(grid.unflipped)
      r
    }
    grid.slots(pos) match {
      case SlotStatus.UNFLIPPED =>
        val cost = nextFlipCost(grid.unflipped)
        // charge 'em
        if (freeFlips.get == 0 && coins.get < cost) failure("e.nsf_for_flip")
        else {
          if (freeFlips.get > 0) freeFlips.decrementClamp(1, 0)
          else coins.decrementClamp(cost, 0)
          grid.slots(pos) = SlotStatus.FLIPPED
          grid.unflipped(card.thing.rarity.ordinal) -= 1
          grid.flipped(pos) = card.toThingCard
          success(cardres)
        }

      case SlotStatus.FLIPPED =>
        success(cardres)

      case _ => failure(s"Card gone: ${grid.slots(pos)}")
    }
  }

  def sellCard (thingId :Int, created :Long) = {
    val revenue = (if (thingId == 1) Rarity.III else Rarity.I).saleValue
    val pos = cards.indexWhere(c => c.thing.thingId == thingId && c.received == created)
    grid.slots(pos) = SlotStatus.SOLD
    success({
      val r = new SellResult
      r.coins = coins.increment(revenue)
      r.newLike = null
      r
    })
  }

  def getGiftCardInfo (thingId :Int, created :Long) = {
    val pos = cards.indexWhere(c => c.thing.thingId == thingId && c.received == created)
    grid.slots(pos) = SlotStatus.GIFTED
    val result = new GiftInfoResult
    result.things = 10
    def friend (name :PlayerName, has :Int) = {
      val f = new FriendCardInfo()
      f.friend = name
      f.hasThings = has
      f
    }
    result.friends = List(friend(frank, 5), friend(kurt, 3), friend(ella, 7))
    success(result)
  }

  def giftCard (thingId :Int, created :Long, friendId :Int, message :String) = {
    success(())
  }

  def setLike (catId :Int, like :JBoolean) = {
    failure("TODO")
  }

  def setWant (catId :Int, want :Boolean) = {
    failure("TODO")
  }

  def openGift (thingId :Int, created :Long) = {
    failure("TODO")
  }

  def getShopInfo () = success({
    val r = new ShopResult
    r.coins = coins.get
    r.powerups = pups
    r
  })

  def buyPowerup (pup :Powerup) = {
    failure("TODO")
  }

  def usePowerup (gridId :Int, pup :Powerup) = {
    def unbox (count :JInteger) = if (count == null) 0 else count.intValue
    unbox(pups.get(pup)) match {
      case 0 => failure("Lack powerup")
      case n =>
        pups.put(pup, n-1)
        pup match {
          case Powerup.SHOW_CATEGORY => reveal(grid, 0)
          case Powerup.SHOW_SUBCATEGORY => reveal(grid, 1)
          case Powerup.SHOW_SERIES => reveal(grid, 2)
          case _ => println(s"Asked to use weird powerup? $pup")
        }
        success(grid)
    }
  }

  protected def reveal (grid :Grid, depth :Int) {
    cards.zipWithIndex.foreach { case (c, idx) =>
      if (grid.flipped(idx) == null || grid.flipped(idx).thingId == 0) {
        val tc = new ThingCard
        tc.name = c.categories(depth).name
        grid.flipped(idx) = tc
      }
    }
  }

  protected def status (unflipped :Array[Int]) :GameStatus = {
    status(coins.get, freeFlips.get, nextFlipCost(unflipped))
  }

  protected def nextFlipCost (unflipped :Array[Int]) = {
    // the cost of a flip is the expected value of the card
    var (total, count) = (0, 0)
    for (rarity <- Rarity.values()) {
      val cards = unflipped(rarity.ordinal)
      total += cards * rarity.value
      count += cards
    }
    if (count == 0) 0 else  (total / count)
  }

  protected val _gson = new GsonBuilder().
    setLongSerializationPolicy(LongSerializationPolicy.STRING).
    create()
}
