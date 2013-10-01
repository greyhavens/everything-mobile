//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;

import playn.core.util.Callback;
import react.RFuture;
import static playn.core.PlayN.*;

public class DroidBook implements Facebook {

    public DroidBook (EverythingActivity activity) {
        _activity = activity;
        _helper = new UiLifecycleHelper(activity, new Session.StatusCallback() {
            public void call (Session sess, SessionState state, Exception exn) {
                log().info("Session status [state=" + state + ", exn=" + exn + "]");
            }
        });
    }

    public void onCreate (Bundle state) {
        _helper.onCreate(state);
        Session.openActiveSession(_activity, false, null);
    }

    public void onPause () {
        _helper.onPause();
    }

    public void onResume () {
        _helper.onResume();
    }

    public void onSaveInstanceState (Bundle outState) {
        _helper.onSaveInstanceState(outState);
    }

    public void onDestroy () {
        _helper.onDestroy();
    }

    public void onActivityResult (int reqCode, int resCode, Intent data) {
        Session sess = Session.getActiveSession();
        if (sess != null) sess.onActivityResult(_activity, reqCode, resCode, data);

        _helper.onActivityResult(reqCode, resCode, data, new FacebookDialog.Callback() {
            public void onComplete (FacebookDialog.PendingCall call, Bundle data) {
                log().info("FB onComplete " + call);
            }
            public void onError (FacebookDialog.PendingCall call, Exception err, Bundle data) {
                log().warn("FB onError " + call + " " + err);
            }
        });
    }

    public String accessToken () {
        Session sess = Session.getActiveSession();
        return (sess == null) ? null : sess.getAccessToken();
    }

    // from Facebook
    public boolean isAuthed () { return Session.getActiveSession() != null; }

    // from Facebook
    public RFuture<String> authenticate () {
        return withSession(new Action<String>() { public void invoke (Session sess) {
            onSuccess(accessToken());
        }});
    }

    public void deauthorize () {
        Session sess = Session.getActiveSession();
        if (sess != null) sess.closeAndClearTokenInformation();
    }

    // from Facebook
    public RFuture<String> showDialog (String name, String caption, String descrip, String picURL,
                                       String link, String ref, String tgtFriendId) {
        List<String> friends = new ArrayList<String>();
        if (tgtFriendId != null) friends.add(tgtFriendId);
        FacebookDialog dialog = new FacebookDialog.ShareDialogBuilder(_activity).
            setName(name).
            setCaption(caption).
            setDescription(descrip).
            setPicture(picURL).
            setLink(link).
            setRef(ref).
            setFriends(friends).
            build();
        _helper.trackPendingDialogCall(dialog.present());

        return null; // TODO

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

    abstract class Action<T> extends DeferredPromise<T>  {
        public abstract void invoke (Session sess);
    }

    protected <T> RFuture<T> withSession (Action<T> action) {
        Session sess = Session.getActiveSession();
        if (sess != null && sess.isOpened()) action.invoke(sess);
        else {
            if (_pendingOp != null) {
                log().warn("Canceling existing pending FB op [op=" + _pendingOp + "]");
                _pendingOp.onFailure(new Exception("Usurped"));
            }
            _pendingOp = action;
            Session.openActiveSession(_activity, true, new Session.StatusCallback() {
                public void call (Session sess, SessionState state, Exception err) {
                    if (_pendingOp != null) {
                        if (state.isOpened()) {
                            _pendingOp.invoke(sess);
                            _pendingOp = null;
                        } else if (err != null) {
                            _pendingOp.onFailure(err);
                            _pendingOp = null;
                        } // otherwise this is some pointless state change like OPENING
                    }
                }
            });
        }
        return action;
    }

    protected Action<?> _pendingOp;
    protected final EverythingActivity _activity;
    protected final UiLifecycleHelper _helper;
}
