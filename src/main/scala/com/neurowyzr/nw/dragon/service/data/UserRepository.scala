package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.User

trait UserRepository {
  def getUserByExternalPatientRef(externalPatientRef: String): Future[Option[User]]
  def getUserBySourceAndExternalPatientRef(source: String, externalPatientRef: String): Future[Option[User]]
  def getUserByUsername(username: String): Future[Option[User]]
  def createUser(user: User): Future[User]
  def getUserById(userId: Long): Future[Option[User]]

  // Update user only updates these fields
  // fields: username, email, password, externalPatientRef, name, dateOfBirth, gender
  def updateUser(user: User): Future[Long]
  def deleteUserByUsername(username: String): Future[Boolean]

}
