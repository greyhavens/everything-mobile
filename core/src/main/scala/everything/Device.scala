//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.RFuture

/** Abstracts away services provided by underlying OS/device. */
trait Device {

  /** Returns the height of our translucent status bar (if any). */
  def statusBarHeight :Float

  /** Returns the current timezone offset as "minutes before GMT". */
  def timeZoneOffset :Int

  /** Formats a date for `flipped on` etc. UI. */
  def formatDate (when :Long) :String

  /** Returns the current hour of the day. */
  def hourOfDay :Int

  /** Schedules the next grid notification and app icon badge, clearing any currently scheduled
    * notification and app icon badge in the process. */
  def scheduleGridNotification (when :Long) :Unit

  /** Returns info on the in-app billing products. */
  def getProducts :RFuture[Array[Product]]

  /** Initiates a purchase of the specified product. Will either result in a callback to
    * `game.redeemPurchase` or a failure will be reported on the returned future. */
  def buyProduct (game :Everything, sku :String) :RFuture[JVoid]
}
