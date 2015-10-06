//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSPropertyList;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.uikit.*;
import org.robovm.pods.facebook.core.FBSDKAppEvents;
import org.robovm.pods.facebook.core.FBSDKApplicationDelegate;

import playn.robovm.RoboPlatform;
import playn.robovm.RoboViewController;

public class EverythingRoboVM extends UIApplicationDelegateAdapter {

  @Override
  public boolean didFinishLaunching (UIApplication app, UIApplicationLaunchOptions launchOpts) {
    CGRect bounds = UIScreen.getMainScreen().getBounds();
    UIWindow window = new UIWindow(bounds);

    RoboPlatform.Config config = new RoboPlatform.Config();
    config.iPadLikePhone = true;
    RoboViewController ctrl = new RoboViewController(window.getBounds(), config);
    window.setRootViewController(ctrl);
    final RoboPlatform pf = ctrl.platform();

    _facebook = new RoboFacebook(pf);
    FBSDKApplicationDelegate.getSharedInstance().didFinishLaunching(app, launchOpts);

    IOSDevice device = new IOSDevice(app);
    Everything game = new Everything(false, device, _facebook);
    device.init(game);
    pf.run(game);

    window.makeKeyAndVisible();
    addStrongRef(window);
    return true;
  }

  @Override public void didBecomeActive (UIApplication application) {
    FBSDKAppEvents.activateApp();
  }

  @Override public boolean openURL (UIApplication app, NSURL url, String sourceApp,
                                    NSPropertyList anno) {
    return FBSDKApplicationDelegate.getSharedInstance().openURL(app, url, sourceApp, anno);
  }

  public static void main (String[] args) {
    NSAutoreleasePool pool = new NSAutoreleasePool();
    UIApplication.main(args, null, EverythingRoboVM.class);
    pool.close();
  }

  private RoboFacebook _facebook;
}
