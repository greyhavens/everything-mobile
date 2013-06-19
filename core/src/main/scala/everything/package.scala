//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

import playn.core.util.Callback
import react.{AbstractSignal, AbstractValue, Slot, UnitSlot}

/** Global stuffs; mostly implicits to make using React/TriplePlay more pleasant. */
package object everything {

  type JBoolean  = java.lang.Boolean
  type JInteger  = java.lang.Integer
  type JMap[K,V] = java.util.Map[K,V]

  def rf[A,B] (f :A => B) = new react.Function[A,B] {
    def apply (a :A) = f(a)
  }

  def cb[T] (f :T => Unit) = new Callback[T] {
    def onSuccess (t :T) = f(t)
    def onFailure (cause :Throwable) {} // unused
  }

  implicit def slot[A] (f :Function1[A,_]) = new Slot[A] {
    override def onEmit (value :A) = f(value)
  }

  def slot[A] (pf :PartialFunction[A,_]) = new Slot[A] {
    override def onEmit (value :A) = if (pf.isDefinedAt(value)) pf.apply(value)
  }

  implicit def toUnitSlot (f :() => Unit) = new UnitSlot {
    override def onEmit = f()
  }

  def unitSlot[A] (action : =>Any) = new Slot[A] {
    override def onEmit (a :A) = action
  }
}
