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

  // from Facebook interface
  public boolean isAuthed () {
    return accessToken() != null;
  }

  // from Facebook interface
  public RFuture<String> authenticate (boolean forceReauth) {
    final DeferredPromise<String> result = new DeferredPromise<String>();
    if (!forceReauth && accessToken() != null) result.succeed(accessToken());
    else {
      if (forceReauth) _loginMgr.logOut();
      _loginMgr.logInWithReadPermissions(
        Arrays.asList("email"), new VoidBlock2<FBSDKLoginManagerLoginResult, NSError>() {
          @Override public void invoke (FBSDKLoginManagerLoginResult res, NSError err) {
            if (err != null) result.onFailure(toExn(err));
            else if (res.isCancelled()) result.onFailure(new Exception("Login canceled."));
            else result.onSuccess(accessToken());
          }
        });
    }
    return result;
  }

  public void deauthorize () {
    _loginMgr.logOut();
  }

  private String accessToken () {
    FBSDKAccessToken token = FBSDKAccessToken.getCurrentAccessToken();
    // TODO: expiration shenanigans?
    return (token == null) ? null : token.getTokenString();
  }

  private Exception toExn (NSError err) {
    return new Exception(err.getLocalizedDescription());
  }

  private final RoboPlatform _platform;
  private final FBSDKLoginManager _loginMgr = new FBSDKLoginManager();
}
