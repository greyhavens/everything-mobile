//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.text.DateFormat
import java.util.{Date, TimeZone}
import playn.core.{ImmediateLayer, PlayN, Surface}
import playn.core.util.Callback
import playn.java.JavaPlatform
import react.RFuture

object EverythingJava {

  case class FauxDevice (width :Int, height :Int, scale :Float, statusBar :Float) {
    def apply (config :JavaPlatform.Config) {
      config.width  = (width/scale).toInt
      config.height = (height/scale).toInt
      config.scaleFactor = scale
    }
  }

  var game :Everything = _

  def main (args :Array[String]) {
    val fbId = if (true) "1008138021" /*testy*/ else "540615819" /*mdb*/

    val config = new JavaPlatform.Config
    config.storageFileName = "playn" + fbId

    val fdev = args.collect(Map(
      "ipad"  -> FauxDevice(768, 1024, 2.0f, 10),
      "i5"    -> FauxDevice(640, 1136, 2.0f, 20),
      "droid" -> FauxDevice(480,  800, 1.5f, 20/1.5f),
      "n7"    -> FauxDevice(800, 1205, 2.5f, 20/2.5f)
    )).headOption.getOrElse(FauxDevice(640, 960, 2, 20))
    fdev.apply(config)

    val pf = JavaPlatform.register(config)
    pf.assets.setAssetScale(2)
    pf.graphics.registerFont("CopperplateGothic-Bold", "fonts/copper.ttf")
    pf.graphics.registerFont("Treasure Map Deadhand", "fonts/treasure.ttf")
    pf.graphics.registerFont("Josschrift", "fonts/josschrift.ttf")

    val facebook = new Facebook {
      def isAuthed = true
      def authenticate () = RFuture.success("test:" + fbId)
      def showDialog (action :String, params :Array[String]) = RFuture.success[String](null)
    }
    val device = new Device {
      def statusBarHeight = fdev.statusBar
      def timeZoneOffset = {
        val tz = TimeZone.getDefault
        // Java returns millis to add to GMT, we want minutes to subtract from GMT
        -tz.getOffset(System.currentTimeMillis)/MillisPerMinute
      }

      def formatDate (when :Long) = _dfmt.format(new Date(when))
      private val _dfmt = DateFormat.getDateInstance()

      def getProducts = RFuture.success(Seq(
        Product( "coins_5000", "$0.99"),
        Product("coins_11000", "$1.99"),
        Product("coins_24000", "$3.99")))

      def buyProduct (sku :String) = sku match {
        case "coins_11000" =>
          RFuture.failure(new Exception("e.test_device_fail"))
        case _ =>
          pf.invokeLater(new Runnable() {
            val curmin = System.currentTimeMillis / MillisPerMinute
            def run = game.redeemPurchase(sku, "TEST", "test_tok:" + curmin, "test_rcpt:" + sku)
          })
          RFuture.success(())
      }
    }

    // put a fake status bar atop the screen
    pf.graphics.rootLayer.add(pf.graphics.createImmediateLayer(new ImmediateLayer.Renderer() {
      def render (surf :Surface) {
        surf.setFillColor(0x88000000).fillRect(0, 0, pf.graphics.width, device.statusBarHeight)
      }
    }).setDepth(Short.MaxValue))

    game = new Everything(args.contains("mock"), device, facebook)
    PlayN.run(game)
  }

  private final val MillisPerMinute = 1000*60
}
