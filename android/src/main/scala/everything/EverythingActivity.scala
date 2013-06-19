//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.android.GameActivity
import playn.core.PlayN
import react.RFuture

class EverythingActivity extends GameActivity {

  override def main () {
    // default to smoothing when rendering canvas images
    platform.graphics.setCanvasFilterBitmaps(true)
    // we have only @2x resources, so use those
    platform.assets.setAssetScale(2)

    PlayN.run(new Everything(new Facebook {
      def userId = "1008138021"
      def authToken = "testToken"
      def authenticate () = RFuture.success(userId)
    }))
  }

  override def usePortraitOrientation = true
  override def logIdent = "every"

  override def scaleFactor = {
    // val dm = getResources.getDisplayMetrics
    // val (dwidth, dheight) = (dm.widthPixels, dm.heightPixels)
    // // we may be in landscape right now, because Android is fucking retarded
    // val (width, height) = if (dwidth > dheight) (dheight, dwidth) else (dwidth, dheight)
    // math.min(width / 320f, height / 480f)
    2 // TODO: switch between 1 and 2 based on screen size?
  }
}
