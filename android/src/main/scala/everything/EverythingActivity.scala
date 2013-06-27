//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import android.content.Intent
import android.os.Bundle
import java.text.DateFormat
import java.util.{Date, TimeZone}
import playn.android.GameActivity
import playn.core.{Font, PlayN}
import react.RFuture

class EverythingActivity extends GameActivity {

  val device = new Device {
    def timeZoneOffset = {
      val tz = TimeZone.getDefault
      // Java returns millis to add to GMT, we want minutes to subtract from GMT
      -tz.getOffset(System.currentTimeMillis)/MillisPerMinute
    }
    def formatDate (when :Long) = _dfmt.format(new Date(when))
    private val _dfmt = DateFormat.getDateInstance()
  }
  val facebook = new DroidBook(this)
  lazy val game = new Everything(device, facebook)

  override def onCreate (savedInstanceState :Bundle) {
    super.onCreate(savedInstanceState)
    facebook.onCreate()
  }

  override def onActivityResult (requestCode :Int, resultCode :Int, data :Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    facebook.onActivityResult(requestCode, resultCode, data)
  }

  override def main () {
    // default to smoothing when rendering canvas images
    platform.graphics.setCanvasFilterBitmaps(true)
    // we have only @2x resources, so use those
    platform.assets.setAssetScale(2)
    // register our custom fonts
    platform.graphics.registerFont("fonts/copper.ttf", "Copperplate Gothic Bold", Font.Style.PLAIN)
    platform.graphics.registerFont("fonts/treasure.ttf", "Treasure Map Deadhand", Font.Style.PLAIN)
    platform.graphics.registerFont("fonts/josschrift.ttf", "Josschrift", Font.Style.PLAIN)
    // start the ball rolling
    PlayN.run(game)
  }

  override def platform = super.platform // make visible to friends
  override def usePortraitOrientation = true
  override def logIdent = "every"

  override def scaleFactor = {
    val dm = getResources.getDisplayMetrics
    val (dwidth, dheight) = (dm.widthPixels, dm.heightPixels)
    // we may be in landscape right now, because Android is fucking retarded
    val (width, height) = if (dwidth > dheight) (dheight, dwidth) else (dwidth, dheight)
    math.min(width / 320f, height / 480f)
  }

  override def onBackPressed () {
    // only allow BACK to exit the app if we're on the main menu screen (and not currently
    // switching between screens)
    if (!game.screens.isTransiting && game.screens.top.isInstanceOf[MainMenuScreen]) {
      super.onBackPressed();
    }
  }

  private final val MillisPerMinute = 1000*60
}
