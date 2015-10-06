//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.model.OpenGraphAction;
import com.facebook.model.OpenGraphObject;
import com.facebook.widget.FacebookDialog;

import playn.core.util.Callback;
import react.RFuture;
import static playn.core.PlayN.*;

public class DroidBook implements Facebook {

    public DroidBook (EverythingActivity activity) {
        _activity = activity;
    }

    public void onCreate (Bundle state) {
        _helper = new UiLifecycleHelper(_activity, new Session.StatusCallback() {
            public void call (Session sess, SessionState state, Exception exn) {
                log().info("Session status [state=" + state + ", exn=" + exn + "]");
            }
        });
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
    public RFuture<String> authenticate (boolean forceReauth) {
        return withSession(forceReauth, new Action<String>() { public void invoke (Session sess) {
            onSuccess(accessToken());
        }});
    }

    public void deauthorize () {
        Session sess = Session.getActiveSession();
        if (sess != null) sess.closeAndClearTokenInformation();
    }

    abstract class Action<T> extends DeferredPromise<T>  {
        public abstract void invoke (Session sess);
    }

    protected <T> RFuture<T> withSession (boolean forceReauth, Action<T> action) {
        Session sess = Session.getActiveSession();
        if (!forceReauth && sess != null && sess.isOpened()) action.invoke(sess);
        else {
            if (_pendingOp != null) {
                log().warn("Canceling existing pending FB op [op=" + _pendingOp + "]");
                _pendingOp.onFailure(new Exception("Usurped"));
            }
            _pendingOp = action;
            if (forceReauth && sess != null) sess.closeAndClearTokenInformation();
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

    protected final EverythingActivity _activity;
    protected UiLifecycleHelper _helper;
    protected Action<?> _pendingOp;
}
