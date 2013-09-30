//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import android.app.Activity
import android.content.Intent;
import android.os.Bundle

import com.facebook.UiLifecycleHelper
import com.facebook.widget.FacebookDialog
import com.facebook.{Request, Response, Session, SessionState}

import playn.core.PlayN._
import playn.core.util.Callback
import react.RFuture

class DroidBook (activity :EverythingActivity) extends Facebook {

  def onCreate (state :Bundle) {
    helper.onCreate(state)
    Session.openActiveSession(activity, false, null)
  }

  def onPause () {
    helper.onPause()
  }

  def onResume () {
    helper.onResume()
  }

  def onSaveInstanceState (outState :Bundle) {
    helper.onSaveInstanceState(outState)
  }

  def onDestroy () {
    helper.onDestroy()
  }

  def onActivityResult (reqCode :Int, resCode :Int, data :Intent) {
    val sess = Session.getActiveSession
    if (sess != null) sess.onActivityResult(activity, reqCode, resCode, data)

    helper.onActivityResult(reqCode, resCode, data, new FacebookDialog.Callback() {
      override def onComplete (call :FacebookDialog.PendingCall, data :Bundle) {
        log.info(s"FB onComplete $call")
      }
      override def onError (call :FacebookDialog.PendingCall, err :Exception, data :Bundle) {
        log.warn(s"FB onError $call $err")
      }
    })
  }

  def accessToken :String = Session.getActiveSession match {
    case null => null
    case sess => sess.getAccessToken
  }

  // from Facebook
  override def isAuthed = (Session.getActiveSession != null)

  // from Facebook
  override def authenticate () = withSession { (_, cb) => cb.onSuccess(accessToken) }

  def deauthorize () {
    val sess = Session.getActiveSession
    if (sess != null) {
      sess.closeAndClearTokenInformation()
    }
  }

  // from Facebook
  override def showDialog (action :String, params :Map[String,String]) = withSession { (sess, cb) =>
    val dialog = new FacebookDialog.ShareDialogBuilder(activity).
      setName(params("name")).
      setCaption(params("caption")).
      setDescription(params("description")).
      setPicture(params("picture")).
      setLink(params("link")).
      setRef(params("ref")).
      build()
    helper.trackPendingDialogCall(dialog.present())
    // val fb = new OldFacebook(sess.getApplicationId)
    // fb.setAccessToken(sess.getAccessToken)
    // fb.setAccessExpires(sess.getExpirationDate.getTime)
    // activity.runOnUiThread(new Runnable() { def run () {
    //   val bundle = new Bundle()
    //   for (Array(key, value) <- params.grouped(2)) {
    //     if (value != null) bundle.putString(key, value)
    //   }
    //   fb.dialog(activity, action, bundle, new OldFacebook.DialogListener() {
    //     def onComplete (values :Bundle) { cb.onSuccess(values.getString("post_id")) }
    //     def onFacebookError (e :FacebookError) { cb.onFailure(e) }
    //     def onError (e :DialogError) { cb.onFailure(e) }
    //     def onCancel () { cb.onSuccess(null) }
    //   })
    // }})
  }

  class FBOp[T] (action :(Session, Callback[T]) => Unit) extends DeferredPromise[T] {
    def apply (sess :Session) = action(sess, this)
  }

  protected def withSession[T] (action :(Session, Callback[T]) => Unit) :RFuture[T] = {
    val op = new FBOp[T](action)
    val sess = Session.getActiveSession
    if (sess != null && sess.isOpened) op(sess)
    else {
      if (_pendingOp != null) {
        log.warn(s"Canceling existing pending FB op [op=${_pendingOp}]")
        _pendingOp.onFailure(new Exception("Usurped"))
      }

      _pendingOp = op
      Session.openActiveSession(activity, true, new Session.StatusCallback() {
        def call (sess :Session, state :SessionState, err :Exception) {
          if (_pendingOp != null) {
            if (state.isOpened) {
              _pendingOp(sess)
              _pendingOp = null
            } else if (err != null) {
              _pendingOp.onFailure(err)
              _pendingOp = null
            } // otherwise this is some pointless state change like OPENING
          }
        }
      })
    }
    op
  }

  // protected def invoke (req :Request) {
  //   activity.platform.invokeAsync(new Runnable { def run () {
  //     req.executeAndWait()
  //   }})
  // }

  protected var _pendingOp :FBOp[_] = _

  private val helper = new UiLifecycleHelper(activity, new Session.StatusCallback() {
    override def call (sess :Session, state :SessionState, exn :Exception) {
      log.info(s"Session status [state=$state, exn=$exn]")
    }
  })
}
