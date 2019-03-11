package com.cbft

package object merkle {

  /** Hash functions map blocks to blocks */
  type Digest = (String) => String

  private[merkle] def notNull(x: Any, n: String) =
    require(null != x, s"`$n` must not be null.")
}
