//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.RFuture

/** Models an in-app billing product. */
case class Product (sku :String, price :String) {
  val coins :Int = try {
    sku.substring(sku.lastIndexOf("_")+1).toInt
  } catch {
    case t :Throwable => throw new IllegalArgumentException(s"Invalid SKU (need 'blah_CCCC'): $sku")
  }
}

/** Abstracts away services provided by underlying OS/device. */
trait Device {

  /** Returns the height of our translucent status bar (if any). */
  def statusBarHeight :Float

  /** Returns the current timezone offset as "minutes before GMT". */
  def timeZoneOffset :Int

  /** Formats a date for `flipped on` etc. UI. */
  def formatDate (when :Long) :String

  /** Returns info on the in-app billing products. */
  def getProducts :RFuture[Seq[Product]]

  /** Initiates a purchase of the specified product. Will either result in a callback to
    * `redeemPurchase` or a failure will be reported on the returned future. */
  def buyProduct (sku :String) :RFuture[Unit]
}
