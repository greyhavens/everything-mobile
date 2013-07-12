//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

object Swipe {
  sealed trait Dir
  object Up    extends Dir
  object Down  extends Dir
  object Left  extends Dir
  object Right extends Dir
}
