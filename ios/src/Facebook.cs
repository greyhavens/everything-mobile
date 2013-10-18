using System;
using System.Collections.Generic;
using System.Web;

using MonoTouch.UIKit;
using MonoTouch.Foundation;
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
    public RFuture authenticate () {
      return withSession(true, delegate (Callback cb) { cb.onSuccess(accessToken()); });
    }

    public void deauthorize () {
      var session = FBSession.ActiveSession;
      if (session != null) session.CloseAndClearTokenInformation();
    }

    // from Facebook interface
    public RFuture shareGotCard (string name, string descrip, string image, string link,
                                 string category, string series, string tgtFriendId, string refid) {
      return withSession(false, delegate (Callback cb) {
          var card = new OGCard(FBGraphObject.GraphObject().Handle);
          card.Title = name;
          card.Description = descrip;
          card.Image = image;
          card.Url = link;
          card.Category = category;

          var action = new OGGetCardAction(FBGraphObject.GraphObject().Handle);
          action.Card = card;
          action.Ref = refid;

          FBRequestConnection.StartForPostWithGraphPath("me/everythinggame:get", action,
            (FBRequestConnection conn, NSObject result, NSError error) => {
              PlayN.log().info("Graph share " + conn + ", result=" + result + ", error=" + error);
            });
      });
    }

    // public RFuture showDialog (string name, string caption, string descrip, string picURL,
    //                            string link, string tgtFriendId, string refid) {
    //   return withSession(false, delegate (Callback cb) {
    //     var paramz = toDict("title", name, "caption", caption, "description", descrip,
    //                         "image", picURL, "link", link, "ref", refid);
    //     // TODO: tgtFriendId
    //     FBWebDialogs.PresentFeedDialogModally(null, paramz, handler(cb));
    //   });
    // }

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

    protected RFuture withSession (bool forceAuth, SessionAction action) {
      var result = new DeferredPromise();
      if (!forceAuth && accessToken() != null) action(result);
      else {
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

    // protected FBWebDialogHandler handler (Callback cb) {
    //   return (result, url, error) => {
    //     if (error != null) cb.onFailure(new Exception(error.Description));
    //     else if (result == FBWebDialogResult.NotCompleted) cb.onSuccess(null);
    //     else if (url.Query == null) cb.onSuccess(null);
    //     else cb.onSuccess(HttpUtility.ParseQueryString(url.Query)["post_id"]);
    //   };
    // }

    // protected static NSMutableDictionary toDict (params string[] paramz) {
    //   NSMutableDictionary dict = new NSMutableDictionary();
    //   for (int ii = 0; ii < paramz.Length; ii += 2) {
    //     if (paramz[ii] != null) {
    //       dict.Add(new NSString(paramz[ii]), new NSString(paramz[ii+1]));
    //     }
    //   }
    //   // dict.Add(new NSString("show_error"), new NSString("true"));
    //   return dict;
    // }
  }

  public class OGCard : FBGraphObject {
    public OGCard () {}
    public OGCard (IntPtr ptr) : base (ptr) {}
    public string Id { get; set; }
    public string Url { get; set; }
    public string Title { get; set; }
    public string Image { get; set; }
    public string Description { get; set; }
    public string Category { get; set; }
  }

  public class OGGetCardAction : FBOpenGraphAction {
    public OGGetCardAction () {}
    public OGGetCardAction (IntPtr ptr) : base (ptr) {}
    public OGCard Card { get; set; }
  }
}
