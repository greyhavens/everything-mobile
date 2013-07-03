//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import com.google.gson.{GsonBuilder, LongSerializationPolicy}
import java.util.{Date, HashMap}
import react.IntValue
import scala.collection.JavaConversions._

import com.threerings.everything.data._
import com.threerings.everything.rpc.GameAPI._

/** A mocked `GameService` which allows us to test interaction locally. */
object MockGameService extends GameService with Mockery {

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

  def getCollection (ownerId :Int) = {
    val json = """{"owner":{"userId":2,"facebookId":"540615819","name":"Michael","surname":"Bayne"},"series":{"Animals":{"Amphibians":[{"categoryId":1341,"name":"North American Frogs II","parentId":13,"things":20,"owned":4},{"categoryId":246,"name":"Photographed Extinct Amphibians","parentId":13,"things":5,"owned":2}],"Birds":[{"categoryId":1699,"name":"Eagles","parentId":9,"things":18,"owned":6},{"categoryId":1703,"name":"Falcons","parentId":9,"things":14,"owned":6},{"categoryId":1702,"name":"Hawks","parentId":9,"things":18,"owned":5},{"categoryId":934,"name":"Hummingbirds","parentId":9,"things":17,"owned":7},{"categoryId":57,"name":"Kingfishers","parentId":9,"things":8,"owned":8},{"categoryId":441,"name":"Owls","parentId":9,"things":14,"owned":9},{"categoryId":97,"name":"Penguins","parentId":9,"things":9,"owned":9},{"categoryId":244,"name":"Photographed Extinct Birds","parentId":9,"things":8,"owned":4},{"categoryId":312,"name":"Pigeons","parentId":9,"things":10,"owned":9},{"categoryId":433,"name":"Raptors","parentId":9,"things":8,"owned":8},{"categoryId":944,"name":"Tits","parentId":9,"things":8,"owned":6},{"categoryId":1494,"name":"Woodpeckers I","parentId":9,"things":18,"owned":10}],"Domestic":[{"categoryId":1608,"name":"AKC Sporting Dogs II","parentId":1623,"things":14,"owned":6},{"categoryId":1584,"name":"Bovine I","parentId":1623,"things":14,"owned":1},{"categoryId":98,"name":"Cats","parentId":1623,"things":12,"owned":10},{"categoryId":1697,"name":"Cats II","parentId":1623,"things":14,"owned":2},{"categoryId":1367,"name":"Equine Ponies","parentId":1623,"things":16,"owned":6},{"categoryId":355,"name":"Herding Dogs","parentId":1623,"things":14,"owned":14},{"categoryId":580,"name":"Hound Dogs","parentId":1623,"things":15,"owned":8},{"categoryId":896,"name":"Retrievers","parentId":1623,"things":6,"owned":4},{"categoryId":712,"name":"Setters","parentId":1623,"things":6,"owned":3},{"categoryId":285,"name":"Terriers","parentId":1623,"things":12,"owned":9}],"Insects":[{"categoryId":128,"name":"Bees","parentId":55,"things":12,"owned":12},{"categoryId":71,"name":"Beetles","parentId":55,"things":12,"owned":10},{"categoryId":56,"name":"Butterflies","parentId":55,"things":12,"owned":10}],"Mammals":[{"categoryId":17,"name":"Bears","parentId":8,"things":8,"owned":8},{"categoryId":694,"name":"Camelids","parentId":8,"things":6,"owned":1},{"categoryId":306,"name":"Dolphins","parentId":8,"things":17,"owned":16},{"categoryId":1495,"name":"Horned Mammals","parentId":8,"things":17,"owned":5},{"categoryId":249,"name":"Marsupials","parentId":8,"things":11,"owned":11},{"categoryId":362,"name":"Pachyderms","parentId":8,"things":9,"owned":9},{"categoryId":289,"name":"Pinnipeds","parentId":8,"things":13,"owned":13},{"categoryId":357,"name":"Primates","parentId":8,"things":14,"owned":13},{"categoryId":235,"name":"Squirrels","parentId":8,"things":10,"owned":10},{"categoryId":1369,"name":"Weasels","parentId":8,"things":16,"owned":4},{"categoryId":181,"name":"Wild Cats","parentId":8,"things":21,"owned":21},{"categoryId":1032,"name":"Wolves","parentId":8,"things":16,"owned":7}],"Marine":[{"categoryId":248,"name":"Seahorses","parentId":12,"things":9,"owned":7},{"categoryId":245,"name":"Sharks","parentId":12,"things":16,"owned":14}],"Misc":[{"categoryId":1616,"name":"Odd Couples","parentId":38,"things":21,"owned":5}],"Reptiles":[{"categoryId":207,"name":"Turtles","parentId":11,"things":10,"owned":10}]},"Art":{"Artists":[{"categoryId":682,"name":"Leonardo da Vinci","parentId":521,"things":10,"owned":3},{"categoryId":1541,"name":"Vincent van Gogh","parentId":521,"things":12,"owned":4}],"Materials":[{"categoryId":440,"name":"Retired Crayola Colors","parentId":439,"things":16,"owned":11}],"Painting":[{"categoryId":301,"name":"Expressionists","parentId":76,"things":11,"owned":9},{"categoryId":237,"name":"Impressionists","parentId":76,"things":9,"owned":8}],"Paper":[{"categoryId":1194,"name":"Origami","parentId":917,"things":10,"owned":4},{"categoryId":1184,"name":"Origami Techniques","parentId":917,"things":14,"owned":6}],"Sculpture":[{"categoryId":498,"name":"Fountains","parentId":77,"things":17,"owned":6}]},"Commerce":{"Products":[{"categoryId":969,"name":"Cigars","parentId":968,"things":10,"owned":4}],"Stocks and Bonds":[{"categoryId":767,"name":"Market Crashes","parentId":766,"things":10,"owned":3}]},"Cuisine":{"American":[{"categoryId":1418,"name":"Carnival Favorites","parentId":860,"things":17,"owned":6},{"categoryId":1262,"name":"Sandwiches II","parentId":860,"things":11,"owned":5},{"categoryId":211,"name":"Seven Layer Dip","parentId":860,"things":7,"owned":7}],"Beverages":[{"categoryId":822,"name":"After Dinner Cocktails","parentId":238,"things":8,"owned":6},{"categoryId":239,"name":"Alcoholic Beverages","parentId":238,"things":15,"owned":14},{"categoryId":810,"name":"Before Dinner Cocktails","parentId":238,"things":11,"owned":6},{"categoryId":342,"name":"Colors of Tea","parentId":238,"things":6,"owned":6},{"categoryId":827,"name":"Fancy Cocktails","parentId":238,"things":10,"owned":2},{"categoryId":972,"name":"Fine Wines","parentId":238,"things":10,"owned":5},{"categoryId":826,"name":"Long Drink Cocktails","parentId":238,"things":12,"owned":6},{"categoryId":1530,"name":"U.S. Soft Drinks","parentId":238,"things":20,"owned":8}],"Bread":[{"categoryId":732,"name":"Varieties of","parentId":731,"things":12,"owned":6}],"Breakfast":[{"categoryId":224,"name":"Cold Cereal","parentId":223,"things":12,"owned":9},{"categoryId":446,"name":"Prepared Eggs","parentId":223,"things":11,"owned":4}],"British":[{"categoryId":661,"name":"UK Chocolates","parentId":1263,"things":15,"owned":3}],"Candy":[{"categoryId":892,"name":"Sweets of Harry Potter","parentId":260,"things":10,"owned":3},{"categoryId":261,"name":"Willy Wonka","parentId":260,"things":10,"owned":8}],"Chinese":[{"categoryId":382,"name":"American Chinese","parentId":381,"things":17,"owned":16}],"Cookies":[{"categoryId":800,"name":"American Favorites","parentId":378,"things":13,"owned":3},{"categoryId":377,"name":"Girl Scout cookies","parentId":378,"things":9,"owned":8},{"categoryId":761,"name":"Keebler","parentId":378,"things":8,"owned":3}],"Dessert":[{"categoryId":296,"name":"Cakes","parentId":295,"things":16,"owned":16},{"categoryId":466,"name":"Pies","parentId":295,"things":12,"owned":4}],"Fast food":[{"categoryId":742,"name":"Burgers","parentId":1865,"things":14,"owned":2}],"Indian":[{"categoryId":762,"name":"Western","parentId":573,"things":7,"owned":3}],"Japanese":[{"categoryId":397,"name":"Nigiri Sushi","parentId":396,"things":14,"owned":13},{"categoryId":906,"name":"Sweets","parentId":396,"things":20,"owned":4}],"Meat":[{"categoryId":434,"name":"Cuts of Beef","parentId":429,"things":8,"owned":4},{"categoryId":432,"name":"Cuts of Chicken","parentId":429,"things":8,"owned":7},{"categoryId":430,"name":"Cuts of Pork","parentId":429,"things":10,"owned":8}],"Misc":[{"categoryId":1324,"name":"Eating Utensils","parentId":1285,"things":10,"owned":2},{"categoryId":1059,"name":"On A Stick","parentId":1285,"things":12,"owned":4}],"Preparation":[{"categoryId":1635,"name":"Cooking Techniques","parentId":1634,"things":15,"owned":5}],"Vegetarian":[{"categoryId":457,"name":"Meat Substitutes","parentId":63,"things":7,"owned":2}]},"Culture":{"Astrology":[{"categoryId":924,"name":"Chinese Zodiac","parentId":363,"things":12,"owned":4},{"categoryId":364,"name":"Zodiac Signs","parentId":363,"things":12,"owned":11}],"Dance":[{"categoryId":344,"name":"20th Century, American","parentId":343,"things":9,"owned":7}],"Gestures":[{"categoryId":346,"name":"Greetings","parentId":345,"things":10,"owned":7}],"Holidays":[{"categoryId":563,"name":"American I","parentId":562,"things":14,"owned":8},{"categoryId":565,"name":"American II","parentId":562,"things":10,"owned":5}],"Occult":[{"categoryId":547,"name":"Tarot - Major Arcana I","parentId":546,"things":11,"owned":7},{"categoryId":552,"name":"Tarot - Major Arcana II","parentId":546,"things":11,"owned":1},{"categoryId":549,"name":"Tarot - Minor Arcana Cups","parentId":546,"things":14,"owned":2},{"categoryId":548,"name":"Tarot - Minor Arcana Swords","parentId":546,"things":14,"owned":5}],"Sociology":[{"categoryId":1414,"name":"Castes","parentId":384,"things":6,"owned":1}],"Symbols":[{"categoryId":1496,"name":"Animals in Celtic Designs","parentId":1233,"things":11,"owned":6},{"categoryId":1690,"name":"Greek Letters I","parentId":1233,"things":18,"owned":2},{"categoryId":1237,"name":"Native Spirit Animals I","parentId":1233,"things":21,"owned":9}]},"Design":{"Architecture":[{"categoryId":974,"name":"Cathedrals","parentId":212,"things":10,"owned":5},{"categoryId":255,"name":"French Châteaux","parentId":212,"things":16,"owned":15},{"categoryId":824,"name":"Opera Houses","parentId":212,"things":10,"owned":4},{"categoryId":405,"name":"Skyscrapers","parentId":212,"things":13,"owned":10},{"categoryId":213,"name":"Unusual Homes","parentId":212,"things":8,"owned":8}],"Fashion":[{"categoryId":1357,"name":"Harajuku","parentId":176,"things":14,"owned":1},{"categoryId":745,"name":"Lingerie","parentId":176,"things":13,"owned":7},{"categoryId":508,"name":"Moustaches","parentId":176,"things":9,"owned":7},{"categoryId":168,"name":"Renaissance Headwear","parentId":176,"things":6,"owned":6},{"categoryId":726,"name":"Sportswear","parentId":176,"things":10,"owned":4}],"Furniture":[{"categoryId":100,"name":"Modern Chairs","parentId":99,"things":9,"owned":9}],"Jewelry":[{"categoryId":1031,"name":"British Crown Jewels","parentId":832,"things":12,"owned":4},{"categoryId":1040,"name":"Famous Diamonds","parentId":832,"things":16,"owned":1}],"Lighting":[{"categoryId":443,"name":"Chandeliers","parentId":442,"things":9,"owned":3},{"categoryId":494,"name":"Neon Signs","parentId":442,"things":14,"owned":6}]},"Entertainment":{"Actors":[{"categoryId":370,"name":"Brat Pack","parentId":376,"things":8,"owned":5},{"categoryId":977,"name":"James Bond","parentId":376,"things":7,"owned":3},{"categoryId":1549,"name":"Marx Brothers","parentId":376,"things":6,"owned":2},{"categoryId":426,"name":"Monty Python","parentId":376,"things":6,"owned":5}],"Anime":[{"categoryId":1493,"name":"Mobile Fighter G Gundam","parentId":1118,"things":15,"owned":5}],"Cartoons":[{"categoryId":166,"name":"1980s Cartoons","parentId":756,"things":12,"owned":12},{"categoryId":1042,"name":"GI Joe: Joes I","parentId":756,"things":12,"owned":4},{"categoryId":276,"name":"Hanna-Barbera","parentId":756,"things":10,"owned":9},{"categoryId":1444,"name":"Hanna-Barbera II","parentId":756,"things":17,"owned":4},{"categoryId":653,"name":"Looney Tunes","parentId":756,"things":16,"owned":8},{"categoryId":693,"name":"Nicktoons I","parentId":756,"things":10,"owned":8},{"categoryId":715,"name":"TMNT","parentId":756,"things":8,"owned":1},{"categoryId":488,"name":"The Simpsons I","parentId":756,"things":14,"owned":10},{"categoryId":486,"name":"The Simpsons II","parentId":756,"things":18,"owned":9},{"categoryId":489,"name":"The Simpsons III","parentId":756,"things":18,"owned":15},{"categoryId":493,"name":"The Simpsons IV","parentId":756,"things":18,"owned":12},{"categoryId":490,"name":"The Simpsons V","parentId":756,"things":18,"owned":3}],"Comics":[{"categoryId":558,"name":"DC Superheroes","parentId":500,"things":12,"owned":4},{"categoryId":567,"name":"Gotham\u0027s Most Wanted","parentId":500,"things":12,"owned":6},{"categoryId":501,"name":"Marvel Superheroes I","parentId":500,"things":12,"owned":8},{"categoryId":698,"name":"Peanuts","parentId":500,"things":13,"owned":7},{"categoryId":794,"name":"X-Men I","parentId":500,"things":13,"owned":2}],"Disney":[{"categoryId":771,"name":"Alice in Wonderland","parentId":663,"things":15,"owned":9},{"categoryId":697,"name":"Disneyworld I","parentId":663,"things":16,"owned":10},{"categoryId":678,"name":"Dogs","parentId":663,"things":17,"owned":4},{"categoryId":1838,"name":"Early Animated Films","parentId":663,"things":16,"owned":3},{"categoryId":688,"name":"Princesses","parentId":663,"things":9,"owned":1},{"categoryId":724,"name":"Sidekicks I","parentId":663,"things":15,"owned":5},{"categoryId":1189,"name":"The Little Mermaid","parentId":663,"things":9,"owned":1},{"categoryId":695,"name":"Villains","parentId":663,"things":19,"owned":10}],"Games":[{"categoryId":93,"name":"Board Games","parentId":152,"things":12,"owned":10},{"categoryId":773,"name":"Chess","parentId":152,"things":7,"owned":2},{"categoryId":1356,"name":"Playing Cards - Hearts","parentId":152,"things":13,"owned":5},{"categoryId":1353,"name":"Playing Cards - Spades","parentId":152,"things":13,"owned":6}],"Humor":[{"categoryId":409,"name":"Famous Clowns","parentId":250,"things":11,"owned":10},{"categoryId":251,"name":"Joke Devices","parentId":250,"things":9,"owned":9}],"Misc":[{"categoryId":419,"name":"Evil Wizards","parentId":221,"things":11,"owned":8}],"Movies":[{"categoryId":626,"name":"Alfred Hitchcock","parentId":129,"things":12,"owned":6},{"categoryId":1453,"name":"Harry Potter Characters","parentId":129,"things":20,"owned":5},{"categoryId":272,"name":"Horror Villains","parentId":129,"things":12,"owned":11},{"categoryId":734,"name":"James Bond I","parentId":129,"things":11,"owned":3},{"categoryId":736,"name":"James Bond II","parentId":129,"things":11,"owned":7},{"categoryId":670,"name":"Labyrinth","parentId":129,"things":11,"owned":3},{"categoryId":1421,"name":"LotR Monsters","parentId":129,"things":13,"owned":6},{"categoryId":383,"name":"Robot Villains","parentId":129,"things":13,"owned":12},{"categoryId":898,"name":"Sam Raimi","parentId":129,"things":13,"owned":2},{"categoryId":273,"name":"Sevens","parentId":129,"things":7,"owned":6},{"categoryId":975,"name":"Stanley Kubrick","parentId":129,"things":13,"owned":10},{"categoryId":1081,"name":"Star Trek","parentId":129,"things":11,"owned":3},{"categoryId":499,"name":"Star Wars Characters I","parentId":129,"things":16,"owned":9}],"Pixar":[{"categoryId":1330,"name":"John Ratzenberger Characters","parentId":863,"things":10,"owned":4},{"categoryId":792,"name":"WALL-E","parentId":863,"things":6,"owned":3}],"Television":[{"categoryId":461,"name":"Daily Show Correspondents","parentId":165,"things":16,"owned":12},{"categoryId":231,"name":"Game Show Hosts","parentId":165,"things":8,"owned":7},{"categoryId":1191,"name":"Glee - Season 1","parentId":165,"things":15,"owned":1},{"categoryId":1743,"name":"I love Lucy","parentId":165,"things":6,"owned":1},{"categoryId":177,"name":"Muppets","parentId":165,"things":13,"owned":12},{"categoryId":274,"name":"Sesame Street","parentId":165,"things":12,"owned":11},{"categoryId":465,"name":"Star Trek Races","parentId":165,"things":17,"owned":8},{"categoryId":479,"name":"Star Trek TOS","parentId":165,"things":11,"owned":6},{"categoryId":482,"name":"Star Trek Voyager","parentId":165,"things":10,"owned":5},{"categoryId":1744,"name":"The Carol Burnett Show","parentId":165,"things":6,"owned":1},{"categoryId":631,"name":"The Office I","parentId":165,"things":14,"owned":6},{"categoryId":755,"name":"The Office II","parentId":165,"things":10,"owned":5}],"Toys":[{"categoryId":510,"name":"Retro Toys","parentId":121,"things":12,"owned":6}],"Trading Cards":[{"categoryId":700,"name":"Garbage Pail Kids","parentId":699,"things":17,"owned":1}],"Video Games":[{"categoryId":154,"name":"1st Gen Consoles","parentId":1423,"things":4,"owned":4},{"categoryId":155,"name":"2nd Gen Consoles","parentId":1423,"things":10,"owned":9},{"categoryId":156,"name":"3rd Gen Consoles","parentId":1423,"things":4,"owned":4},{"categoryId":157,"name":"4th Gen Consoles","parentId":1423,"things":6,"owned":5},{"categoryId":158,"name":"5th Gen Consoles","parentId":1423,"things":13,"owned":12},{"categoryId":159,"name":"6th Gen Consoles","parentId":1423,"things":4,"owned":4},{"categoryId":160,"name":"7th Gen Consoles","parentId":1423,"things":5,"owned":5},{"categoryId":966,"name":"Final Fantasy Series I","parentId":1423,"things":13,"owned":5},{"categoryId":271,"name":"Game Genres","parentId":1423,"things":15,"owned":15},{"categoryId":1501,"name":"Left 4 Dead Series - Infected","parentId":1423,"things":15,"owned":3},{"categoryId":689,"name":"Top Sellers","parentId":1423,"things":10,"owned":7}]},"History":{"Ancient":[{"categoryId":266,"name":"Roman Emperors","parentId":861,"things":10,"owned":9},{"categoryId":184,"name":"Wonders of the World","parentId":861,"things":7,"owned":7}],"Archaeological Sites":[{"categoryId":1760,"name":"Ireland","parentId":1757,"things":7,"owned":2}],"Events":[{"categoryId":253,"name":"Deadliest Natural Disasters","parentId":252,"things":10,"owned":8}],"Technological":[{"categoryId":300,"name":"Inventors","parentId":299,"things":10,"owned":10}],"US Civil War":[{"categoryId":476,"name":"Generals I","parentId":506,"things":13,"owned":10}],"United States":[{"categoryId":1476,"name":"American Founders","parentId":116,"things":20,"owned":1},{"categoryId":1471,"name":"Declaration Signers II","parentId":116,"things":19,"owned":3},{"categoryId":1472,"name":"Declaration Signers III","parentId":116,"things":19,"owned":3},{"categoryId":311,"name":"U.S. Presidents I","parentId":116,"things":11,"owned":11},{"categoryId":315,"name":"U.S. Presidents II","parentId":116,"things":12,"owned":11},{"categoryId":322,"name":"U.S. Presidents III","parentId":116,"things":10,"owned":10},{"categoryId":332,"name":"U.S. Presidents IV","parentId":116,"things":10,"owned":10}],"Wild West":[{"categoryId":1437,"name":"Gunslingers","parentId":1701,"things":6,"owned":2},{"categoryId":1435,"name":"Lawmen","parentId":1701,"things":16,"owned":4},{"categoryId":1439,"name":"Notable Native Americans","parentId":1701,"things":11,"owned":1}],"World War Two":[{"categoryId":1172,"name":"Axis Powers","parentId":582,"things":11,"owned":2}]},"Literature":{"Characters":[{"categoryId":1299,"name":"Fictional Detectives","parentId":555,"things":14,"owned":7},{"categoryId":556,"name":"Mad Scientists","parentId":555,"things":11,"owned":6}],"Children\u0027s Literature":[{"categoryId":823,"name":"Beatrix Potter II","parentId":690,"things":13,"owned":1},{"categoryId":711,"name":"Dr. Seuss","parentId":690,"things":13,"owned":6},{"categoryId":753,"name":"Shel Silverstein","parentId":690,"things":12,"owned":2}],"Novels":[{"categoryId":979,"name":"Harry Potter","parentId":286,"things":11,"owned":4},{"categoryId":855,"name":"Jane Austen","parentId":286,"things":7,"owned":3}],"Plays":[{"categoryId":412,"name":"Playwrights","parentId":411,"things":15,"owned":11},{"categoryId":415,"name":"Shakespeare","parentId":411,"things":15,"owned":10}]},"Mathematics":{"Geometry":[{"categoryId":505,"name":"Fractals","parentId":46,"things":12,"owned":9},{"categoryId":47,"name":"Platonic Solids","parentId":46,"things":5,"owned":5}]},"Music":{"Albums":[{"categoryId":437,"name":"\"Weird Al\" Yankovic","parentId":233,"things":12,"owned":9},{"categoryId":234,"name":"Beatles","parentId":233,"things":12,"owned":12},{"categoryId":487,"name":"Pink Floyd","parentId":233,"things":14,"owned":7},{"categoryId":843,"name":"The Rolling Stones I","parentId":233,"things":12,"owned":1}],"Bands":[{"categoryId":1617,"name":"British Invasion I","parentId":144,"things":12,"owned":2},{"categoryId":1049,"name":"Fictional Bands","parentId":144,"things":20,"owned":3},{"categoryId":146,"name":"Girl Groups","parentId":144,"things":10,"owned":6},{"categoryId":540,"name":"Wu-Tang Clan","parentId":144,"things":10,"owned":7}],"Classical":[{"categoryId":416,"name":"Sopranos","parentId":413,"things":10,"owned":8},{"categoryId":414,"name":"Tenors","parentId":413,"things":11,"owned":8}],"Instruments":[{"categoryId":85,"name":"Electronic","parentId":78,"things":8,"owned":8}],"Musicians":[{"categoryId":620,"name":"Bankrupt","parentId":619,"things":12,"owned":6}],"Opera":[{"categoryId":760,"name":"Italian","parentId":759,"things":13,"owned":3}],"Theatre":[{"categoryId":739,"name":"Musicals I","parentId":737,"things":15,"owned":2}]},"Mythology":{"Ancient Greece":[{"categoryId":542,"name":"Labors of Hercules","parentId":365,"things":12,"owned":5}],"Celtic":[{"categoryId":1388,"name":"Deities","parentId":1407,"things":14,"owned":1},{"categoryId":1467,"name":"Deities II","parentId":1407,"things":14,"owned":2}],"Folklore":[{"categoryId":204,"name":"Werewolf Variants","parentId":203,"things":9,"owned":7}],"General":[{"categoryId":584,"name":"Psychopomps","parentId":544,"things":10,"owned":6}],"Monsters":[{"categoryId":417,"name":"Dragons","parentId":131,"things":10,"owned":7},{"categoryId":147,"name":"Greek","parentId":131,"things":12,"owned":12}]},"People":{"Celebrities":[{"categoryId":1631,"name":"Film Legends I","parentId":86,"things":16,"owned":9},{"categoryId":1632,"name":"Film Legends II","parentId":86,"things":16,"owned":7},{"categoryId":1633,"name":"Film Legends III","parentId":86,"things":18,"owned":9}],"Criminals":[{"categoryId":456,"name":"Assassins","parentId":89,"things":14,"owned":5},{"categoryId":90,"name":"Bank Robbers","parentId":89,"things":7,"owned":6},{"categoryId":379,"name":"Serial Killers","parentId":89,"things":12,"owned":11}],"Notable Women":[{"categoryId":1266,"name":"Old West Outlaws","parentId":1265,"things":10,"owned":3}],"Scottish Clans":[{"categoryId":1275,"name":"Clans IV","parentId":1227,"things":20,"owned":4},{"categoryId":1277,"name":"Clans VI","parentId":1227,"things":20,"owned":1}],"US Native Tribes":[{"categoryId":1221,"name":"Northeast Tribes","parentId":1220,"things":8,"owned":1},{"categoryId":1223,"name":"Plains Tribes","parentId":1220,"things":11,"owned":4},{"categoryId":1222,"name":"Southeast Tribes","parentId":1220,"things":7,"owned":3},{"categoryId":1250,"name":"Western Tribes","parentId":1220,"things":9,"owned":2}]},"Places":{"Canada":[{"categoryId":740,"name":"Provinces","parentId":866,"things":10,"owned":4}],"Mexico":[{"categoryId":927,"name":"Chichén Itzá","parentId":926,"things":20,"owned":4}],"Notable":[{"categoryId":804,"name":"Famous Beaches","parentId":868,"things":11,"owned":2},{"categoryId":831,"name":"World Heritage Sites I","parentId":868,"things":10,"owned":1},{"categoryId":1029,"name":"World Landmarks","parentId":868,"things":20,"owned":9}],"United Kingdom":[{"categoryId":1399,"name":"National Parks","parentId":1401,"things":15,"owned":3}],"United States":[{"categoryId":830,"name":"National Parks","parentId":864,"things":10,"owned":4},{"categoryId":350,"name":"States I","parentId":864,"things":13,"owned":12},{"categoryId":351,"name":"States II","parentId":864,"things":13,"owned":11},{"categoryId":352,"name":"States III","parentId":864,"things":12,"owned":10},{"categoryId":353,"name":"States IV","parentId":864,"things":12,"owned":9},{"categoryId":1626,"name":"The Rotunda II","parentId":864,"things":20,"owned":4}]},"Plants":{"Flowers":[{"categoryId":307,"name":"Orchids","parentId":16,"things":14,"owned":12},{"categoryId":1748,"name":"Wildflowers II","parentId":16,"things":17,"owned":4}],"Fruit":[{"categoryId":389,"name":"Melons","parentId":304,"things":13,"owned":11},{"categoryId":1183,"name":"White Wine Grapes","parentId":304,"things":15,"owned":3}],"Herbs":[{"categoryId":526,"name":"Herbs and Spices I","parentId":15,"things":15,"owned":5},{"categoryId":527,"name":"Herbs and Spices II","parentId":15,"things":15,"owned":6},{"categoryId":1516,"name":"Medicinal Herbs I","parentId":15,"things":20,"owned":8},{"categoryId":1523,"name":"Medicinal Herbs II","parentId":15,"things":20,"owned":8},{"categoryId":217,"name":"Poisonous Berries","parentId":15,"things":8,"owned":6}],"Predatory":[{"categoryId":451,"name":"Carnivorous","parentId":450,"things":11,"owned":9}],"Seeds":[{"categoryId":151,"name":"Nuts","parentId":394,"things":11,"owned":10}]},"Politics":{"Government":[{"categoryId":407,"name":"Forms of","parentId":410,"things":13,"owned":10}],"Monarchs":[{"categoryId":1597,"name":"Scottish I","parentId":1123,"things":17,"owned":6}],"World Leaders":[{"categoryId":452,"name":"English Monarchs","parentId":215,"things":7,"owned":3}]},"Religion":{"Buddhism":[{"categoryId":199,"name":"Lucky Gods","parentId":163,"things":7,"owned":7},{"categoryId":164,"name":"Ten Bulls","parentId":163,"things":10,"owned":9}],"Catholicism":[{"categoryId":25,"name":"Saints","parentId":24,"things":15,"owned":14}],"Christianity":[{"categoryId":114,"name":"Deadly Sins","parentId":112,"things":7,"owned":7},{"categoryId":660,"name":"Virtues","parentId":112,"things":7,"owned":2}],"Hinduism":[{"categoryId":188,"name":"Chakras","parentId":22,"things":7,"owned":7},{"categoryId":37,"name":"Deities","parentId":22,"things":12,"owned":10},{"categoryId":368,"name":"Planetary Deities","parentId":22,"things":9,"owned":8}],"Judaism":[{"categoryId":310,"name":"Archangels","parentId":309,"things":7,"owned":7},{"categoryId":189,"name":"Shepherds of Israel","parentId":309,"things":7,"owned":7}],"Norse":[{"categoryId":369,"name":"Gods","parentId":103,"things":16,"owned":16}],"Other":[{"categoryId":402,"name":"Joke","parentId":401,"things":8,"owned":7}]},"Science":{"Anatomy":[{"categoryId":1485,"name":"Bodily Disturbances","parentId":18,"things":11,"owned":3},{"categoryId":1486,"name":"Bodily Fluids","parentId":18,"things":11,"owned":2},{"categoryId":325,"name":"Fingerprint Patterns","parentId":18,"things":8,"owned":8},{"categoryId":19,"name":"Human Organs","parentId":18,"things":10,"owned":10}],"Astronomy":[{"categoryId":621,"name":"Common Constellations","parentId":34,"things":12,"owned":5},{"categoryId":130,"name":"Dwarf Planet Satellites","parentId":34,"things":6,"owned":6},{"categoryId":44,"name":"Dwarf Planets","parentId":34,"things":5,"owned":5},{"categoryId":380,"name":"Former Stars","parentId":34,"things":6,"owned":6},{"categoryId":35,"name":"Planets","parentId":34,"things":8,"owned":8},{"categoryId":623,"name":"Zodiac Constellations","parentId":34,"things":13,"owned":7}],"Chemistry":[{"categoryId":334,"name":"Actinoids","parentId":316,"things":15,"owned":13},{"categoryId":318,"name":"Alkali metals","parentId":316,"things":6,"owned":6},{"categoryId":319,"name":"Alkaline earth metals","parentId":316,"things":6,"owned":5},{"categoryId":336,"name":"Halogens","parentId":316,"things":5,"owned":4},{"categoryId":333,"name":"Lanthanoids","parentId":316,"things":15,"owned":5},{"categoryId":323,"name":"Metalloids","parentId":316,"things":7,"owned":7},{"categoryId":337,"name":"Noble gases","parentId":316,"things":6,"owned":6},{"categoryId":324,"name":"Other metals","parentId":316,"things":7,"owned":6},{"categoryId":317,"name":"Other nonmetals","parentId":316,"things":7,"owned":5},{"categoryId":335,"name":"Transition IV","parentId":316,"things":10,"owned":6}],"Geography":[{"categoryId":74,"name":"Continents","parentId":95,"things":7,"owned":7},{"categoryId":209,"name":"Great Lakes","parentId":95,"things":5,"owned":4},{"categoryId":226,"name":"Hawaiian Islands","parentId":95,"things":8,"owned":8},{"categoryId":270,"name":"Mountain Ranges","parentId":95,"things":12,"owned":11},{"categoryId":420,"name":"South American Countries","parentId":95,"things":13,"owned":10},{"categoryId":713,"name":"The Balkans","parentId":95,"things":8,"owned":3},{"categoryId":331,"name":"Waterfalls","parentId":95,"things":10,"owned":10}],"Geology":[{"categoryId":49,"name":"Epochs","parentId":48,"things":7,"owned":6}],"Medicine":[{"categoryId":448,"name":"Patent Medicines","parentId":125,"things":11,"owned":6}],"Meteorology":[{"categoryId":180,"name":"Clouds","parentId":179,"things":11,"owned":10},{"categoryId":240,"name":"Natural Disasters","parentId":179,"things":10,"owned":9},{"categoryId":232,"name":"Seasons","parentId":179,"things":4,"owned":4}],"Pharmacology":[{"categoryId":367,"name":"B Vitamins","parentId":259,"things":8,"owned":7},{"categoryId":222,"name":"Recreational Drugs","parentId":259,"things":10,"owned":10}],"Physics":[{"categoryId":308,"name":"Simple Machines","parentId":96,"things":6,"owned":6},{"categoryId":220,"name":"Visible Spectrum","parentId":96,"things":7,"owned":6}],"Psychology":[{"categoryId":962,"name":"Love styles","parentId":53,"things":6,"owned":4},{"categoryId":447,"name":"Myers-Briggs","parentId":53,"things":16,"owned":7},{"categoryId":320,"name":"Rorschach Inkblots","parentId":53,"things":10,"owned":7},{"categoryId":444,"name":"Specific Phobias","parentId":53,"things":16,"owned":7}]},"Sports":{"Events":[{"categoryId":816,"name":"Famous Events","parentId":815,"things":10,"owned":5}],"Gymnastics":[{"categoryId":436,"name":"Apparatus","parentId":435,"things":8,"owned":7}],"Martial Arts":[{"categoryId":1568,"name":"Forms I","parentId":256,"things":10,"owned":3},{"categoryId":1581,"name":"Forms II","parentId":256,"things":12,"owned":4},{"categoryId":1570,"name":"Weapons","parentId":256,"things":10,"owned":1}],"Olympics":[{"categoryId":1716,"name":"Summer Games II","parentId":122,"things":15,"owned":1},{"categoryId":1717,"name":"Summer Sports I","parentId":122,"things":16,"owned":5},{"categoryId":1718,"name":"Summer Sports II","parentId":122,"things":16,"owned":3},{"categoryId":515,"name":"Winter Games I","parentId":122,"things":10,"owned":2}],"Track and Field":[{"categoryId":360,"name":"Decathlon","parentId":359,"things":10,"owned":7}]},"Structures":{"Barriers":[{"categoryId":507,"name":"Dams","parentId":268,"things":13,"owned":9},{"categoryId":269,"name":"Walls","parentId":268,"things":9,"owned":8}],"Bridges":[{"categoryId":33,"name":"Arch","parentId":32,"things":14,"owned":12}]},"Technology":{"Computers":[{"categoryId":50,"name":"8-bit","parentId":30,"things":8,"owned":8},{"categoryId":247,"name":"Input Devices","parentId":30,"things":11,"owned":11},{"categoryId":31,"name":"Programming Languages","parentId":30,"things":17,"owned":16},{"categoryId":464,"name":"iPods","parentId":30,"things":12,"owned":9}],"Time":[{"categoryId":170,"name":"Clocks","parentId":862,"things":13,"owned":12},{"categoryId":205,"name":"Days of the Week","parentId":862,"things":7,"owned":5}],"Transportation":[{"categoryId":422,"name":"60s Muscle Cars","parentId":101,"things":11,"owned":8},{"categoryId":1511,"name":"Concept Cars","parentId":101,"things":10,"owned":4},{"categoryId":455,"name":"Dirigibles","parentId":101,"things":8,"owned":4},{"categoryId":930,"name":"Iconic Cars","parentId":101,"things":10,"owned":3},{"categoryId":102,"name":"Miscellaneous","parentId":101,"things":7,"owned":6},{"categoryId":174,"name":"Space Shuttles","parentId":101,"things":6,"owned":5},{"categoryId":491,"name":"Steam Locomotive Parts","parentId":101,"things":11,"owned":4},{"categoryId":178,"name":"Subways","parentId":101,"things":9,"owned":8}],"Writing":[{"categoryId":459,"name":"Typewriters","parentId":262,"things":11,"owned":7},{"categoryId":263,"name":"Utensils","parentId":262,"things":9,"owned":9}]}},"trophies":[{"trophyId":"sets1","name":"Completed 1","description":"Complete 1 sets of any kind"},{"trophyId":"sets3","name":"Completed 3","description":"Complete 3 sets of any kind"},{"trophyId":"sets5","name":"Completed 5","description":"Complete 5 sets of any kind"},{"trophyId":"sets10","name":"Completed 10","description":"Complete 10 sets of any kind"},{"trophyId":"sets15","name":"Completed 15","description":"Complete 15 sets of any kind"},{"trophyId":"sets20","name":"Completed 20","description":"Complete 20 sets of any kind"},{"trophyId":"sets30","name":"Completed 30","description":"Complete 30 sets of any kind"},{"trophyId":"sets40","name":"Completed 40","description":"Complete 40 sets of any kind"},{"trophyId":"sets50","name":"Completed 50","description":"Complete 50 sets of any kind"},{"trophyId":"mammals3","name":"Mammals I","description":"Collect 3 sets of mammals"},{"trophyId":"mammals5","name":"Mammals II","description":"Collect 5 sets of mammals"},{"trophyId":"birds3","name":"Birds I","description":"Collect 3 sets of birds"},{"trophyId":"chemistry3","name":"Chemistry I","description":"Collect 3 sets of chemicals"}]}"""
    success(_gson.fromJson(json, classOf[PlayerCollection]))
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
