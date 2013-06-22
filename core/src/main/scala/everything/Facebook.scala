//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import react.RFuture

/** Provides an interface to the Facebook API on the device. */
trait Facebook {

  /** Returns whether or not we've ever authed with Facebook. */
  def isAuthed :Boolean

  /** Authenticates with Facebook, adding the app if necessary.
    * @return a future which will provide the Facebook auth token. */
  def authenticate () :RFuture[String]
}
