package com.neurowyzr.nw.dragon.service.data.impl

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.models.{Episode, TestSession}
import com.neurowyzr.nw.dragon.service.data.{EpisodeDao, EpisodeRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class EpisodeRepositoryImpl @Inject() (dao: EpisodeDao, pool: FuturePool) extends EpisodeRepository with Logging {

  override def insertEpisodeAndTestSession(newEpisode: root.biz.models.Episode,
                                           newTestSession: root.biz.models.TestSession
                                          ): Future[(Episode, TestSession)] = {
    val episodeEntity             = EpisodeRepositoryImpl.toEntity(newEpisode)
    val episodeEntityWithTimstamp = episodeEntity.copy(maybeUtcCreatedAt = Some(LocalDateTime.now(ZoneOffset.UTC)))
    val testSessionEntity         = TestSessionRepositoryImpl.toEntity(newTestSession)

    pool {
      dao.insertEpisodeAndTestSession(episodeEntityWithTimstamp, testSessionEntity).map {
        case (insertedEpisode, insertedTestSession) =>
          (EpisodeRepositoryImpl.toBiz(insertedEpisode), TestSessionRepositoryImpl.toBiz(insertedTestSession))
      }
    }.flatMap(tried => Future.const(tried))
  }

  override def getEpisodeByTestId(testId: String): Future[Option[root.biz.models.Episode]] = {
    pool {
      dao.getEpisodeByTestId(testId).map(maybe => maybe.map(EpisodeRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def getEpisodeByMessageId(messageId: String): Future[Option[root.biz.models.Episode]] = {
    pool {
      dao.getEpisodeByMessageId(messageId).map(maybe => maybe.map(EpisodeRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def allEpisodes: Future[Seq[root.biz.models.Episode]] = {
    pool {
      dao.allEpisodes().map(maybe => maybe.map(EpisodeRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def updateEpisodeExpiryDate(episodeId: Long, expiryDate: LocalDateTime): Future[Long] = {
    pool {
      dao.updateEpisodeExpiryDate(episodeId, expiryDate)
    }.flatMap(tried => Future.const(tried))
  }

  override def invalidateEpisode(episodeId: Long): Future[Long] = {
    pool {
      dao.invalidateEpisode(episodeId)
    }.flatMap(tried => Future.const(tried))
  }

  override def getLatestEpisodeByUsername(username: String): Future[Option[root.biz.models.Episode]] = {
    pool {
      dao.getLatestEpisodeByUsername(username).map(maybe => maybe.map(EpisodeRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def getLatestCompletedTestSessionsByUsername(
      username: String
  ): Future[Option[(root.biz.models.Episode, root.biz.models.TestSession)]] = {
    pool {
      dao.getLatestCompletedTestSessionsByUsername(username).map { maybe =>
        maybe.map { episode_testSession =>
          {
            (EpisodeRepositoryImpl.toBiz(episode_testSession._1), TestSessionRepositoryImpl.toBiz(episode_testSession._2))
          }
        }
      }
    }.flatMap(tried => Future.const(tried))
  }

  override def getEpisodeByMessageIdAndSource(messageId: String, source: String): Future[Option[root.biz.models.Episode]] = {
    pool {
      dao
        .getEpisodeByMessageIdAndSource(messageId, source)
        .map(maybeEpisode => maybeEpisode.map(EpisodeRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def getEpisodeByEpisodeRefAndSource(episodeRef: String, source: String): Future[Option[Episode]] = {
    pool {
      dao
        .getEpisodeByEpisodeRefAndSource(episodeRef, source)
        .map(maybeEpisode => maybeEpisode.map(EpisodeRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def getEpisodeById(episodeId: Long): Future[Option[Episode]] = {
    pool {
      dao.getEpisodeById(episodeId).map(maybeEpisode => maybeEpisode.map(EpisodeRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

}

private object EpisodeRepositoryImpl {

  def toEntity(biz: root.biz.models.Episode): root.data.models.Episode = {
    biz.into[root.data.models.Episode].transform
  }

  def toBiz(entity: root.data.models.Episode): root.biz.models.Episode = {
    entity.into[root.biz.models.Episode].transform
  }

}
