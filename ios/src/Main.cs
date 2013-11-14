using System;
using System.Collections.Generic;
using MonoTouch.Foundation;
using MonoTouch.UIKit;
using MonoTouch.StoreKit;

using playn.core;
using playn.core.util;
using playn.ios;
using react;
using com.threerings.everything.rpc;

namespace everything
{
  [Register ("AppDelegate")]
  public partial class AppDelegate : IOSApplicationDelegate {

    override public bool FinishedLaunching (UIApplication app, NSDictionary options) {
      var pconfig = new IOSPlatform.Config();
      pconfig.iPadLikePhone = true;
      if (_hack) preventStripping();
      var p = IOSPlatform.register(app, pconfig);
      p.rootViewController().WantsFullScreenLayout = true;
      // blah; some fiddling needed to tie a gordian knot
      var device = new IOSDevice(app);
      var game = new Everything(false, device, _facebook);
      device.init(game);
      PlayN.run(game);
      return true;
    }

    override public bool OpenUrl (UIApplication app, NSUrl url, string sourceApp, NSObject anno) {
      _facebook.handleOpenURL(url);
      return true;
    }

    override public void OnActivated (UIApplication app) {
      base.OnActivated(app);
      _facebook.onActivated();
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
    public IOSDevice (UIApplication app) {
      _app = app;
      _fmt.DateStyle = NSDateFormatterStyle.Medium;
    }

    public void init (Everything game) {
      // keep the _paymentObs reference to avoid premature garbage collection
      SKPaymentQueue.DefaultQueue.AddTransactionObserver(_paymentObs = new PaymentObserver(game));
    }

    public float statusBarHeight () {
      return _app.StatusBarFrame.Height;
    }

    public int timeZoneOffset () {
      // iOS gives us number of seconds to add to GMT to get local timezone, we want to return
      // number of minutes to subtract from GMT to get local timezone (thank JavaScript for this
      // retardedness)
      return NSTimeZone.LocalTimeZone.GetSecondsFromGMT / -60;
    }

    public string formatDate (long when) {
      return _fmt.ToString(NSDate.FromTimeIntervalSince1970(when/1000d));
    }

    public int hourOfDay () {
      return DateTime.Now.Hour;
    }

    public RFuture getProducts () {
      var result = new DeferredPromise();

      var array = new NSString[Product.skus().Length];
      for (var ii = 0; ii < array.Length; ii++) {
        array[ii] = new NSString(Product.skus()[ii]);
      }
      NSSet prodIds = NSSet.MakeNSObjectSet<NSString>(array);
      _prodsReq  = new SKProductsRequest(prodIds);
      _prodsReq.Delegate = new ProductFetcher(result);
      _prodsReq.Start();

      return result;
    }

    public RFuture buyProduct (Everything game, string sku) {
      return _paymentObs.buyProduct(sku);
    }

    protected readonly UIApplication _app;
    protected readonly NSDateFormatter _fmt = new NSDateFormatter();
    private PaymentObserver _paymentObs;
    private SKProductsRequest _prodsReq; // retain to avoid GC
  }

  internal class ProductFetcher : SKProductsRequestDelegate {
    public ProductFetcher (Callback/*Product[]*/ cb) {
      _cb = cb;
    }

    public override void ReceivedResponse (SKProductsRequest req, SKProductsResponse rsp) {
      Product[] products = new Product[rsp.Products.Length];
      for (var ii = 0; ii < products.Length; ii++) {
        var prod = rsp.Products[ii];
        products[ii] = Product.apply(prod.ProductIdentifier,
                                     prod.PriceLocale.CurrencySymbol + prod.Price);
      }
      _cb.onSuccess(products);
    }

    public override void RequestFailed (SKRequest req, NSError error) {
      _cb.onFailure(new Exception(error.LocalizedDescription));
    }

    protected readonly Callback/*Product[]*/ _cb;
  }

  internal class PaymentObserver : SKPaymentTransactionObserver {
    public PaymentObserver (Everything game) {
      _game = game;
    }

    public RFuture buyProduct (string sku) {
      PlayN.log().info("Initiating IAP [prodId=" + sku + "]");
      var result = new DeferredPromise();
      SKPaymentQueue.DefaultQueue.AddPayment(SKPayment.PaymentWithProduct(sku));
      _penders.Add(sku, result);
      return result;
    }

    override public void UpdatedTransactions (SKPaymentQueue queue, SKPaymentTransaction[] txns) {
      // PlayN.log().info("Got updated transactions", "count", txns.Length);
      foreach (SKPaymentTransaction txn in txns) {
        string sku = txn.Payment.ProductIdentifier;
        switch (txn.TransactionState) {
        case SKPaymentTransactionState.Purchased: {
          // if we have a pending promise relating to this txn, let it know that all is well
          Callback pender;
          if (_penders.TryGetValue(sku, out pender)) {
            _penders.Remove(sku);
            pender.onSuccess(null);
          }
          var pf = "APPSTORE"; // EveryAPI.PF_APPSTORE not visible due to Java->C# blah blah
          // notify the game that the purchase succeeded; it will then validate the receipt with
          // Apple's servers, and call back to our PurchaseFinisher
          _game.redeemPurchase(sku, pf, txn.TransactionIdentifier,
                               txn.TransactionReceipt.ToString(), txn, new PurchaseFinisher());
          break;
        }

        case SKPaymentTransactionState.Failed: {
          if (txn.Error.Code != 2) {
            PlayN.log().info("Purchase failed [prodId=" + txn.Payment.ProductIdentifier +
                             ", errcode=" + txn.Error.Code + "]");
          }
          // if we have a pending promise relating to this txn, let it know that we're hosed
          Callback pender;
          if (_penders.TryGetValue(sku, out pender)) {
            _penders.Remove(sku);
            if (txn.Error.Code == 2) {
              pender.onFailure(new Exception("Purchase canceled."));
            } else {
              pender.onFailure(new Exception(txn.Error.LocalizedDescription));
            }
          }
          // clear the txn immediately, there's no use in keeping it around
          SKPaymentQueue.DefaultQueue.FinishTransaction(txn);
          break;
        }

        case SKPaymentTransactionState.Restored:
          break; // we don't restore purchases, so nothing doing

        case SKPaymentTransactionState.Purchasing:
          break; // nothing to see here, move it along

        default:
          PlayN.log().warn("Unknown txn state? [state=" + txn.TransactionState +
                           ", txn=" + txn + "]");
          break;
        }
      }
    }

    protected readonly Everything _game;
    protected readonly Dictionary<string,Callback> _penders = new Dictionary<string,Callback>();
  }

  internal class PurchaseFinisher : Callback/*SKPaymentTransaction*/ {
    public void onSuccess (Object txn) {
      // let iOS know that this transaction has been put to bed
      SKPaymentQueue.DefaultQueue.FinishTransaction((SKPaymentTransaction)txn);
    }
    public void onFailure (Exception error) {
      // nada; other systems will have logged this error
    }
  }

  public class Application {
    static void Main (string[] args) {
      UIApplication.Main (args, null, "AppDelegate");
    }
  }
}
