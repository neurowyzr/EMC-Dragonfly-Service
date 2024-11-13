package com.neurowyzr.nw.dragon.service

package object api {

  final case class ErrorMsg(
      errors: Seq[String]
  )

  object ErrorMsg {
    def apply(error: String): ErrorMsg = ErrorMsg(Seq(error))
  }

}
