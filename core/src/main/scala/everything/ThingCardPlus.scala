//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import com.threerings.everything.data._

/** Augments a thing card with a few extra bits, which we often have. */
case class ThingCardPlus (
  thing  :ThingCard,
  path   :Seq[String],
  pos    :Int,
  things :Int,
  owner  :PlayerName,
  giver  :PlayerName
) {
  def this (card :Card) = this(card.toThingCard, card.categories.map(_.name),
                               card.position, card.things, card.owner, card.giver)

  // ThingCard forwarders
  def thingId = thing.thingId
  def categoryId = thing.categoryId
  def name = thing.name
  def image = thing.image
  def rarity = thing.rarity
  def received = thing.received

  // other useful bits
  def categoryName = path(path.length-1)
}
