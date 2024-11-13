package com.neurowyzr.nw.dragon.service.biz.impl

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject

import com.twitter.finagle.http.Status
import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.UserService
import com.neurowyzr.nw.dragon.service.biz.exceptions.*
import com.neurowyzr.nw.dragon.service.biz.models.{UserDataConsent, UserWithDataConsent}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.*
import com.neurowyzr.nw.dragon.service.biz.models.UserConsentModels.CreateUserDataConsentParams
import com.neurowyzr.nw.dragon.service.biz.models.UserModels.LatestReportParams
import com.neurowyzr.nw.dragon.service.clients.CoreHttpClient
import com.neurowyzr.nw.dragon.service.data.*
import com.neurowyzr.nw.dragon.service.utils.context.EncryptionUtil

import com.google.inject.Singleton
import io.scalaland.chimney.dsl.TransformationOps

@Singleton
class UserServiceImpl @Inject() (userRepository: UserRepository,
                                 userDataConsentRepository: UserDataConsentRepository,
                                 coreHttpClient: CoreHttpClient,
                                 episodeRepository: EpisodeRepository
                                )
    extends UserService {

  override def updateUser(params: UpdateUserParams): Future[Unit] = {
    userRepository.getUserByUsername(params.userId).flatMap {
      case Some(user) =>
        val dob = params.birthYear.toString + "-01-01 00:00:00"
        val updatedUser = user.copy(
          username = params.email,
          firstName = params.name,
          maybeDateOfBirth = Some(dob),
          maybeGender = Some(params.gender),
          maybeEmailHash = Some(EncryptionUtil.aesEncrypt(params.email)),
          maybeExternalPatientRef = Some(params.email)
        )
        userRepository.updateUser(updatedUser).map(_ => ())
      case None =>
        val reason = s"User '${params.userId}' does not exist"
        Future.exception(UserNotFoundException(reason))
    }
  }

  override def createUserConsent(params: CreateUserDataConsentParams): Future[Unit] = {
    userRepository.getUserByUsername(params.email).flatMap {
      case Some(user) =>
        val dataConsent =
          params
            .into[UserDataConsent]
            .withFieldConst(_.userId, user.id)
            .withFieldConst(_.utcCreatedAt, LocalDateTime.now(ZoneOffset.UTC))
            .withFieldConst(_.maybeUtcRevokedAt, None)
            .transform
        userDataConsentRepository.createUserConsent(dataConsent).map(_ => ())
      case None =>
        val reason = s"User '${params.email}' does not exist"
        Future.exception(UserNotFoundException(reason))
    }
  }

  override def deleteUserByUsername(username: String): Future[Unit] = {
    userRepository.deleteUserByUsername(username).map(res => ())
  }

  override def deleteUserConsentByUsername(username: String): Future[Unit] = {
    userRepository.getUserByUsername(username).flatMap {
      case Some(user) => userDataConsentRepository.deleteUserConsentByUserId(user.id).map(res => ())
      case None =>
        val reason = s"User '$username' does not exist"
        Future.exception(UserNotFoundException(reason))
    }

  }

  override def getUserByUsername(username: String): Future[UserWithDataConsent] = {
    userRepository.getUserByUsername(username).flatMap {
      case Some(user) =>
        userDataConsentRepository.getUserConsentByUserId(user.id).flatMap {
          case Some(consent) =>
            val birthYear = user.maybeDateOfBirth.map(_.split("-")(0).toInt).getOrElse(0)
            Future.value(
              UserWithDataConsent(username, user.firstName, birthYear, user.maybeGender.getOrElse(""), consent.isConsent)
            )
          case None =>
            val reason = s"Data consent for user '$username' does not exist"
            Future.exception(UserConsentNotFoundException(reason))
        }
      case None =>
        val reason = s"User '$username' does not exist"
        Future.exception(UserNotFoundException(reason))
    }
  }

  override def sendLatestReport(userBatchCode: String, username: String): Future[String] = {
    episodeRepository.getLatestCompletedTestSessionsByUsername(username).flatMap {
      case Some((episode, testSession)) =>
        val sessionId = episode.episodeRef

        coreHttpClient.sendLatestReport(LatestReportParams(userBatchCode, sessionId)).flatMap { response =>
          response.status match {
            case Status.Ok => Future.value(response.contentString)
            case Status.NotFound =>
              val reason = s"Report for $username not found"
              Future.exception(ReportNotFoundException(reason))
          }
        }
      case None =>
        val reason = s"Latest completed test session not found for: $username"
        Future.exception(ReportNotFoundException(reason))
    }
  }

}

private[impl] object UserServiceImpl
