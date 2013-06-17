//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.util.Callback
import react.RFuture

/** Provides an interface to the Facebook API on the device. */
trait Facebook {

  /** Returns the current Facebook userId. Only valid after `authenticate`. */
  def userId :String

  /** Returns the current Facebook auth token. Only valid after `authenticate`. */
  def authToken :String

  /** Authenticates with Facebook, adding the app if necessary.
    * @return a future which will provide the Facebook userId. */
  def authenticate () :RFuture[String]
}
