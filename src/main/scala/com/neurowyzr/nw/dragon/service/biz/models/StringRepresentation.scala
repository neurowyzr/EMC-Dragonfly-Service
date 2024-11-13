package com.neurowyzr.nw.dragon.service.biz.models

trait StringRepresentation {
  self: Product =>

  override def toString: String = {
    pprint.apply(this).plainText
  }

}
