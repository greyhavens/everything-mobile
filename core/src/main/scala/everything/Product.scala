//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

/** Models an in-app billing product. */
class Product (val sku :String, val price :String, val coins :Int)

/** Factory method &c for `Product`. */
object Product {

  /** The list of skus that should be registered with the IAP service. */
  val skus = Array("coins_5000", "coins_11000", "coins_24000")

  /** Creates a product given its `sku` and `price`.
    * `sku` must be of form blah_CCCC (e.g. coins_5000). */
  def apply (sku :String, price :String) = new Product(sku, price, try {
    sku.substring(sku.lastIndexOf("_")+1).toInt
  } catch {
    case t :Throwable => throw new IllegalArgumentException(s"Invalid SKU (need 'blah_CCCC'): $sku")
  })
}
