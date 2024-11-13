package com.neurowyzr.nw.dragon.service.data

import java.time.LocalDateTime

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.{Episode, TestSession}

trait EpisodeRepository {

  def insertEpisodeAndTestSession(episode: Episode, testSession: TestSession): Future[(Episode, TestSession)]
  def getEpisodeByTestId(testId: String): Future[Option[Episode]]
  def getEpisodeByMessageId(messageId: String): Future[Option[Episode]]
  def allEpisodes: Future[Seq[Episode]]
  def updateEpisodeExpiryDate(episodeId: Long, expiryDate: LocalDateTime): Future[Long]
  def invalidateEpisode(episodeId: Long): Future[Long]
  def getLatestEpisodeByUsername(username: String): Future[Option[Episode]]

  def getLatestCompletedTestSessionsByUsername(username: String): Future[Option[(Episode, TestSession)]]
  def getEpisodeByMessageIdAndSource(messageId: String, source: String): Future[Option[Episode]]
  def getEpisodeByEpisodeRefAndSource(episodeRef: String, source: String): Future[Option[Episode]]
  def getEpisodeById(episodeId: Long): Future[Option[Episode]]
}
