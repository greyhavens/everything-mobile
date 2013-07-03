//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

trait Device {

  /** Returns the height of our translucent status bar (if any). */
  def statusBarHeight :Float

  /** Returns the current timezone offset as "minutes before GMT". */
  def timeZoneOffset :Int

  /** Formats a date for `flipped on` etc. UI. */
  def formatDate (when :Long) :String
}
