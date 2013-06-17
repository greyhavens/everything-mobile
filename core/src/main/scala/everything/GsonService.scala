//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import com.google.gson.Gson
import playn.core.PlayN._
import playn.core._
import playn.core.util.Callback
import react.{RFuture, RPromise}
import scala.collection.JavaConversions._

abstract class GsonService (game :Everything, baseURL :String) {

  def request[R] (method :String, clazz :Class[R]) :RFuture[R] = request(method, null, clazz)

  def request[R] (method :String, args :AnyRef, clazz :Class[R]) :RFuture[R] = {
    val promise = RPromise.create[R]
    val bldr = net.req(s"$baseURL/$method")
    if (args != null) bldr.setPayload(_gson.toJson(args))
    game.authToken match {
      case None => // nada
      case Some(authTok) => bldr.addHeader("Cookie", s"auth=$authTok")
    }
    bldr.execute(new Callback[Net.Response] {
      def onSuccess (rsp :Net.Response) = try {
        noteAuthCookie(rsp)
        promise.succeed(_gson.fromJson(rsp.payloadString, clazz))
      } catch {
        case e :Throwable => promise.fail(e)
      }
      def onFailure (cause :Throwable) = {
        println("Fail " + cause)
        promise.fail(cause)
      }
    })
    promise
  }

  protected def noteAuthCookie (rsp :Net.Response) = {
    val cookies = rsp.headers("Set-Cookie")
    try {
      cookies find(_.startsWith("auth=")) foreach { ac =>
        // the cookie header looks like this; we split on ; then on = to get the hash
        // auth=7dc9de404c488abaf15035b3ae958374;Path=/;Expires=Sat, 14-Dec-2013 22:08:02 GMT
        game.updateAuthToken(ac.split(";")(0).split("=")(1))
      }
    } catch {
      case e :Throwable => log.warn(s"Failed to note auth cookie [ch=$cookies]", e)
    }
  }

  protected val _gson = new Gson
}
