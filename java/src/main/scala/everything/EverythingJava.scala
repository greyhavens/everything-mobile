//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.text.DateFormat
import java.util.{Date, TimeZone}
import playn.core.PlayN
import playn.core.util.Callback
import playn.java.JavaPlatform
import react.RFuture

object EverythingJava {

  def main (args :Array[String]) {
    val fbId = if (false) "1008138021" /*testy*/ else "540615819" /*mdb*/

    val config = new JavaPlatform.Config
    config.width = 320
    config.height = 480
    config.scaleFactor = 2
    config.storageFileName = "playn" + fbId

    val platform = JavaPlatform.register(config)
    platform.graphics.registerFont("Copperplate Gothic Bold", "fonts/copper.ttf")
    platform.graphics.registerFont("Treasure Map Deadhand", "fonts/treasure.ttf")
    platform.graphics.registerFont("Josschrift", "fonts/josschrift.ttf")

    val facebook = new Facebook {
      def isAuthed = true
      def authenticate () = RFuture.success("test:" + fbId)
      def showDialog (action :String, params :Array[String]) = RFuture.success[String](null)
    }
    val device = new Device {
      def timeZoneOffset = {
        val tz = TimeZone.getDefault
        // Java returns millis to add to GMT, we want minutes to subtract from GMT
        -tz.getOffset(System.currentTimeMillis)/MillisPerMinute
      }
      def formatDate (when :Long) = _dfmt.format(new Date(when))
      private val _dfmt = DateFormat.getDateInstance()
    }
    val mock = args.headOption.map(_ == "mock").getOrElse(false)
    PlayN.run(new Everything(mock, device, facebook))
  }

  private final val MillisPerMinute = 1000*60
}
