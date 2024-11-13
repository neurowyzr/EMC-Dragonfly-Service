package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service as root

trait CygnusEventRepository {
  def createCygnusEvent(cygnusEvent: root.biz.models.CygnusEvent): Future[root.biz.models.CygnusEvent]

  def getCygnusEventByMessageTypeAndMessageId(messageType: String,
                                              messageId: String
                                             ): Future[Option[root.biz.models.CygnusEvent]]

}
