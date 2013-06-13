//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN
import playn.java.JavaPlatform

object EverythingJava {

  def main (args :Array[String]) {
    val config = new JavaPlatform.Config
    config.width = 320
    config.height = 480
    config.scaleFactor = 2
    JavaPlatform.register(config)
    PlayN.run(new Everything())
  }
}
