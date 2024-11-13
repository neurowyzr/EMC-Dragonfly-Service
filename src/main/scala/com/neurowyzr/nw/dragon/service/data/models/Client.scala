package com.neurowyzr.nw.dragon.service.data.models

private[data] final case class Client(id: Long,
                                      name: String,
                                      maybeAddress: Option[String],
                                      maybeContactPhone: Option[String],
                                      maybeContactEmail: Option[String],
                                      maybeContactName: Option[String]
                                     )

object Client {

  def apply(name: String): Client = Client(Defaults.DefaultLongId, name, None, None, None, None)

}
