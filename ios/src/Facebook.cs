using System;
using System.Collections.Generic;
using MonoTouch.UIKit;
using MonoTouch.Foundation;
using MonoTouch.FacebookConnect;
using OFacebook = MonoTouch.FacebookConnect.Facebook;

using playn.core;
using playn.core.util;
using react;

namespace everything
{
  public class IOSFacebook : Facebook {

    public IOSFacebook () {
      FBSession.OpenActiveSession(false);
    }

    public void post (UIViewController rctrl, string name, string caption, string descrip,
                      string link, UIImage photo, string photoURL) {
      withSession(delegate (Exception authErr) {
        if (authErr != null) PlayN.log().warn("Failed to obtain session for post", authErr);
        else {
          bool shared = false;
          // try {
          //   shared = FBNativeDialogs.PresentShareDialogModallyFrom(
          //     rctrl, name, photo, new NSUrl(link),
          //     delegate (FBNativeDialogResult result, NSError error) {
          //       DLog.log.info("Native shared", "result", result, "error", error);
          //     });
          // } catch (Exception e) {
          //   DLog.log.warning("Native share choked", e);
          // }
          if (!shared) showDialog("feed", "name", name, "caption", caption, "description", descrip,
                                  "link", link, "picture", photoURL);
        }
      });
    }

    public void showDialog (string action, params string[] paramz) {
      NSMutableDictionary dict = new NSMutableDictionary();
      for (int ii = 0; ii < paramz.Length; ii += 2) {
        if (paramz[ii] != null) {
          dict.Add(new NSString(paramz[ii]), new NSString(paramz[ii+1]));
        }
      }
      oldFB().Dialog(action, dict, _noopDialogDel);
    }

    public bool handleOpenURL (NSUrl url) {
      var session = FBSession.ActiveSession;
      return (session == null) ? false : session.HandleOpenURL(url);
    }

    // from Facebook interface
    public bool isAuthed () {
      return accessToken() != null;
    }

    // from Facebook interface
    public RFuture authenticate () {
      RPromise result = RPromise.create();
      PlayN.log().info("Authenticating with Facebook...");
      withSession(delegate (Exception authErr) {
        IOSUtil.invokeLater(delegate {
          if (authErr != null) result.fail(authErr);
          else result.succeed(accessToken());
        });
      });
      return result;
    }

    // from Facebook interface
    public RFuture showCardDialog (string actionRef, string cardAction, string cardName,
                                   string cardDescrip, string imageURL, string everyURL,
                                   string targetId) {
      NSMutableDictionary dict = new NSMutableDictionary();
      addTo(dict, "name", cardName);
      addTo(dict, "caption", cardAction);
      addTo(dict, "description", cardDescrip);
      addTo(dict, "link", everyURL);
      addTo(dict, "picture", imageURL);
      addTo(dict, "ref", actionRef);
      addTo(dict, "actions", "[ { 'name': 'Collect Everything!', 'link': '" + everyURL + "' } ]");
      oldFB().Dialog("feed", dict, _noopDialogDel);
      return RFuture.success(""); // TODO
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

    public string accessToken () {
      var session = FBSession.ActiveSession;
      return (session == null) ? null : session.AccessToken;
    }

    // override public void getFriends (Callback callback) {
    //   DLog.log.info("Fetching Facebook friends...");
    //   withSession(delegate (Exception authErr) {
    //     if (authErr != null) callback.onFailure(authErr);
    //     else FBRequest.GetRequestForMyFriends.Start(delegate (FBRequestConnection connection,
    //                                                           NSObject result, NSError nserr) {
    //       try {
    //         if (nserr != null) throw new Exception(nserr.ToString());
    //         if (result == null) throw new Exception("No result for /me/friends request");
    //         FBGraphObject obj = (FBGraphObject)result;
    //         var data = NSArray.FromArray<NSObject>((NSArray)obj.ObjectForKey(new NSString("data")));
    //         var friends = new ArrayList();
    //         for (int ii = 0; ii < data.Length; ii++) {
    //           if (data[ii] is FBGraphObject) friends.add(toPerson((FBGraphObject)data[ii]));
    //           else DLog.log.warning("Unknown friend type " + data[ii]);
    //         }
    //         IOSUtil.invokeLater(delegate { callback.onSuccess(friends); });
    //       } catch (Exception e) {
    //         IOSUtil.invokeLater(delegate { callback.onFailure(e); });
    //       }
    //     });
    //   });
    // }

    public void sendInvite (string message) {
      withSession(delegate (Exception authErr) {
        if (authErr != null) PlayN.log().warn("Failed to obtain session for invite", authErr);
        else showDialog("apprequests", "message", message);
      });
    }

    protected static void addTo (NSMutableDictionary dict, String key, String value) {
      dict.Add(new NSString(key), new NSString(value));
    }

    protected delegate void SessionAction (Exception authErr);

    protected void withSession (SessionAction action) {
      if (accessToken() != null) action(null);
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
            if (errmsg == null) action(null);
            else action(new Exception(errmsg));
          });
      }
    }

    protected OFacebook oldFB () {
      if (_oldFB == null) {
        var session = FBSession.ActiveSession;
        _oldFB = new OFacebook(session.AppID, _noopSessDel);
        _oldFB.AccessToken = session.AccessToken;
        _oldFB.ExpirationDate = session.ExpirationDate;
      }
      return _oldFB;
    }

    protected OFacebook _oldFB;
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
