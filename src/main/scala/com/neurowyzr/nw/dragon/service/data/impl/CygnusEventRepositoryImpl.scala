package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{CygnusEventDao, CygnusEventRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class CygnusEventRepositoryImpl @Inject() (dao: CygnusEventDao, pool: FuturePool)
    extends CygnusEventRepository with Logging {

  override def createCygnusEvent(cygnusEvent: root.biz.models.CygnusEvent): Future[root.biz.models.CygnusEvent] = {
    val entity = CygnusEventRepositoryImpl.toEntity(cygnusEvent)

    pool {
      dao.insertNewCygnusEvent(entity).map(newId => CygnusEventRepositoryImpl.toBiz(entity.copy(id = newId)))
    }.flatMap(tried => Future.const(tried))
  }

  override def getCygnusEventByMessageTypeAndMessageId(messageType: String,
                                                       messageId: String
                                                      ): Future[Option[root.biz.models.CygnusEvent]] = {
    pool {
      dao
        .getCygnusEventByMessageTypeAndMessageId(messageType, messageId)
        .map(maybe => maybe.map(CygnusEventRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

}

private object CygnusEventRepositoryImpl {

  def toEntity(biz: root.biz.models.CygnusEvent): root.data.models.CygnusEvent = {
    biz.into[root.data.models.CygnusEvent].transform
  }

  def toBiz(entity: root.data.models.CygnusEvent): root.biz.models.CygnusEvent = {
    entity.into[root.biz.models.CygnusEvent].transform
  }

}
