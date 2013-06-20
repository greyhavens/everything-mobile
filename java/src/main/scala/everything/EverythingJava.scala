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
    val config = new JavaPlatform.Config
    config.width = 320
    config.height = 480
    config.scaleFactor = 2
    JavaPlatform.register(config)
    PlayN.run(new Everything(new Device {
      def timeZoneOffset = {
        val tz = TimeZone.getDefault
        // Java returns millis to add to GMT, we want minutes to subtract from GMT
        -tz.getOffset(System.currentTimeMillis)/MillisPerMinute
      }
    }, new Facebook {
      def userId = "540615819"
      def authToken = "testToken"
      def authenticate () = RFuture.success(userId)
    }))
  }

  private final val MillisPerMinute = 1000*60
}
