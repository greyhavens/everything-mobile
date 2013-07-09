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

  def main (args :Array[String]) {
    val fbId = if (true) "1008138021" /*testy*/ else "540615819" /*mdb*/

    val config = new JavaPlatform.Config
    config.width = 320
    config.height = 480
    config.scaleFactor = 2
    config.storageFileName = "playn" + fbId

    val pf = JavaPlatform.register(config)
    pf.graphics.registerFont("CopperplateGothic-Bold", "fonts/copper.ttf")
    pf.graphics.registerFont("Treasure Map Deadhand", "fonts/treasure.ttf")
    pf.graphics.registerFont("Josschrift", "fonts/josschrift.ttf")

    val facebook = new Facebook {
      def isAuthed = true
      def authenticate () = RFuture.success("test:" + fbId)
      def showDialog (action :String, params :Array[String]) = RFuture.success[String](null)
    }
    val device = new Device {
      def statusBarHeight = 20
      def timeZoneOffset = {
        val tz = TimeZone.getDefault
        // Java returns millis to add to GMT, we want minutes to subtract from GMT
        -tz.getOffset(System.currentTimeMillis)/MillisPerMinute
      }
      def formatDate (when :Long) = _dfmt.format(new Date(when))
      private val _dfmt = DateFormat.getDateInstance()
    }

    // put a fake status bar atop the screen
    pf.graphics.rootLayer.add(pf.graphics.createImmediateLayer(new ImmediateLayer.Renderer() {
      def render (surf :Surface) {
        surf.setFillColor(0x88000000).fillRect(0, 0, pf.graphics.width, device.statusBarHeight)
      }
    }).setDepth(Short.MaxValue))

    val mock = args.headOption.map(_ == "mock").getOrElse(false)
    PlayN.run(new Everything(mock, device, facebook))
  }

  private final val MillisPerMinute = 1000*60
}
