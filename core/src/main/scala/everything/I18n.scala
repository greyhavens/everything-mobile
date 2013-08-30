//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package everything

import com.threerings.everything.rpc._

object I18n {
  import GameAPI._
  import EveryAPI._

  def xlate (msg :String) = msg match {
    case E_FACEBOOK_DOWN => "Egad! We're having difficulty talking to Facebook."
    case GameAPI.E_UNKNOWN_USER => "That player doesn't seem to exist."
    case E_UNKNOWN_SERIES => "That series doesn't seem to exist."
    case E_TOO_FEW_SERIES => "You must have at least 8 unfinished series to use this powerup."
    case E_GRID_EXPIRED => "Your grid has expired."
    case E_ALREADY_FLIPPED => "It seems you've already flipped that card."
    case E_LACK_FREE_FLIP => "It looks like you don't have a free flip anymore."
    case E_FLIP_COST_CHANGED => "The cost of that flip changed. Reload and try again?"
    case E_NSF_FOR_FLIP => "You don't have enough coins for that flip."
    case E_UNKNOWN_CARD => "That card doesn't seem to exist."
    case E_ALREADY_OWN_POWERUP => "You already own that powerup."
    case E_NSF_FOR_PURCHASE => "You don't have enough coins to buy that powerup."
    case E_LACK_CHARGE => "You don't have any charges left for that powerup."
    case _ => if (msg.length > 256) s"${msg.substring(0, 256)}..." else msg
  }
}
