//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import java.text.DateFormat
import java.util.{Arrays, ArrayList, Date, TimeZone}

import scala.collection.JavaConversions._

import android.app.Activity
import android.app.PendingIntent
import android.content.{Context, ComponentName, Intent, ServiceConnection}
import android.graphics.Typeface
import android.os.{Bundle, IBinder}
import android.view.WindowManager

import com.android.vending.billing.IInAppBillingService

import playn.android.GameActivity
import playn.core.{Font, PlayN}
import react.{RFuture, RPromise, Slot}

import com.threerings.everything.data.SessionData
import com.threerings.everything.rpc.EveryAPI

class EverythingActivity extends GameActivity {

  val device = new Device {
    def statusBarHeight = 0
    def timeZoneOffset = {
      val tz = TimeZone.getDefault
      // Java returns millis to add to GMT, we want minutes to subtract from GMT
      -tz.getOffset(System.currentTimeMillis)/MillisPerMinute
    }
    def formatDate (when :Long) = _dfmt.format(new Date(when))
    private val _dfmt = DateFormat.getDateInstance()

    def getProducts = {
      val result = RPromise.create[Seq[Product]]()
      platform.invokeAsync(new Runnable() {
        def run = resolveProducts(result)
      })
      result
    }

    def buyProduct (game :Everything, sku :String) = {
      val result = RPromise.create[Unit]()
      val devtok = sku + ":" + System.currentTimeMillis
      val buyIntentBundle = billSvc.getBuyIntent(3, getPackageName, sku, "inapp", devtok)
      buyIntentBundle.getInt("RESPONSE_CODE") match {
        case 0 =>
          platform.log.info(s"Starting purchase intent for $sku")
          val pendingIntent = buyIntentBundle.getParcelable[PendingIntent]("BUY_INTENT")
          startIntentSenderForResult(pendingIntent.getIntentSender, BuyCode, new Intent(), 0, 0, 0)
        case code =>
          platform.log.warn(s"getBuyIntent($sku) failed: $code")
          result.fail(new Exception(s"Android Billing error $code"))
      }
      result
    }

    def purchaseRedeemed (sku :String, orderId :String) {
      val purchs = readPurchases(null)
      purchs find(_.orderId == orderId) match {
        case Some(purch) =>
          platform.log.info(s"Consuming purchase $purch")
          platform.invokeAsync(new Runnable() {
            def run = {
              val rv = billSvc.consumePurchase(3, getPackageName, purch.purchaseToken)
              if (rv != 0) platform.log.warn(s"consumePurchase($orderId) fail: $rv")
            }
          })
        case None =>
          platform.log.warn(s"consumePurchase($orderId) found no such order [have=$purchs]")
      }
    }
  }

  val facebook = new DroidBook(this)
  lazy val game = new Everything(false, device, facebook)

  // android billing stuffs; yay for mutability!
  var billSvc :IInAppBillingService = _
  var billConn = new ServiceConnection() {
    override def onServiceDisconnected (name :ComponentName) {
      billSvc = null
    }
    override def onServiceConnected (name :ComponentName, service :IBinder) {
      billSvc = IInAppBillingService.Stub.asInterface(service)
      // re-redeem any lingering unconsumed purchases when our session is refreshed
      game.sess.connectNotify(new Slot[SessionData]() {
        def onEmit (sess :SessionData) =
          if (sess != null) readPurchases(null) foreach redeemPurchase("linger")
      })
    }
  }

