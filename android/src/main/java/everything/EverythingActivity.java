//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.android.vending.billing.IInAppBillingService;

import playn.android.AndroidPlatform;
import playn.android.GameActivity;
import playn.core.Font;
import playn.core.Json;
import playn.core.PlayN;
import playn.core.util.Callback;
import react.RFuture;
import react.RPromise;
import react.Slot;

import com.threerings.everything.data.SessionData;
import com.threerings.everything.rpc.EveryAPI;
import com.threerings.everything.R;

public class EverythingActivity extends GameActivity {

    public static class NoteReceiver extends BroadcastReceiver {
        @Override public void onReceive (Context ctx, Intent intent) {
            Bitmap icon = BitmapFactory.decodeStream(
                ctx.getResources().openRawResource(R.drawable.icon));
            Intent onTap = ctx.getPackageManager().getLaunchIntentForPackage(
                "com.threerings.everything");
            onTap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Notification note = new NotificationCompat.Builder(ctx).
                setLargeIcon(icon).
                setSmallIcon(R.drawable.noteicon).
                setContentTitle("The Everything Game").
                setContentText("A fresh new grid is ready for flipping!").
                setContentIntent(PendingIntent.getActivity(ctx, 0, onTap, 0)).
                setAutoCancel(true).
                getNotification();
            ((NotificationManager)ctx.getSystemService(NOTIFICATION_SERVICE)).notify(1, note);
        }
    }

    public final Device device = new Device() {
        public float statusBarHeight () { return 0; }

        public int timeZoneOffset () {
            TimeZone tz = TimeZone.getDefault();
            // Java returns millis to add to GMT, we want minutes to subtract from GMT
            return -tz.getOffset(System.currentTimeMillis())/MillisPerMinute;
        }

        public String formatDate (long when) {
            return _dfmt.format(new Date(when));
        }
        private DateFormat _dfmt = DateFormat.getDateInstance();

        public int hourOfDay () {
            return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        }

        public void scheduleGridNotification (long when) {
            Intent intent = new Intent(EverythingActivity.this, NoteReceiver.class);
            ((AlarmManager)getSystemService(ALARM_SERVICE)).set(
                AlarmManager.RTC_WAKEUP, when,
                PendingIntent.getBroadcast(
                    EverythingActivity.this, 3771, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        }

        public RFuture<Product[]> getProducts () {
            final DeferredPromise<Product[]> result = new DeferredPromise<Product[]>();
            platform().invokeAsync(new Runnable() {
                public void run () { resolveProducts(result); }
            });
            return result;
        }

        public RFuture<Void> buyProduct (Everything game, String sku) {
            RPromise<Void> result = RPromise.create();
            String devtok = sku + ":" + System.currentTimeMillis();
            try {
                Bundle buyIntentBundle = billSvc.getBuyIntent(
                    3, getPackageName(), sku, "inapp", devtok);
                int rc = buyIntentBundle.getInt("RESPONSE_CODE");
                if (rc == 0) {
                    platform().log().info("Starting purchase intent for " + sku);
                    PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                    startIntentSenderForResult(
                        pendingIntent.getIntentSender(), BuyCode, new Intent(), 0, 0, 0);
                } else {
                    platform().log().warn("getBuyIntent(" + sku + ") failed: " + rc);
                    result.fail(new Exception("Android Billing error " + rc));
                }
            } catch (IntentSender.SendIntentException sie) {
                platform().log().warn("getBuyIntent(" + sku + ") failed", sie);
                result.fail(sie);
            } catch (RemoteException re) {
                platform().log().warn("getBuyIntent(" + sku + ") failed", re);
                result.fail(re);
            }
            return result;
        }
    };

    public final DroidBook facebook = new DroidBook(this);
    public Everything game;

    // android billing stuffs; yay for mutability!
    public IInAppBillingService billSvc;
    public ServiceConnection billConn = new ServiceConnection() {
        public void onServiceDisconnected (ComponentName name) {
            billSvc = null;
        }
        public void onServiceConnected (ComponentName name, IBinder service) {
            billSvc = IInAppBillingService.Stub.asInterface(service);
            maybeRedeemPurchases();
        }
    };

    @Override public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        facebook.onCreate(savedInstanceState);
        bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"),
                    billConn, Context.BIND_AUTO_CREATE);
    }

