//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.foundation.NSDate;
import org.robovm.apple.foundation.NSDateFormatter;
import org.robovm.apple.foundation.NSDateFormatterStyle;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSTimeZone;
import org.robovm.apple.storekit.*;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UILocalNotification;

import static playn.core.PlayN.log;
import playn.core.util.Callback;
import react.RFuture;

import com.threerings.everything.rpc.EveryAPI;

public class IOSDevice implements Device {

  public IOSDevice (UIApplication app) {
    _app = app;
    _fmt.setDateStyle(NSDateFormatterStyle.Medium);
  }

  public void init (Everything game) {
    // keep the _paymentObs reference to avoid premature garbage collection
    SKPaymentQueue.getDefaultQueue().addTransactionObserver(_paymentObs = new PaymentObserver(game));
  }

  public float statusBarHeight () {
    return (float)_app.getStatusBarFrame().getHeight();
  }

  public int timeZoneOffset () {
    // iOS gives us number of seconds to add to GMT to get local timezone, we want to return number
    // of minutes to subtract from GMT to get local TZ (thank JavaScript for this retardedness)
    return (int)NSTimeZone.getLocalTimeZone().getSecondsFromGMT() / -60;
  }

  public String formatDate (long when) {
    return _fmt.format(new NSDate(when/1000d));
  }

  public int hourOfDay () {
    return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
  }

  public void scheduleGridNotification (long when) {
    _app.cancelAllLocalNotifications();
    _app.setApplicationIconBadgeNumber(0);
    UILocalNotification note = new UILocalNotification();
    note.setFireDate(new NSDate(when/1000d));
    note.setTimeZone(NSTimeZone.getLocalTimeZone());
    note.setApplicationIconBadgeNumber(1);
    note.setAlertBody("A fresh new grid is ready for flipping!");
    _app.scheduleLocalNotification(note);
    log().info("Scheduled new grid note for " + note.getFireDate() + ".");
  }

  public RFuture<Product[]> getProducts () {
    DeferredPromise<Product[]> result = new DeferredPromise<Product[]>();
    _prodsReq = new SKProductsRequest(new HashSet<String>(Arrays.asList(Product.skus())));
    _prodsReq.setDelegate(new ProductFetcher(result));
    _prodsReq.start();
    return result;
  }

  public RFuture<Void> buyProduct (Everything game, String sku) {
    return _paymentObs.buyProduct(sku);
  }

  private static class ProductFetcher extends SKProductsRequestDelegateAdapter {
    public ProductFetcher (Callback<Product[]> cb) {
      _cb = cb;
    }
    @Override public void didReceiveResponse (SKProductsRequest req, SKProductsResponse rsp) {
      Product[] products = new Product[rsp.getProducts().size()];
      int ii = 0;
      for (SKProduct prod : rsp.getProducts()) {
        products[ii++] = Product.apply(prod.getProductIdentifier(),
                                       prod.getPriceLocale().getCurrencySymbol() + prod.getPrice());
      }
      _cb.onSuccess(products);
    }
    @Override public void didFail (SKRequest req, NSError error) {
      _cb.onFailure(new Exception(error.getLocalizedDescription()));
    }
    protected final Callback<Product[]> _cb;
  }

  private static class PaymentObserver extends SKPaymentTransactionObserverAdapter {
    public PaymentObserver (Everything game) {
      _game = game;
    }

    public RFuture<Void> buyProduct (final String sku) {
      log().info("Initiating IAP [prodId=" + sku + "]");
      DeferredPromise<Void> result = new DeferredPromise<Void>();
      SKProductsRequest preq = new SKProductsRequest(Collections.singleton(sku));
      preq.setDelegate(new SKProductsRequestDelegateAdapter() {
        @Override public void didFail (SKRequest req, NSError error) {
          log().info("IAP prod request failed [prodId=" + sku + ", error=" + error + "]");
        }
        @Override public void didReceiveResponse (SKProductsRequest req, SKProductsResponse rsp) {
          for (SKProduct skp : rsp.getProducts()) {
            SKPaymentQueue.getDefaultQueue().addPayment(new SKPayment(skp));
          }
        }
      });;
      // SKPaymentQueue.getDefaultQueue().addPayment(SKPayment.createFromProductIdentifier(sku));
      _penders.put(sku, result);
      return result;
    }

    @Override public void updatedTransactions (SKPaymentQueue queue, NSArray<SKPaymentTransaction> txns) {
      // log().info("Got updated transactions", "count", txns.Length);
      for (SKPaymentTransaction txn : txns) {
        String sku = txn.getPayment().getProductIdentifier();
        switch (txn.getTransactionState()) {
        case Purchased: {
          // if we have a pending promise relating to this txn, let it know that all is well
          Callback<Void> pender = _penders.remove(sku);
          if (pender != null) pender.onSuccess(null);
          String pf = EveryAPI.PF_APPSTORE;
          // notify the game that the purchase succeeded; it will then validate the receipt with
          // Apple's servers, and call back to our PurchaseFinisher
          String rcpt = txn.getTransactionReceipt().toString();
          _game.redeemPurchase(sku, pf, txn.getTransactionIdentifier(), rcpt, txn,
                               new Callback<SKPaymentTransaction>() {
            public void onSuccess (SKPaymentTransaction txn) {
              // let iOS know that this transaction has been put to bed
              SKPaymentQueue.getDefaultQueue().finishTransaction(txn);
            }
            // nada; other systems will have logged this error
            public void onFailure (Throwable error) {}
          });
          break;
        }

        case Failed: {
          int ecode = (int)txn.getError().getCode();
          if (ecode != 2) {
            log().info("Purchase failed [prodId=" + txn.getPayment().getProductIdentifier() +
                       ", errcode=" + ecode + "]");
          }
          // if we have a pending promise relating to this txn, let it know that we're hosed
          Callback<Void> pender = _penders.remove(sku);
          if (pender != null) {
            if (ecode == 2) {
              pender.onFailure(new Exception("Purchase canceled."));
            } else {
              pender.onFailure(new Exception(txn.getError().getLocalizedDescription()));
            }
          }
          // clear the txn immediately, there's no use in keeping it around
          SKPaymentQueue.getDefaultQueue().finishTransaction(txn);
          break;
        }

        case Restored:
          break; // we don't restore purchases, so nothing doing

        case Purchasing:
          break; // nothing to see here, move it along

        default:
          log().warn("Unknown txn state? [state=" + txn.getTransactionState() +
                     ", txn=" + txn + "]");
          break;
        }
      }
    }

    protected final Everything _game;
    protected final Map<String,Callback<Void>> _penders = new HashMap<String,Callback<Void>>();
  }

  protected final UIApplication _app;
  protected final NSDateFormatter _fmt = new NSDateFormatter();
  private PaymentObserver _paymentObs;
  private SKProductsRequest _prodsReq; // retain to avoid GC
}
