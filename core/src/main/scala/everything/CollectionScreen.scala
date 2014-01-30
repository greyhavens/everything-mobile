//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.mutable.{Set => MSet}

import playn.core.PlayN._
import playn.core._
import playn.core.util.Clock
import pythagoras.f.{Dimension, MathUtil}
import react.{IntValue, Value}
import tripleplay.anim.Flicker
import tripleplay.ui._
import tripleplay.ui.layout.{AxisLayout, TableLayout}
import tripleplay.util.{Interpolator, StyledText}

import com.threerings.everything.data._

class CollectionScreen (game :Everything, who :PlayerName) extends EveryScreen(game) {

  val (things, series) = (new IntValue(0), new IntValue(0))
  val collect = Value.create(null :PlayerCollection)
  val cbox = UI.stretchBox()
  var cview = new CollectionView(new java.util.HashMap[String,JMap[String,JList[SeriesCard]]]())

  val getCol = game.gameSvc.getCollection(who.userId)
  onShown.connect(unitSlot {
    getCol.onFailure(onFailure).onSuccess(slot { col =>
      collect.update(col)
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

    val all = UI.toggleButton("All") { cbox.set(cview) }
    val faves = UI.toggleButton("Faves") {
      // use our 'always up-to-date' favorites info if we're looking at our own collection
      def selfLike (s :SeriesCard) = game.likes.get(s.categoryId) == java.lang.Boolean.TRUE
      def ownerLike (s :SeriesCard) = collect.get.likes.contains(s.categoryId)
      cbox.set(filteredView(if (who.userId == game.self.get.userId) selfLike _ else ownerLike _,
                            s"${who.name} has no favorite series."))
    }
    val comp = UI.toggleButton("Done") {
      cbox.set(filteredView(s => s.things == s.owned, s"${who.name} has no completed series."))
    }
    val near = UI.toggleButton("One More!") {
      cbox.set(filteredView(s => s.things-1 == s.owned,
                            s"${who.name} has no almost complete series."))
    }
    val modes = UI.bgroup(all, faves, comp, near)
    val sel = new Selector(modes, all).preventDeselection
    modes.layer.setAlpha(0)

    root.add(headerPlate(UI.icon(UI.frameImage(UI.friendImage(who), 36, 36)),
                         UI.headerLabel(who.toString), bits),
             cbox.set(new Label("Loading...")), modes)

    // fade our bits in when the collection arrives
    getCol.onSuccess(slot { col =>
      things.update(col.countCards)
      series.update(col.countSeries)
      iface.animator.tweenAlpha(bits.layer).to(1).in(500).easeIn
      iface.animator.tweenAlpha(modes.layer).to(1).in(500).easeIn
    })
  }

  override def paint (clock :Clock) {
    super.paint(clock)
    cview.paint(clock)
  }

  override protected def layout () = AxisLayout.vertical().offStretch.gap(0)

  protected def filteredView (pred :SeriesCard => Boolean, emptyMsg :String) = {
    val group = UI.vgroup0().addStyles(Style.HALIGN.left).
      setStylesheet(Stylesheet.builder.add(classOf[Button], Style.FONT.is(UI.collectFont)).create())
    var lastParent = 0
    for ((cat, scats) <- collect.get.series ; (scat, ss) <- scats ; s <- ss) {
      if (pred(s)) {
        addSeriesLabel(group, Seq(cat, scat), lastParent, s)
        lastParent = s.parentId
      }
    }
    if (group.childCount == 0) UI.wrapLabel(emptyMsg).addStyles(Style.HALIGN.center)
    else UI.vscroll(group)
  }

  protected def addSeriesLabel (group :Group, path :Seq[String], lastParentId :Int,
                                series :SeriesCard) = {
    val pie = UI.pieImage(series.owned / series.things.toFloat, 8)
    if (series.parentId != lastParentId) group.add(UI.pathLabel(path, 18))
    val sbutton = UI.labelButton(series.name) {
      new SeriesScreen(game, who, path :+ series.name, series.categoryId).push()
    }.addStyles(Style.UNDERLINE.off, Style.AUTO_SHRINK.on, Style.HALIGN.left)
    sbutton.icon.update(Icons.image(pie))
    group.add(sbutton)
  }

  class CollectionView (data :JMap[String,JMap[String,JList[SeriesCard]]]) extends Shim(1, 1) {

    layer.setHitTester(UI.absorber)

    abstract class FloatingLabel (val text :String, parent :ParentLabel, val next :FloatingLabel) {
      lazy val image = renderImage
      def renderImage = StyledText.span(text, UI.collectStyle).toImage()

      def left :Float = if (parent == null) 0 else parent.left + 15
      def last :FloatingLabel = if (next == nil) this else next.last

      def selfHeight :Float = image.height
      def height :Float = selfHeight + next.height
      def nextHeight :Float =
        if (next != nil) next.image.height
        else if (parent != null) parent.nextHeight
        else 0

      var layY = 0f
      def layout (y :Float) :Float = next.layout(layoutSelf(y))
      protected def layoutSelf (y :Float) :Float = {
        layY = y
        y + image.height
      }

      def render (surf :Surface, y :Float, maxy :Float) :Float = {
        if (y >= maxy) y
        else {
          surf.setAlpha(math.min(1, (maxy-y)/image.height))
          val ny = renderSelf(surf, left, y, maxy)
          surf.setAlpha(1)
          next.render(surf, ny, maxy)
        }
      }

      protected def renderSelf (surf :Surface, x :Float, y :Float, maxy :Float) :Float = {
        surf.drawImage(image, x, y)
        if (parent != null) {
          val pl = parent.left
          val hh = image.height/2
          surf.drawLine(pl+7, y+hh, pl+13, y+hh, 1)
          parent.childRenderedAt(this, y)
        }
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
      text :String, kidCount :Int, parent :ParentLabel, next :FloatingLabel, sel :Selection
    ) extends FloatingLabel(text, parent, next) {

      val numImg = StyledText.span(kidCount.toString, UI.statusStyle).toImage()
      lazy val children = makeChildren
      lazy val lastChild = children.last

      var childrenShowing = false
      var childrenHeight = 0f
      var refChildrenHeight = 0f
      var animAccum = 0f
      var animTime = 0f
      var lastChildBot = 0f

      def makeChildren :FloatingLabel

      def select (selected :Boolean) {
        if (childrenShowing) {
          childrenShowing = false
        } else {
          childrenShowing = true
          if (selected) scrollTarget = this
        }
        refChildrenHeight = children.height
        animAccum = 0
        animTime = math.min(refChildrenHeight*2, 500) // .5 "pixel"/ms
        noteAnimating(this, true)
      }

      def childRenderedAt (label :FloatingLabel, y :Float) {
        if (label == lastChild) {
          lastChildBot = math.min(lastChildBot, y + label.image.height/2)
        }
      }

      override def selfHeight = super.selfHeight + (if (childrenShowing) children.height else 0)

      override protected def layoutSelf (y :Float) = {
        val bot = super.layoutSelf(y)
        if (childrenShowing) children.layout(bot) else bot
      }

      override protected def renderSelf (surf :Surface, x :Float, y :Float, maxy :Float) = {
        val ny = super.renderSelf(surf, x, y, maxy)

        // if our children are animating, update them
        val ch = if (animTime == 0) childrenHeight else {
          animAccum = math.min(animTime, animAccum + frameDt)
          val animPct = animAccum / animTime
          val newCH = refChildrenHeight * (if (childrenShowing) animPct else 1-animPct)
          if (parent != null) {
            parent.childrenHeight -= childrenHeight
            parent.childrenHeight += newCH
          }
          if (animAccum == animTime) {
            animTime = 0
            noteAnimating(this, false)
          }
          childrenHeight = newCH
          newCH
        }

        // fade our count in/out as our children are hidden/shown
        val numAlpha = 1 - math.max(0, ch / 10)
        if (numAlpha > 0) surf.setAlpha(numAlpha).
          drawImage(numImg, width-10-numImg.width, y+(image.height-numImg.height)/2).
          setAlpha(1)

        // render whatever part of our children is visible
        val bot = math.min(maxy, ny + ch)
        lastChildBot = bot
        if (childrenHeight > 0) children.render(surf, ny, bot)

        // render the line connecting us to our children
        if (ch > 0 && lastChildBot > ny-5) surf.drawLine(left+7, ny-5, left+7, lastChildBot, 1)
        ny + ch
      }

      override def onClick (x :Float, y :Float) = {
        if (childrenHeight == 0) super.onClick(x, y)
        else {
          val eheight = selfHeight
          if (y > eheight) next.onClick(x, y-eheight)
          else if (y > image.height) children.onClick(x, y-image.height)
          else super.onClick(x, y)
        }
      }

      override def onTap () {
        sel.set(this)
        invalidate()
      }
    }

    val nil = new FloatingLabel("", null, null) {
      override def height = 0
      override def layout (y :Float) = y
      override def render (surf :Surface, y :Float, maxy :Float) = y
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
              new FloatingLabel(s.name, this, next) {
                val pie = UI.pieImage(s.owned / s.things.toFloat, 8)
                override def renderImage = {
                  val avail = size.width - left - pie.width - 2
                  var stxt = StyledText.span(text, UI.collectStyle)
                  while (stxt.width > avail && stxt.style.font.size > 10) {
                    stxt = stxt.resize(stxt.style.font.size-1)
                  }
                  stxt.toImage()
                }

                override def renderSelf (surf :Surface, x :Float, y :Float, maxy :Float) = {
                  surf.drawImage(pie, x, y + image.height/2 - pie.height/2)
                  super.renderSelf(surf, x + pie.width + 2, y, maxy)
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
        cats.render(surf, flicker.position, Short.MaxValue)
      }
    }

    var flicker = {
      val f = new Flicker(0, -height, 0)
      f.clicked.connect(slot { ev => cats.onClick(ev.localX, ev.localY - f.position) })
      layer.addListener(f)
      f
    }

    var labels = graphics.createImmediateLayer(1, 1, renderer)
    var scrollTarget :ParentLabel = _
    var frameDt :Float = 0
    val active = MSet[ParentLabel]()

    def paint (clock :Clock) {
      frameDt = clock.dt
      flicker.paint(clock)
    }

    def noteAnimating (label :ParentLabel, isAnim :Boolean) {
      // disable the flicker while anything is animating
      if (isAnim) active += label else active -= label
      flicker.enabled.update(active.isEmpty)
    }

    override def layout () {
      super.layout()
      labels.destroy()
      labels = graphics.createImmediateLayer(size.width.toInt, size.height.toInt, renderer)
      layer.add(labels)
      // adjusting the min won't cause anything to render at a different position, yay!
      flicker.min = size.height - cats.layout(0)
      // if we have a label that wants to be scrolled into view, do that now
      if (scrollTarget != null) {
        val ttop = math.max(-scrollTarget.layY, flicker.min)
        val tbot = ttop - scrollTarget.selfHeight - scrollTarget.nextHeight
        val cbot = flicker.position - size.height
        val tpos = math.max(ttop, if (cbot > tbot) tbot+size.height else flicker.position)
        val npos = math.min(0, tpos) // don't scroll off the top
        scrollTarget = null
        // attempt to scroll all of our children into view
        iface.animator.setValue(flicker.enabled, false :JBoolean).`then`.
          tween(flicker.posValue).to(npos).in(500).easeInOut.`then`.
          setValue(flicker.enabled, true :JBoolean)
      }
      // TODO: animate a scrollbar...
    }
  }
}
