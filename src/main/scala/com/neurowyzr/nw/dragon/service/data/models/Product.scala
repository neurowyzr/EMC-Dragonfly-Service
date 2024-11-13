package com.neurowyzr.nw.dragon.service.data.models

private[data] final case class Product(id: Long, name: String, maybeDescription: Option[String])

object Product {

  def apply(name: String): Product = Product(Defaults.DefaultLongId, name, None)

}
