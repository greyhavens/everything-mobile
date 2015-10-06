//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything;

import java.util.Arrays;

import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSURL;
import org.robovm.objc.block.VoidBlock2;
import org.robovm.pods.facebook.core.FBSDKAccessToken;
import org.robovm.pods.facebook.login.FBSDKDefaultAudience;
import org.robovm.pods.facebook.login.FBSDKLoginManager;
import org.robovm.pods.facebook.login.FBSDKLoginManagerLoginResult;

import react.RFuture;

import playn.robovm.RoboPlatform;

public class RoboFacebook implements Facebook {

  public RoboFacebook (RoboPlatform platform) {
    _platform = platform;
    _loginMgr.setDefaultAudience(FBSDKDefaultAudience.Everyone);
  }

  public boolean handleOpenURL (NSURL url) {
    // var session = FBSession.ActiveSession;
    // return (session == null) ? false : session.HandleOpenURL(url);
    return false;
  }

  public void onActivated () {
    // // We need to properly handle activation of the application with regards to SSO (e.g.,
    // // returning from iOS 6.0 authorization dialog or from fast app switching).
    // FBSession.ActiveSession.HandleDidBecomeActive();
  }

  public String accessToken () {
    FBSDKAccessToken token = FBSDKAccessToken.getCurrentAccessToken();
    // TODO: expiration shenanigans?
    return (token == null) ? null : token.getTokenString();
  }

  // from Facebook interface
  public boolean isAuthed () {
    return accessToken() != null;
  }

  // from Facebook interface
  public RFuture<String> authenticate (boolean forceReauth) {
    final DeferredPromise<String> result = new DeferredPromise<String>();
    if (!forceReauth && accessToken() != null) result.succeed(accessToken());
    else _loginMgr.logInWithReadPermissions(
      Arrays.asList("email"), new VoidBlock2<FBSDKLoginManagerLoginResult, NSError>() {
        @Override public void invoke (FBSDKLoginManagerLoginResult res, NSError err) {
          if (err != null) result.fail(toExn(err));
          else if (res.isCancelled()) result.fail(new Exception("Login canceled."));
          else result.succeed(accessToken());
        }
      });
    return result;
  }

  public void deauthorize () {
    // var session = FBSession.ActiveSession;
    // if (session != null) session.CloseAndClearTokenInformation();
  }

  // from Facebook interface
  public RFuture<String> shareGotCard (
    String name, String descrip, String image, String link,
    String category, String series, String tgtFriendId, String refid) {

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

  public RFuture<String> showDialog (String name, String caption, String descrip, String picURL,
                                     String link, String tgtFriendId, String refid) {
    // return withSession(false, delegate (Callback cb) {
    //   var paramz = toDict("name", name, "caption", caption, "description", descrip,
    //                       "picture", picURL, "link", link, "ref", refid);
    //   if (tgtFriendId != null) addToDict(paramz, "to", tgtFriendId);
    //   FBWebDialogs.PresentFeedDialogModally(null, paramz, handler(cb));
    // });
    return null;
  }

  private Exception toExn (NSError err) {
    return new Exception(err.getLocalizedDescription());
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
  //       addToDict(dict, paramz[ii], paramz[ii+1]);
  //     }
  //   }
  //   // addToDict(dict, "show_error", "true");
  //   return dict;
  // }

  // protected static void addToDict (NSMutableDictionary dict, string key, string value) {
  //   dict.Add(new NSString(key), new NSString(value));
  // }

  private final RoboPlatform _platform;
  private final FBSDKLoginManager _loginMgr = new FBSDKLoginManager();
}
