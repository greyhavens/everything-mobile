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
    * @param forceReauth if true, any cached authentication information will be ignored.
    * @return (via future) the user's Facebook auth token. */
  def authenticate (forceReauth :Boolean) :RFuture[String]

  /** Shows a dialog allowing player to share that they got a card. */
  def shareGotCard (name :String, descrip :String, image :String, link :String, category :String,
                    series :String, tgtFriendId :String, ref :String) :RFuture[String]
}
