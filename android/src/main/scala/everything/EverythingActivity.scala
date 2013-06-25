//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.util.TimeZone
import playn.android.GameActivity
import playn.core.{Font, PlayN}
import react.RFuture

class EverythingActivity extends GameActivity {

  override def main () {
    // default to smoothing when rendering canvas images
    platform.graphics.setCanvasFilterBitmaps(true)
    // we have only @2x resources, so use those
    platform.assets.setAssetScale(2)
    // register our custom fonts
    platform.graphics.registerFont("fonts/copper.ttf", "Copperplate Gothic Bold", Font.Style.PLAIN)
    platform.graphics.registerFont("fonts/treasure.ttf", "Treasure Map Deadhand", Font.Style.PLAIN)
    platform.graphics.registerFont("fonts/josschrift.ttf", "Josschrift", Font.Style.PLAIN)

    val facebook = new Facebook {
      def userId = "1008138021" // testy
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

  override def usePortraitOrientation = true
  override def logIdent = "every"

  override def scaleFactor = {
    val dm = getResources.getDisplayMetrics
    val (dwidth, dheight) = (dm.widthPixels, dm.heightPixels)
    // we may be in landscape right now, because Android is fucking retarded
    val (width, height) = if (dwidth > dheight) (dheight, dwidth) else (dwidth, dheight)
    if (height > 800) 2 else 1
  }

  private final val MillisPerMinute = 1000*60
}
