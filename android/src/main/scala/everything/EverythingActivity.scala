//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.android.GameActivity
import playn.core.PlayN

class EverythingActivity extends GameActivity {

  override def main () {
    PlayN.run(new Everything())
  }
}
