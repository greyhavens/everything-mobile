//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import android.app.Activity
import android.content.Intent;
import android.os.Bundle

import com.facebook.{Request, Response, Session, SessionState}
import com.facebook.model.GraphUser

import playn.core.PlayN._
import react.RPromise

class DroidBook (activity :EverythingActivity) extends Facebook {

  def onCreate () {
    Session.openActiveSession(activity, false, null)
  }

  def onActivityResult (requestCode :Int, resultCode :Int, data :Intent) {
    val sess = Session.getActiveSession
    if (sess != null) sess.onActivityResult(activity, requestCode, resultCode, data)
  }

  def post (name :String, caption :String, descrip :String, link :String, photoURL :String) {
    showDialog("feed", "name", name, "caption", caption, "description", descrip,
               "link", link, "picture", photoURL)
  }

  def accessToken :String = Session.getActiveSession match {
    case null => null
    case sess => sess.getAccessToken
  }

  override def isAuthed = (Session.getActiveSession != null)

  override def authenticate () = {
    val result = RPromise.create[String]()
    withSession(new FBAction[String](result) {
      def invoke (sess :Session) = succeed(accessToken)
    })
    result
  }

  def deauthorize () {
    val sess = Session.getActiveSession
    if (sess != null) {
      sess.closeAndClearTokenInformation()
    }
  }

  protected def showDialog (action :String, params :String*) {
    // withSession(new FBAction<Void>(CB.NOOP) {
    //   @SuppressWarnings("deprecated") // all of this crap is deprecated, yay!
    //   public void invoke (Session sess) {
    //     final Facebook fb = new Facebook(sess.getApplicationId())
    //     fb.setAccessToken(sess.getAccessToken())
    //     fb.setAccessExpires(sess.getExpirationDate().getTime())
    //     activity.runOnUiThread(new Runnable() { public void run () {
    //       Bundle bundle = new Bundle()
    //       for (int ii = 0 ii < params.length ii += 2) {
    //         if (params[ii+1] != null) bundle.putString(params[ii], params[ii+1])
    //       }
    //       fb.dialog(activity, action, bundle, new Facebook.DialogListener() {
    //         public void onComplete (Bundle values) {}
    //         public void onFacebookError (FacebookError e) {}
    //         public void onError (DialogError e) {}
    //         public void onCancel () {}
    //       })
    //     }})
    //   }
    // })
  }

  abstract class FBAction[T] (result :RPromise[T]) {

    def invoke (sess :Session) :Unit

    def succeed (value :T) {
      activity.platform.invokeLater(new Runnable() { def run () {
        result.succeed(value)
      }})
    }

    def fail (err :Throwable) {
      activity.platform.invokeLater(new Runnable() { def run () {
        result.fail(err)
      }})
    }

    protected def invoke (req :Request) {
      activity.platform.invokeAsync(new Runnable { def run () {
        req.executeAndWait()
      }})
    }
  }

  protected def withSession (action :FBAction[_]) {
    val sess = Session.getActiveSession
    if (sess != null && sess.isOpened) action.invoke(sess)
    else {
      if (_pendingAction != null) {
        log.warn(s"Canceling existing pending FB action [action=${_pendingAction}]")
        _pendingAction.fail(new Exception("Usurped"))
      }

      _pendingAction = action
      Session.openActiveSession(activity, true, new Session.StatusCallback() {
        def call (sess :Session, state :SessionState, err :Exception) {
          if (_pendingAction != null) {
            if (state.isOpened) {
              _pendingAction.invoke(sess)
              _pendingAction = null
            } else if (err != null) {
              _pendingAction.fail(err)
              _pendingAction = null
            } // otherwise this is some pointless state change like OPENING
          }
        }
      })
    }
  }

  protected var _pendingAction :FBAction[_] = _
}
