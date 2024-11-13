package com.neurowyzr.nw.dragon.service.data.impl

import java.sql.SQLIntegrityConstraintViolationException
import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException
import com.neurowyzr.nw.dragon.service.biz.models.User
import com.neurowyzr.nw.dragon.service.data.{UserDao, UserRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class UserRepositoryImpl @Inject() (userDao: UserDao, pool: FuturePool) extends UserRepository with Logging {

  override def getUserByExternalPatientRef(externalPatientRef: String): Future[Option[root.biz.models.User]] = {
    pool {
      userDao.getUserByExtPatientRef(externalPatientRef).map(maybe => maybe.map(UserRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def getUserBySourceAndExternalPatientRef(source: String,
                                                    externalPatientRef: String
                                                   ): Future[Option[root.biz.models.User]] = {
    pool {
      userDao
        .getUserBySourceAndExtPatientRef(source, externalPatientRef)
        .map(maybe => maybe.map(UserRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def getUserByUsername(username: String): Future[Option[User]] = {
    pool {
      userDao.getUserByUsername(username).map(maybe => maybe.map(UserRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def createUser(user: root.biz.models.User): Future[root.biz.models.User] = {
    val entity              = UserRepositoryImpl.toEntity(user)
    val entityWithTimestamp = entity.copy(maybeUtcCreatedAt = Some(LocalDateTime.now(ZoneOffset.UTC)))

    pool {
      userDao
        .insertNewUser(entityWithTimestamp)
        .map(newId => UserRepositoryImpl.toBiz(entityWithTimestamp.copy(id = newId)))
    }.flatMap(tried => Future.const(tried)).rescue { case _: SQLIntegrityConstraintViolationException =>
      val reason = s"Session id: ${entity.username} is already created."
      Future.exception(BizException(reason))
    }
  }

  override def updateUser(user: User): Future[Long] = {
    val entity              = UserRepositoryImpl.toEntity(user)
    val entityWithTimestamp = entity.copy(maybeUtcUpdatedAt = Some(LocalDateTime.now(ZoneOffset.UTC)))

    pool {
      userDao.updateUser(entityWithTimestamp).map(newId => newId)
    }.flatMap(tried => Future.const(tried))
  }

  override def deleteUserByUsername(username: String): Future[Boolean] = {
    pool {
      userDao.deleteUserByUsername(username)
    }.flatMap(tried => Future.const(tried.map(deletedEntries => deletedEntries > 0)))
  }

  override def getUserById(userId: Long): Future[Option[User]] = {
    pool {
      userDao.getUserById(userId).map(maybe => maybe.map(UserRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

}

private object UserRepositoryImpl {

  def toEntity(biz: root.biz.models.User): root.data.models.User = {
    biz.into[root.data.models.User].transform
  }

  def toBiz(entity: root.data.models.User): root.biz.models.User = {
    entity.into[root.biz.models.User].transform
  }

}
