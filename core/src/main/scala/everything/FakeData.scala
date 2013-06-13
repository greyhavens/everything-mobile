//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import com.threerings.everything.data._
import java.util.Date

object FakeData {

  val mikeB = player("Michael", "Bayne", 1, 1)
  val mikeD = player("Mike", "Daniels", 1067, 1067)

  val mythology = category(1, "Mythology", 0, mikeB)
  val general = category(2, "General", 1, mikeD)
  val psychopomps = category(3, "Psychopomps", 2, mikeD)

  val yanluo = thing(
    id = 1,
    categoryId = psychopomps.categoryId,
    name = "Yanluo",
    rarity = Rarity.III,
    image = "62966384e14f645f94d1c4c595a38eb1112d53c2",
    descrip = "In asian mythology, Yanluo is the god of death and the ruler of Diyu, " +
      "or the underworld.",
    facts = "Yanluo is portrayed as a large man with a scowling red face, " +
      "bulging eyes and a long beard.\n" +
      "Yanluo is considered to be a bureaucratic post, rather than an individual god. " +
      "There were said to be cases in which an honest mortal was rewarded the post of Yanluo. " +
      "He is also tasked with deciding the reincarnation fate of the deceased.",
    source = "http://en.wikipedia.org/wiki/Enma#Yama_in_Chinese.2C_Korean.2C_and_Japanese_mythology",
    creator = mikeD
  )

  val yanluoCard = card(
    mikeB, Array(mythology, general, psychopomps), yanluo, 6, 10, new Date(), mikeD)

  def player (name :String, surname :String, userId :Int, fbId :Long) = {
    val nm = PlayerName.create(userId)
    nm.name = name
    nm.surname = surname
    nm.facebookId = fbId
    nm
  }

  def thing (id :Int, categoryId :Int, name :String, rarity :Rarity, image :String,
             descrip :String, facts :String, source :String, creator :PlayerName) = {
    val thing = new Thing
    thing.thingId = id
    thing.categoryId = categoryId
    thing.name = name
    thing.rarity = rarity
    thing.image = image
    thing.descrip = descrip
    thing.facts = facts
    thing.source = source
    thing.creator = creator
    thing
  }

  def category (id :Int, name :String, parentId :Int, creator :PlayerName) = {
    val cat = new Category()
    cat.categoryId = id
    cat.name = name
    cat.parentId = parentId
    cat.creator = creator
    cat.state = Category.State.ACTIVE
    cat.things = 10
    cat
  }

  def card (owner :PlayerName, cats :Array[Category], thing :Thing, pos :Int, things :Int,
            recvd :Date, giver :PlayerName) = {
    val card = new Card
    card.owner = owner
    card.categories = cats
    card.thing = thing
    card.position = pos
    card.things = things
    card.received = recvd
    card.giver = giver
    card
  }
}
