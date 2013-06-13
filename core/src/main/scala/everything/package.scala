//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

import react.{Slot, UnitSlot}

/** Global stuffs; mostly implicits to make using React/TriplePlay more pleasant. */
package object everything {

  // we have to call this one non-implicitly, so give it the good name
  def slot[A] (pf :PartialFunction[A,_]) = new Slot[A] {
    override def onEmit (value :A) = if (pf.isDefinedAt(value)) pf.apply(value)
  }

  implicit def toSlot[A] (f :Function1[A,_]) = new Slot[A] {
    override def onEmit (value :A) = f(value)
  }

  implicit def toUnitSlot (action : =>Unit) = new UnitSlot {
    override def onEmit = action
  }
}
