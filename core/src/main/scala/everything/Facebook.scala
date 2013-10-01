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
    * @return (via future) the user's Facebook auth token. */
  def authenticate () :RFuture[String]

  /** Shows a Facebook feed dialog. `tgtFriendId` may be null.
    * @return (via future) the story id on success, null if the dialog was canceled. */
  def showDialog (name :String, caption :String, descrip :String, picURL :String, link :String,
                  tgtFriendId :String, ref :String) :RFuture[String]

  /** Shows a Facebook open graph dialog. `tgtFriendId` may be null.
    * @return (via future) the story id on success, null if the dialog was canceled. */
  def showGraphDialog (ogAction :String, ogType :String, props :JMap[String,String],
                       tgtFriendId :String, ref :String) :RFuture[String]
}
