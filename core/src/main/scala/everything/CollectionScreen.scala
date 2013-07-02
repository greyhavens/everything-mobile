//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.annotation.tailrec
import scala.collection.JavaConversions._

import playn.core.PlayN._
import playn.core._
import playn.core.util.Clock
import pythagoras.f.{Dimension, MathUtil}
import react.IntValue
import tripleplay.anim.Flicker
import tripleplay.ui._
import tripleplay.ui.layout.AxisLayout
import tripleplay.ui.layout.TableLayout

import com.threerings.everything.data._

class CollectionScreen (game :Everything, who :PlayerName) extends EveryScreen(game) {

  val (things, series) = (new IntValue(0), new IntValue(0))
  val cbox = UI.stretchBox()
  var cview = new CollectionView(new java.util.HashMap[String,JMap[String,JList[SeriesCard]]]())

  val getColl = game.gameSvc.getCollection(who.userId)
  onShown.connect(unitSlot {
    getColl.onFailure(onFailure).onSuccess(slot { col =>
      cview = new CollectionView(col.series)
      cbox.set(cview)
    })
  }).once()

  override def createUI () {
    val bits = UI.hgroup(UI.tipLabel("Things:"),
                         new ValueLabel(things).addStyles(Style.FONT.is(UI.tipFont)),
                         UI.tipLabel("Series:"),
                         new ValueLabel(series).addStyles(Style.FONT.is(UI.tipFont)))
    bits.layer.setAlpha(0)
    root.add(headerPlate(UI.icon(UI.frameImage(UI.friendImage(who), 36, 36)),
                         UI.headerLabel(who.toString), bits),
             cbox.set(new Label("Loading...")))
    // fade our bits in when the collection arrives
    getColl.onSuccess(slot { col =>
      things.update(col.countCards)
      series.update(col.countSeries)
      iface.animator.tweenAlpha(bits.layer).to(1).in(500).easeIn
    })
  }

  override def paint (clock :Clock) {
    super.paint(clock)
    cview.paint(clock)
  }

  class CollectionView (data :JMap[String,JMap[String,JList[SeriesCard]]]) extends Shim(1, 1) {

    layer.setHitTester(UI.absorber)

    abstract class FloatingLabel (text :String, parent :FloatingLabel, val next :FloatingLabel) {
      val image = UI.collectCfg.toImage(text)

      def left :Float = if (parent == null) 0 else parent.left + 15
      def height :Float = image.height + next.height
      def last :FloatingLabel = if (next == nil) this else next.last

      var curY = 0f
      def layout (y :Float) :Float = next.layout(layoutSelf(y))
      protected def layoutSelf (y :Float) :Float = {
        curY = y
        y + image.height
      }

      def render (surf :Surface, y :Float) :Float = next.render(surf, renderSelf(surf, left, y))
      protected def renderSelf (surf :Surface, x :Float, y :Float) :Float = {
        surf.drawImage(image, x, y)
        // TODO: animate ourselves into this position instead of jumping right there
        y + image.height
      }

      def onClick (x :Float, y :Float) :Boolean = {
        if (y > image.height) next.onClick(x, y-image.height)
        else {
          onTap()
          true
        }
      }

      def onTap () :Unit
    }

    class Selection {
      var active :ParentLabel = _
      def set (label :ParentLabel) {
        if (active == label) {
          active = null
          label.select(false)
        } else {
          if (active != null) active.select(false)
          active = label
          active.select(true)
        }
      }
    }

    abstract class ParentLabel (
      text :String, kids :Int, parent :FloatingLabel, next :FloatingLabel, sel :Selection
    ) extends FloatingLabel(text, parent, next) {

      def makeChildren :FloatingLabel
      lazy val children = makeChildren
      lazy val lastChild = children.last
      var childrenShowing = false
      val cimage = UI.statusCfg.toImage(kids.toString)

      def select (selected :Boolean) {
        // TODO: animate
        childrenShowing = selected
      }

      override def height = super.height + (if (childrenShowing) children.height else 0)

      override protected def layoutSelf (y :Float) = {
        val bot = super.layoutSelf(y)
        if (childrenShowing) children.layout(bot) else bot
      }

      override protected def renderSelf (surf :Surface, x :Float, y :Float) = {
        val ny = super.renderSelf(surf, x, y)

        if (!childrenShowing) {
          surf.drawImage(cimage, width-10-cimage.width, y+(image.height-cimage.height)/2)
          ny

        } else {
          val cy = children.render(surf, ny)
          val hh = image.height/2
          val lh = lastChild.curY + flicker.position
          // render the lines connecting us to our children
          surf.drawLine(left+7, ny-5, left+7, lh+hh, 1)
          var child = children
          while (child != nil) {
            val ch = child.curY + flicker.position
            surf.drawLine(left+7, ch+hh, left+13, ch+hh, 1)
            child = child.next
          }
          cy
        }
      }

      override def onClick (x :Float, y :Float) = {
        if (!childrenShowing) super.onClick(x, y)
        else {
          val eheight = image.height + children.height
          if (y > eheight) next.onClick(x, y-eheight)
          else if (y > image.height) children.onClick(x, y-image.height)
          else super.onClick(x, y)
        }
      }

      override def onTap () {
        trigger = this
        sel.set(this)
        invalidate()
      }
    }

    val nil = new FloatingLabel("", null, null) {
      override def height = 0
      override def layout (y :Float) = y
      override def render (surf :Surface, y :Float) = y
      override def onClick (x :Float, y :Float) = false
      override def onTap () {}
    }

    val openCat = new Selection()
    val cats = (data :\ nil) { case ((cname, subs), next) =>
      new ParentLabel(cname, subs.values.map(_.size).sum, null, next, openCat) {
        val openSubCat = new Selection()
        def makeChildren = (subs :\ nil) { case ((scname, series), next) =>
          new ParentLabel(scname, series.size, this, next, openSubCat) {
            def makeChildren = (series :\ nil) { case (s, next) =>
              // TODO: series label
              new FloatingLabel(s.name, this, next) {
                val pie = UI.pieImage(s.owned / s.things.toFloat, 8)
                override def renderSelf (surf :Surface, x :Float, y :Float) = {
                  surf.drawImage(pie, x, y + image.height/2 - pie.height/2)
                  super.renderSelf(surf, x + pie.width + 2, y)
                }
                override def onTap () {
                  new SeriesScreen(game, who, Array(cname, scname, s.name), s.categoryId).push()
                }
              }
            }
          }
        }
      }
    }

    val renderer = new ImmediateLayer.Renderer() {
      def render (surf :Surface) {
        surf.setFillColor(UI.textColor)
        cats.render(surf, flicker.position)
      }
    }

    var flicker = {
      val f = new Flicker(0, -height, 0)
      f.clicked.connect(slot { ev => cats.onClick(ev.localX, ev.localY - f.position) })
      layer.addListener(f)
      f
    }

    var labels = graphics.createImmediateLayer(1, 1, renderer)
    var trigger :FloatingLabel = _

    def paint (clock :Clock) {
      flicker.paint(clock)
    }

    override def layout () {
      super.layout()
      labels.destroy()
      labels = graphics.createImmediateLayer(width.toInt, height.toInt, renderer)
      layer.add(labels)
      // make sure that the trigger label is in the same screen position after "relayout"
      if (trigger != null) {
        val otpos = trigger.curY
        val bot = cats.layout(0)
        val ntpos = trigger.curY
        flicker.position += (otpos-ntpos)
        flicker.min = size.height - bot
      } else {
        flicker.min = size.height - cats.layout(0)
      }
      // TODO: animate a scrollbar...
    }
  }
}
