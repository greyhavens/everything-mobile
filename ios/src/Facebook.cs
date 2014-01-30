using System;
using System.Collections.Generic;
using System.Web;

using MonoTouch.Foundation;
using MonoTouch.UIKit;
using MonoTouch.FacebookConnect;

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

    public void onActivated () {
      // We need to properly handle activation of the application with regards to SSO (e.g.,
      // returning from iOS 6.0 authorization dialog or from fast app switching).
      FBSession.ActiveSession.HandleDidBecomeActive();
    }

    public string accessToken () {
      var session = FBSession.ActiveSession;
      if (session == null) return null;
      var data = session.AccessTokenData;
      if (data == null) return null;
      DateTime expire = data.ExpirationDate;
      if (expire < DateTime.Now) return null;
      return data.AccessToken;
    }

    // from Facebook interface
    public bool isAuthed () {
      return accessToken() != null;
    }

    // from Facebook interface
    public RFuture authenticate (bool forceReauth) {
      return withSession(forceReauth, delegate (Callback cb) { cb.onSuccess(accessToken()); });
    }

    public void deauthorize () {
      var session = FBSession.ActiveSession;
      if (session != null) session.CloseAndClearTokenInformation();
    }

    // from Facebook interface
    public RFuture shareGotCard (string name, string descrip, string image, string link,
                                 string category, string series, string tgtFriendId, string refid) {

      // this results in com.facebook.sdk.error 5 for whatever reason; the same code (modulo
      // bullshit platform differences) works on Android; who fucking knows...

      // return withSession(false, delegate (Callback cb) {
      //   var card = new FBOpenGraphObject(FBGraphObject.OpenGraphObject.Handle);
      //   // card.SetType("everythinggame:get");
      //   card.SetTitle(name);
      //   card.SetDescription(new NSString(descrip));
      //   card.SetImage(new NSString(image));
      //   card.SetUrl(new NSString(link));
      //   card.SetObject(new NSString(category), "category");

      //   var action = new FBOpenGraphAction(FBGraphObject.OpenGraphAction.Handle);
      //   // action.SetType("everythinggame:get");
      //   action.SetObject(card, "card");
      //   action.SetRef(refid);

      //   FBRequestConnection.StartForPostWithGraphPath("me/everythinggame:get", action,
      //     (FBRequestConnection conn, NSObject result, NSError error) => {
      //       PlayN.log().info("Graph share " + conn + ", result=" + result + ", error=" + error);
      //     });
      // });
      return showDialog(name, category, descrip, image, link, tgtFriendId, refid);
    }

    public RFuture showDialog (string name, string caption, string descrip, string picURL,
                               string link, string tgtFriendId, string refid) {
      return withSession(false, delegate (Callback cb) {
        var paramz = toDict("name", name, "caption", caption, "description", descrip,
                            "picture", picURL, "link", link, "ref", refid);
        if (tgtFriendId != null) addToDict(paramz, "to", tgtFriendId);
        FBWebDialogs.PresentFeedDialogModally(null, paramz, handler(cb));
      });
    }

    // public RFuture nativeDialog (UIViewController rctrl, string name, string caption,
    //                              string descrip, string link, UIImage photo) {
    //   return withSession(false, delegate (Callback cb) {
    //     try {
    //       FBNativeDialogs.PresentShareDialogModallyFrom(
    //         rctrl, name, photo, new NSUrl(link),
    //         delegate (FBNativeDialogResult result, NSError error) {
    //           PlayN.log().info("Native shared [result=" + result + ", error=" + error + "]");
    //         });
    //       cb.onSuccess("");
    //     } catch (Exception e) {
    //       cb.onFailure(e);
    //     }
    //   });
    // }

    // public RFuture sendInvite (string message) {
    //   return withSession(false, delegate (Callback cb) {
    //     string title = "Invite Friends!"; // TODO
    //     FBWebDialogs.PresentRequestsDialogModally(null, message, title, null, handler(cb));
    //   });
    // }

    protected delegate void SessionAction (Callback cb);

    protected RFuture withSession (bool forceReauth, SessionAction action) {
      var result = new DeferredPromise();
      if (!forceReauth && accessToken() != null) action(result);
      else {
        var sess = FBSession.ActiveSession;
        if (forceReauth && sess != null) {
          // apparently per-FB retardation, we have to do all three of these things
          sess.CloseAndClearTokenInformation();
          if (sess.IsOpen) sess.Close();
          // FBSession.ActiveSession = null; // this crashes the Mono binding, yay!
        }
        PlayN.log().info("Authenticating with Facebook...");
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

            if (result.alreadyComplete()) PlayN.log().info("Repeated state change, ignoring...");
            // report success or failure
            else if (errmsg == null) action(result);
            else result.onFailure(new Exception(errmsg));
          });
      }
      return result;
    }

    protected FBWebDialogHandler handler (Callback cb) {
      return (result, url, error) => {
        if (error != null) cb.onFailure(new Exception(error.Description));
        else if (result == FBWebDialogResult.NotCompleted) cb.onSuccess(null);
        else if (url.Query == null) cb.onSuccess(null);
        else cb.onSuccess(HttpUtility.ParseQueryString(url.Query)["post_id"]);
      };
    }

    protected static NSMutableDictionary toDict (params string[] paramz) {
      NSMutableDictionary dict = new NSMutableDictionary();
      for (int ii = 0; ii < paramz.Length; ii += 2) {
        if (paramz[ii] != null) {
          addToDict(dict, paramz[ii], paramz[ii+1]);
        }
      }
      // addToDict(dict, "show_error", "true");
      return dict;
    }

    protected static void addToDict (NSMutableDictionary dict, string key, string value) {
      dict.Add(new NSString(key), new NSString(value));
    }
  }
}
