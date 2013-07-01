using System;
using System.Collections.Generic;
using MonoTouch.UIKit;
using MonoTouch.Foundation;
using MonoTouch.FacebookConnect;
using OldFacebook = MonoTouch.FacebookConnect.Facebook;

using playn.core;
using playn.core.util;
using react;

namespace everything
{
  public class IOSFacebook : Facebook {

    public IOSFacebook () {
      FBSession.OpenActiveSession(false);
    }

    public bool handleOpenURL (NSUrl url) {
      var session = FBSession.ActiveSession;
      return (session == null) ? false : session.HandleOpenURL(url);
    }

    public string accessToken () {
      var session = FBSession.ActiveSession;
      if (session == null) return null;
      if (session.ExpirationDate.SecondsSinceReferenceDate <
          NSDate.Now.SecondsSinceReferenceDate) return null;
      return session.AccessToken;
    }

    // from Facebook interface
    public bool isAuthed () {
      return accessToken() != null;
    }

    // from Facebook interface
    public RFuture authenticate () {
      PlayN.log().info("Authenticating with Facebook...");
      return withSession(delegate (Callback cb) { cb.onSuccess(accessToken()); });
    }

    public void deauthorize () {
      if (_oldFB != null) {
        try { _oldFB.Logout(); }
        catch (Exception e) { PlayN.log().warn("Choked clearing old FB object", e); }
        _oldFB = null;
      }
      var session = FBSession.ActiveSession;
      if (session != null) session.CloseAndClearTokenInformation();
    }

    // from Facebook interface
    public RFuture showDialog (string action, string[] paramz) {
      return withSession(delegate (Callback cb) {
        dialog(action, paramz);
        cb.onSuccess(""); // TODO
      });
    }

    public RFuture nativeDialog (UIViewController rctrl, string name, string caption,
                                  string descrip, string link, UIImage photo) {
      return withSession(delegate (Callback cb) {
        try {
          FBNativeDialogs.PresentShareDialogModallyFrom(
            rctrl, name, photo, new NSUrl(link),
            delegate (FBNativeDialogResult result, NSError error) {
              PlayN.log().info("Native shared [result=" + result + ", error=" + error + "]");
            });
          cb.onSuccess("");
        } catch (Exception e) {
          cb.onFailure(e);
        }
      });
    }

    public RFuture sendInvite (string message) {
      return withSession(delegate (Callback cb) {
        dialog("apprequests", "message", message);
        cb.onSuccess(""); // TODO
      });
    }

    protected delegate void SessionAction (Callback cb);

    protected RFuture withSession (SessionAction action) {
      var result = new DeferredPromise();
      if (accessToken() != null) action(result);
      else {
        FBSession.OpenActiveSession(new string[] { "email" }, true,
          delegate (FBSession session, FBSessionState state, NSError nserr) {
            PlayN.log().info("Facebook state change [state=" + state + ", error=" + nserr + "]");

            string errmsg;
            switch (state) {
            case FBSessionState.ClosedLoginFailed:
              errmsg = (nserr == null) ? "Login failed." : nserr.ToString(); break;
            case FBSessionState.CreatedOpening:
              errmsg = "Login was canceled"; break;
            default:
              errmsg = null; break;
            }

            // report success or failure
            if (errmsg == null) action(result);
            else result.onFailure(new Exception(errmsg));
          });
      }
      return result;
    }

    protected void dialog (string action, params string[] paramz) {
      NSMutableDictionary dict = new NSMutableDictionary();
      for (int ii = 0; ii < paramz.Length; ii += 2) {
        if (paramz[ii] != null) {
          dict.Add(new NSString(paramz[ii]), new NSString(paramz[ii+1]));
        }
      }
      oldFB().Dialog(action, dict, _noopDialogDel);
    }

    protected OldFacebook oldFB () {
      if (_oldFB == null) {
        var session = FBSession.ActiveSession;
        _oldFB = new OldFacebook(session.AppID, _noopSessDel);
        // PlayN.log().info("Using old FB with [token=" + session.AccessToken +
        //                  ", expire=" + session.ExpirationDate + "]");
        _oldFB.AccessToken = session.AccessToken;
        _oldFB.ExpirationDate = session.ExpirationDate;
      }
      return _oldFB;
    }

    protected OldFacebook _oldFB;
    // we need to keep a reference to these to prevent them from being collected
    protected FBSessionDelegate _noopSessDel = new NoopSessionDelegate();
    protected FBDialogDelegate _noopDialogDel = new NoopDialogDelegate();
  }

  class NoopSessionDelegate : FBSessionDelegate {
    override public void DidLogin () {}
    override public void DidNotLogin (bool cancelled) {}
    override public void DidExtendToken(string accessToken, NSDate expiresAt) {}
    override public void DidLogout () {}
    override public void SessionInvalidated () {}
  }

  class NoopDialogDelegate : FBDialogDelegate {
    override public void Completed (FBDialog dialog) {
      PlayN.log().info("FBDialog Completed");
    }
    override public void CompletedWithUrl (NSUrl url) {
      PlayN.log().info("FBDialog Completed With: " + url);
    }
    override public void NotCompletedWithUrl (NSUrl url) {
      PlayN.log().info("FBDialog Not Completed With: " + url);
    }
    override public void NotCompleted (FBDialog dialog) {
      PlayN.log().info("FBDialog Not Completed");
    }
    override public void Failed (FBDialog dialog, NSError error) {
      PlayN.log().info("FBDialog Failed: " + error);
    }
    override public bool ShouldOpenUrl (FBDialog dialog, NSUrl url) {
      PlayN.log().info("FBDialog Should open URL? " + url);
      return false;
    }
  }
}
