//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.util.TimeZone
import playn.core.PlayN
import playn.core.util.Callback
import playn.java.JavaPlatform
import react.RFuture

object EverythingJava {

  def main (args :Array[String]) {
    val facebookUserId = "540615819" // mdb
    // val facebookUserId = "1008138021" // testy

    val config = new JavaPlatform.Config
    config.width = 320
    config.height = 480
    config.scaleFactor = 2
    config.storageFileName = s"playn${facebookUserId}"

    val platform = JavaPlatform.register(config)
    platform.graphics.registerFont("Copperplate Gothic Bold", "fonts/copper.ttf")
    platform.graphics.registerFont("Treasure Map Deadhand", "fonts/treasure.ttf")

    val facebook = new Facebook {
      def userId = facebookUserId
      def authToken = "testToken"
      def authenticate () = RFuture.success(userId)
    }
    val device = new Device {
      def timeZoneOffset = {
        val tz = TimeZone.getDefault
        // Java returns millis to add to GMT, we want minutes to subtract from GMT
        -tz.getOffset(System.currentTimeMillis)/MillisPerMinute
      }
    }
    PlayN.run(new Everything(device, facebook))
  }

  private final val MillisPerMinute = 1000*60
}
