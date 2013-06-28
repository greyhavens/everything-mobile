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

  /** Shows a share card dialog.
    * @return the story id on success, null if the dialog was canceled. */
  def showCardDialog (actionRef :String, cardAction :String, cardName :String,
                      cardDescrip :String, imageURL :String, everyURL :String,
                      targetId :String) :RFuture[String]
}