    @Override public void onPause () {
        super.onPause();
        facebook.onPause();
    }

    @Override public void onResume () {
        super.onResume();
        facebook.onResume();
    }

    @Override public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        facebook.onSaveInstanceState(outState);
    }

    @Override public void onDestroy () {
        super.onDestroy();
        facebook.onDestroy();
        if (billConn != null) {
            unbindService(billConn);
            billConn = null;
        }
    }

    @Override public void onActivityResult (int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        switch (reqCode) {
        case BuyCode:
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0); // TODO ???
            if (resCode == Activity.RESULT_OK) {
                Purchase p = toPurchase(data.getStringExtra("INAPP_PURCHASE_DATA"),
                                        data.getStringExtra("INAPP_DATA_SIGNATURE"));
                if (p != null) redeemPurchase("bought", p);
            } else { // TODO: do we need to inform the user when billing unavailable?
                platform().log().info("Non-success purchase result: " + resCode);
            }
            break;

        default:
            facebook.onActivityResult(reqCode, resCode, data);
            break;
        }
    }

    @Override public void onBackPressed () {
        // only allow BACK to exit the app if we're on the main menu screen (and not currently
        // switching between screens)
        if (!game.screens().isTransiting() && (game.screens().top() instanceof MainMenuScreen)) {
            super.onBackPressed();
        }
    }

    @Override public void main () {
        // default to smoothing when rendering canvas images
        platform().graphics().setCanvasFilterBitmaps(true);
        // we have only @2x resources, so use those
        platform().assets().setAssetScale(2);
        // register our custom fonts
        platform().graphics().registerFont("fonts/copper.ttf", "CopperplateGothic-Bold",
                                           Font.Style.PLAIN);
        platform().graphics().registerFont(Typeface.SERIF, "Copperplate", Font.Style.BOLD);
        platform().graphics().registerFont("fonts/treasure.ttf", "Treasure Map Deadhand",
                                           Font.Style.PLAIN);
        platform().graphics().registerFont("fonts/josschrift.ttf", "Josschrift", Font.Style.PLAIN);
        // start the ball rolling
        PlayN.run(game = new Everything(false, device, facebook));
        // re-redeem any lingering unconsumed purchases when our session is refreshed
        game.sess().connectNotify(new Slot<SessionData>() {
            public void onEmit (SessionData sess) { maybeRedeemPurchases(); }
        });

    }

    @Override protected AndroidPlatform platform () {
        return super.platform(); // make visible to friends
    }

    @Override protected boolean usePortraitOrientation () { return true; }
    @Override protected String logIdent () { return "every"; }
    @Override protected int makeWindowFlags () { // we want the status bar to remain visible
        return super.makeWindowFlags() & ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
    }

    @Override protected float scaleFactor () {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int dwidth = dm.widthPixels, dheight = dm.heightPixels;
        // we may be in landscape right now, because Android is fucking retarded
        int width, height;
        if (dwidth > dheight) {
            width = dheight;
            height = dwidth;
        } else {
            width = dwidth;
            height = dheight;
        }
        return Math.min(width / 320f, height / 480f);
    }

    protected void maybeRedeemPurchases () {
        if (game == null || game.sess().get() == null || billSvc == null) return;
        for (Purchase purch : readPurchases(null)) redeemPurchase("linger", purch);
    }

    protected void redeemPurchase (String source, Purchase purch) {
        platform().log().info("Redeeming [" + source + "] purchase " + purch);
        game.redeemPurchase(purch.sku, EveryAPI.PF_PLAYSTORE, purch.orderId, purch.receipt, purch,
                            new Callback<Purchase>() {
                                public void onSuccess (Purchase purch) { consumePurchase(purch); }
                                public void onFailure (Throwable exn) {} // nada
                            });
    }

    protected void consumePurchase (final Purchase purch) {
        platform().log().info("Consuming purchase " + purch);
        platform().invokeAsync(new Runnable() { public void run () {
            try {
                int rv = billSvc.consumePurchase(3, getPackageName(), purch.purchaseToken);
                if (rv != 0) platform().log().warn(
                    "consumePurchase(" + purch.orderId + ") fail: " + rv);
            } catch (RemoteException re) {
                platform().log().warn("consumePurchase(" + purch.orderId + ") fail", re);
            }
        }});
    }

    protected void resolveProducts (Callback<Product[]> cb) {
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(
            "ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(Product.skus())));
        try {
            Bundle skuDetails = billSvc.getSkuDetails(3, getPackageName(), "inapp", querySkus);
            int rc = skuDetails.getInt("RESPONSE_CODE");
            if (rc != 0) cb.onFailure(new Exception("Android billing request failed: " + rc));
            else {
                List<Product> prods = new ArrayList<Product>();
                for (String deets : skuDetails.getStringArrayList("DETAILS_LIST")) {
                    Product prod = toProduct(deets);
                    if (prod != null) prods.add(prod);
                }
                cb.onSuccess(prods.toArray(new Product[prods.size()]));
            }
        } catch (RemoteException re) {
            platform().log().warn("resolveProducts() failed", re);
            cb.onFailure(re);
        }
    }

    protected List<Purchase> toPurchases (List<String> data, List<String> sigs) {
        platform().log().info("Read purchases " + data + " " + sigs);
        if (data == null || data.size() == 0) return Collections.<Purchase>emptyList();
        else {
            ArrayList<Purchase> ps = new ArrayList<Purchase>();
            for (int ii = 0; ii < data.size(); ii++) {
                Purchase p = toPurchase(data.get(ii), sigs.get(ii));
                if (p != null) ps.add(p);
            }
            return ps;
        }
    }

    protected List<Purchase> readPurchases (String conToken) {
        List<Purchase> ps = Collections.emptyList();
        try {
            Bundle ownedItems = billSvc.getPurchases(3, getPackageName(), "inapp", conToken);
            int rc = ownedItems.getInt("RESPONSE_CODE");
            if (rc != 0) platform().log().warn("getPurchases(" + conToken + ") failure: " + rc);
            else ps = toPurchases(ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST"),
                                  ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST"));
            String nextToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");
            if (nextToken != null) ps.addAll(readPurchases(nextToken));
        } catch (RemoteException re) {
            platform().log().warn("getPurchases(" + conToken + ") failure", re);
        }
        return ps;
    }

    public class Purchase {
        public final String sku;
        public final String orderId;
        public final String purchaseToken;
        public final String receipt;

        public Purchase (String sku, String orderId, String purchaseToken, String receipt) {
            this.sku = sku;
            this.orderId = orderId;
            this.purchaseToken = purchaseToken;
            this.receipt = receipt;
        }
    }

    protected Purchase toPurchase (String json, String sig) {
        try {
            Json.Object obj = platform().json().parse(json);
            return new Purchase(obj.getString("productId"), obj.getString("orderId"),
                                obj.getString("purchaseToken"), sig + "\n" + json);
        } catch (Exception e) {
            platform().log().warn("toPurchase(" + json + ", " + sig + ") failure", e);
            return null;
        }
    }

    protected Product toProduct (String json) {
        try {
            Json.Object obj = platform().json().parse(json);
            return Product.apply(obj.getString("productId"), obj.getString("price"));
        } catch (Exception e) {
            platform().log().warn("toProduct(" + json + ") fail", e);
            return null;
        }
    }

    private static final int MillisPerMinute = 1000*60;
    private static final int BuyCode = 3773;
}
