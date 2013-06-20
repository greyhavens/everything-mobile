using System;
using MonoTouch.Foundation;
using MonoTouch.UIKit;

using playn.ios;
using playn.core;
using react;

namespace everything
{
  [Register ("AppDelegate")]
  public partial class AppDelegate : IOSApplicationDelegate {
    public override bool FinishedLaunching (UIApplication app, NSDictionary options) {
      app.SetStatusBarHidden(true, true);
      var pconfig = new IOSPlatform.Config();
      // use pconfig to customize iOS platform, if needed
      IOSPlatform.register(app, pconfig);
      PlayN.run(new Everything(new IOSDevice(), new IOSFacebook()));
      return true;
    }
  }

  public class IOSDevice : Device {
    public int timeZoneOffset () {
      return 0; // TODO
    }
  }

  public class IOSFacebook : Facebook {
    public string userId () {
      return "540615819"; // TODO
    }
    public string authToken () {
      return "todo"; // TODO
    }
    public RFuture authenticate () {
      return RFuture.success(userId());
    }
  }

  public class Application {
    static void Main (string[] args) {
      UIApplication.Main (args, null, "AppDelegate");
    }
  }
}
