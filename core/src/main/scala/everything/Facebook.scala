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
                  ref :String, tgtFriendId :String) :RFuture[String]

  // TODO: implementing sharing "got card", "completed series" via Open Graph API
  // shareGraph (ogType :String, props :Map[String,String]) :RFuture[String]
}
