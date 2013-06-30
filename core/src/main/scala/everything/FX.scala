//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.Layer
import scala.util.Random
import tripleplay.anim.AnimBuilder

/** Handles doing fancy FX on a layer. */
class FX (host :EveryScreen, layer :Layer.HasSize) {

  def delay (range :Int, incr :Int) = {
    _anim = _anim.delay(Random.nextInt(range)*incr).`then`
    this
  }

  def fadeIn (duration :Int) = {
    _anim = _anim.tweenAlpha(layer).from(0).to(1).in(duration).`then`
    this
  }

  def flyIn (duration :Int) = {
    val r = Random.nextFloat()
    val (x, y) = Random.nextInt(4) match {
      case 0 /*t*/ => (-30f + r*(host.width+60f), -30f)
      case 1 /*r*/ => (host.width+30f,            -30f + r*(host.height+60f))
      case 2 /*b*/ => (-30f + r*(host.width+60f), host.height+30f)
      case 3 /*l*/ => (-30f,                      -30f + r*(host.height+60f))
    }
    val (tx, ty) = (layer.tx, layer.ty)
    val lpos = Layer.Util.screenToLayer(layer.parent, x, y)
    layer.setTranslation(lpos.x, lpos.y)
    _anim = _anim.tweenXY(layer).to(tx, ty).in(duration).easeOutBack.`then`
  }

  def dropIn (scale :Float, duration :Int) = {
    val (otx, oty, ox, oy) = (layer.tx, layer.ty, layer.originX, layer.originY)
    val (dx, dy) = (layer.width/2 - ox, layer.height/2 - oy)
    // change origin to center, animate the scale, then change restore origin
    def reOrigin (tx :Float, ty :Float, ox :Float, oy :Float) = new Runnable() {
      def run = layer.setTranslation(tx, ty).setOrigin(ox, oy)
    }
    _anim = _anim.action(reOrigin(otx+dx, oty+dy, layer.width/2, layer.height/2)).`then`.
      tweenScale(layer).from(scale).to(1).in(duration).easeOutBack.`then`.
      action(reOrigin(otx, oty, ox, oy)).`then`
    this
  }

  def popIn (duration :Int) = dropIn(0, duration)

  private var _anim :AnimBuilder = host.iface.animator
}
