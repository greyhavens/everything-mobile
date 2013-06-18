//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.RFuture

import com.threerings.everything.data._
import com.threerings.everything.rpc.JSON._

class GameServiceClient (game :Everything, url :String) extends GsonService(game, url + "game")
    with GameService {

  def getCollection (ownerId :Int) = request(
    "getCollection", new GetCollection(ownerId), classOf[PlayerCollection])

  def getSeries (ownerId :Int, categoryId :Int) = request(
    "getSeries", new GetSeries(ownerId, categoryId), classOf[Series])

  def getCard (ident :CardIdent) = request("getCard", ident, classOf[Card])

  def getGrid (pup :Powerup, expectHave :Boolean) = request(
    "getGrid", new GetGrid(pup, expectHave), classOf[GridResult])

  def flipCard (gridId :Int, pos :Int, expectCost :Int) = request(
    "flipCard", new FlipCard(gridId, pos, expectCost), classOf[FlipCardResult])

  def sellCard (thingId :Int, created :Long) = request(
    "sellCard", new CardInfo(thingId, created), classOf[SellCardResult])

  def getGiftCardInfo (thingId :Int, created :Long) = request(
    "getGiftCardInfo", new CardInfo(thingId, created), classOf[GiftCardInfo])

  def giftCard (thingId :Int, created :Long, friendId :Int, message :String) = invoke(
    "getGiftCardInfo", new GiftCard(thingId, created, friendId, message))

  def setLike (catId :Int, like :Option[Boolean]) = invoke(
    "setLike", new SetLike(catId, like match {
      case None => null
      case Some(like) => new JBoolean(like)
    }))

  def openGift (thingId :Int, created :Long) = request(
    "openGift", new CardInfo(thingId, created), classOf[GiftInfo])

  def getShopInfo () = request("getShopInfo", classOf[ShopInfo])

  def buyPowerup (pup :Powerup) :RFuture[Unit] = invoke(
    "buyPowerup", new BuyPowerup(pup))

  def usePowerup (gridId :Int, pup :Powerup) = request(
    "usePowerup", new UsePowerup(gridId, pup), classOf[Grid])
}
