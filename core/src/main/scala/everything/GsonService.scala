//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import com.google.gson.{GsonBuilder, LongSerializationPolicy}
import playn.core.PlayN._
import playn.core._
import playn.core.util.Callback
import react.{RFuture, RPromise, Try}
import scala.collection.JavaConversions._

abstract class GsonService (game :Everything, baseURL :String) {

  def request[R] (method :String, clazz :Class[R]) :RFuture[R] = request(method, null, clazz)

  def request[R] (method :String, args :AnyRef, clazz :Class[R]) :RFuture[R] =
    doRequest(method, args, body => _gson.fromJson(body, clazz))

  def invoke[R] (method :String, args :AnyRef) :RFuture[Unit] =
    doRequest(method, args, body => ())

  protected def doRequest[R] (method :String, args :AnyRef, rfun :String => R) = {
    val promise = RPromise.create[R]
    val bldr = net.req(s"$baseURL/$method")
    val pay = args match {
      case null => ""
      case args => _gson.toJson(args)
    }
    bldr.setPayload(pay)
    game.authToken match {
      case None => // nada
      case Some(authTok) => bldr.addHeader("Cookie", s"auth=$authTok")
    }
    log.info(s"REQ $method $pay")
    bldr.execute(new Callback[Net.Response] {
      def onSuccess (rsp :Net.Response) = {
        val result = try {
          if (rsp.responseCode != 200)
            throw new Net.HttpException(rsp.responseCode, rsp.payloadString)
          noteAuthCookie(rsp)
          log.info(s"RSP $method ${rsp.payloadString}")
          Try.success(rfun(rsp.payloadString))
        } catch {
          case e :Throwable => Try.failure[R](e)
        }
        promise.completer.onEmit(result)
      }
      def onFailure (cause :Throwable) = {
        log.warn(s"Service call failed [baseURL=$baseURL, method=$method, error=$cause]")
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

  protected val _gson = new GsonBuilder().
    setLongSerializationPolicy(LongSerializationPolicy.STRING).
    create()
}
