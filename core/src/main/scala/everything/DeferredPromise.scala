//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import playn.core.PlayN.invokeLater
import playn.core.util.Callback
import react.RPromise

/** A promise that implements [Callback] and routes `onSuccess` and `onFailure` through
  * `PlayN.invokeLater` before satisfying the promise. This is mainly used by the device backends
  * to get back onto the PlayN thread when reporting results of things like Facebook requests.
  */
class DeferredPromise[T] extends RPromise[T] with Callback[T] {

  override def onSuccess (result :T) = invokeLater(new Runnable() {
    def run () = succeed(result)
  })

  override def onFailure(cause :Throwable) = invokeLater(new Runnable() {
    def run () = fail(cause)
  })
}
