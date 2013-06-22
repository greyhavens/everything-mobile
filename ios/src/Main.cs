using System;
using MonoTouch.Foundation;
using MonoTouch.UIKit;

using playn.core;
using playn.ios;
using react;

namespace everything
{
  [Register ("AppDelegate")]
  public partial class AppDelegate : IOSApplicationDelegate {

    override public bool FinishedLaunching (UIApplication app, NSDictionary options) {
      app.SetStatusBarHidden(true, true);
      var pconfig = new IOSPlatform.Config();
      pconfig.iPadLikePhone = true;
      if (_hack) preventStripping();
      IOSPlatform.register(app, pconfig);
      PlayN.run(new Everything(new IOSDevice(), _facebook));
      return true;
    }

    override public bool OpenUrl (UIApplication app, NSUrl url, string sourceApp, NSObject anno) {
      _facebook.handleOpenURL(url);
      return true;
    }

    // this is never executed, but forces MonoTouch to avoid stripping these reflected types and
    // methods; configuring the linker via its XML file was an undocumented unworking mess
    private void preventStripping () {
      var list = new System.Collections.ArrayList();
      list.Add(new sun.util.resources.CalendarData());
      list.Add(new sun.text.resources.FormatData());
      Console.WriteLine(list);
    }
    private bool _hack = false;

    private IOSFacebook _facebook = new IOSFacebook();
  }

  public class IOSDevice : Device {
    public int timeZoneOffset () {
      return 0; // TODO
    }
  }

  public class Application {
    static void Main (string[] args) {
      UIApplication.Main (args, null, "AppDelegate");
    }
  }
}