  override def onCreate (savedInstanceState :Bundle) {
    super.onCreate(savedInstanceState)
    facebook.onCreate()
    bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"),
                billConn, Context.BIND_AUTO_CREATE);
  }

  override def onDestroy () {
    super.onDestroy()
    if (billConn != null) {
      unbindService(billConn)
      billConn = null
    }
  }

  override def onActivityResult (requestCode :Int, resultCode :Int, data :Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    requestCode match {
      case BuyCode =>
        val responseCode = data.getIntExtra("RESPONSE_CODE", 0) // TODO ???
        resultCode match {
          case Activity.RESULT_OK =>
            toPurchase(data.getStringExtra("INAPP_PURCHASE_DATA"),
                       data.getStringExtra("INAPP_DATA_SIGNATURE")) foreach redeemPurchase("bought")
          case _ => // TODO: do we need to inform the user when billing unavailable?
            platform.log.info(s"Non-success purchase result: $resultCode")
        }

      case _ =>
        facebook.onActivityResult(requestCode, resultCode, data)
    }
  }

  override def main () {
    // default to smoothing when rendering canvas images
    platform.graphics.setCanvasFilterBitmaps(true)
    // we have only @2x resources, so use those
    platform.assets.setAssetScale(2)
    // register our custom fonts
    platform.graphics.registerFont("fonts/copper.ttf", "CopperplateGothic-Bold", Font.Style.PLAIN)
    platform.graphics.registerFont(Typeface.SERIF, "Copperplate", Font.Style.BOLD)
    platform.graphics.registerFont("fonts/treasure.ttf", "Treasure Map Deadhand", Font.Style.PLAIN)
    platform.graphics.registerFont("fonts/josschrift.ttf", "Josschrift", Font.Style.PLAIN)
    // start the ball rolling
    PlayN.run(game)
  }

  override def platform = super.platform // make visible to friends
  override def usePortraitOrientation = true
  override def logIdent = "every"
  override def makeWindowFlags = // we want the status bar to remain visible
    super.makeWindowFlags & ~WindowManager.LayoutParams.FLAG_FULLSCREEN

  protected def redeemPurchase (source :String)(purch :Purchase) {
    platform.log.info(s"Redeeming [$source] purchase $purch")
    game.redeemPurchase(purch.sku, EveryAPI.PF_PLAYSTORE, purch.orderId, purch.receipt)
  }

  protected def resolveProducts (result :RPromise[Seq[Product]]) {
    val querySkus = new Bundle()
    querySkus.putStringArrayList("ITEM_ID_LIST", new ArrayList(Arrays.asList(Product.skus :_*)))
    val skuDetails = billSvc.getSkuDetails(3, getPackageName, "inapp", querySkus);
    skuDetails.getInt("RESPONSE_CODE") match {
      case 0 =>
        result.succeed((skuDetails.getStringArrayList("DETAILS_LIST") map toProduct).flatten)
      case code =>
        result.fail(new Exception(s"Android billing request failed (code $code)"))
    }
  }

  protected def readPurchases (conToken :String) :Seq[Purchase] = {
    val ownedItems = billSvc.getPurchases(3, getPackageName, "inapp", conToken)
    val purchases = ownedItems.getInt("RESPONSE_CODE") match {
      case 0 =>
        val data = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST")
        val sigs = ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST")
        platform.log.info("Read purchases " + data + " " + sigs)
        if (data == null || data.size == 0) Seq()
        else (((data :Seq[String]) zip sigs) map toPurchase.tupled).flatten
      case code =>
        platform.log.warn(s"getPurchases($conToken) failure [code=$code]")
        Seq()
    }
    ownedItems.getString("INAPP_CONTINUATION_TOKEN") match {
      case null => purchases
      case conToken => purchases ++ readPurchases(conToken)
    }
  }

  case class Purchase (sku :String, orderId :String, purchaseToken :String, receipt :String)
  protected val toPurchase = (json :String, sig :String) => try {
    val obj = platform.json.parse(json)
    Some(Purchase(obj.getString("productId"), obj.getString("orderId"),
                  obj.getString("purchaseToken"), sig + "\n" + json))
  } catch {
    case e :Throwable => platform.log.warn(s"toPurchase($json, $sig) failure", e) ; None
  }

  protected val toProduct = (json :String) => try {
    val obj = platform.json.parse(json)
    Some(Product(obj.getString("productId"), obj.getString("price")))
  } catch {
    case e :Throwable => platform.log.warn(s"toProduct($json) fail", e) ; None
  }

  override def scaleFactor = {
    val dm = getResources.getDisplayMetrics
    val (dwidth, dheight) = (dm.widthPixels, dm.heightPixels)
    // we may be in landscape right now, because Android is fucking retarded
    val (width, height) = if (dwidth > dheight) (dheight, dwidth) else (dwidth, dheight)
    math.min(width / 320f, height / 480f)
  }

  override def onBackPressed () {
    // only allow BACK to exit the app if we're on the main menu screen (and not currently
    // switching between screens)
    if (!game.screens.isTransiting && game.screens.top.isInstanceOf[MainMenuScreen]) {
      super.onBackPressed();
    }
  }

  private final val MillisPerMinute = 1000*60
  private final val BuyCode = 3773
}
