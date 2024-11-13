package com.neurowyzr.nw.dragon.service.biz.impl

import java.time.LocalDateTime
import javax.inject.Inject

import com.twitter.util.Future
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.ScoreService
import com.neurowyzr.nw.dragon.service.biz.impl.ScoreServiceImpl.{
  extractScores, removeBracketsAndConvertToNum, ZScoreOverallPath
}
import com.neurowyzr.nw.dragon.service.biz.models.ScoreModels.{GetScoresResponse, ScoreByDate}
import com.neurowyzr.nw.dragon.service.biz.models.TestSessionWithSessionId
import com.neurowyzr.nw.dragon.service.data.TestSessionRepository

import com.google.inject.Singleton
import com.jayway.jsonpath.JsonPath

@Singleton
class ScoreServiceImpl @Inject() (testSessionRep: TestSessionRepository) extends ScoreService with Logging {

  override def getScores(username: String, userBatchCode: String): Future[Option[GetScoresResponse]] = {
    testSessionRep.getTestSessionsByUsernameAndUserBatch(username, userBatchCode).map { testSessions =>
      val scoresByDate = testSessions.flatMap { completedTestSession =>
        for {
          zScore <- completedTestSession.maybeZScore
          date   <- completedTestSession.maybeUtcCompletedAt
        } yield extractScores(zScore, date)
      }
      if (scoresByDate.isEmpty) {
        None
      } else {
        val maybeLatest = testSessions.headOption
        maybeLatest.flatMap { latestSession =>
          val scoreMap =
            scoresByDate
              .flatMap(_.toSeq)
              .groupBy { case (category, _) => category }
              .view
              .mapValues(_.map { case (_, scoreSeq) => scoreSeq })
              .toMap
          Some(generateGetScoresResponse(latestSession, scoreMap))
        }
      }

    }
  }

  private[biz] def generateGetScoresResponse(session: TestSessionWithSessionId,
                                             scoreMap: Map[String, Seq[ScoreByDate]]
                                            ): GetScoresResponse = {
    val overallLatestScore = session.maybeZScore
      .map(zScore => removeBracketsAndConvertToNum(JsonPath.parse(zScore).read(ZScoreOverallPath).toString))
      .getOrElse(Integer.valueOf(-1))
    val latestSessionId = session.sessionId
    GetScoresResponse(overallLatestScore, overallLatestScore < 85, latestSessionId, scoreMap)
  }

}

object ScoreServiceImpl {

  val ZScorePath: String = "*.scoreReports.%1$s.zscore.calloutScore"
  val ZScoreOverallPath  = "*.scoreReportOverall.zscore.calloutScore"
  val MaxScore           = 200
  val Threshold          = 85

  def extractScores(zScore: String, date: LocalDateTime): Map[String, ScoreByDate] = {
    val memoryScore = removeBracketsAndConvertToNum(
      JsonPath.parse(zScore).read(String.format(ZScorePath, "['Working Memory']")).toString
    )
    val attentionScore = removeBracketsAndConvertToNum(
      JsonPath.parse(zScore).read(String.format(ZScorePath, "Attention")).toString
    )
    val executiveFunction = removeBracketsAndConvertToNum(
      JsonPath.parse(zScore).read(String.format(ZScorePath, "['Executive Function']")).toString
    )

    val overallScore = removeBracketsAndConvertToNum(JsonPath.parse(zScore).read(ZScoreOverallPath).toString)

    Map(
      "overall"            -> ScoreByDate(date.toLocalDate, overallScore, MaxScore, overallScore < Threshold),
      "working_memory"     -> ScoreByDate(date.toLocalDate, memoryScore, MaxScore, memoryScore < Threshold),
      "attention"          -> ScoreByDate(date.toLocalDate, attentionScore, MaxScore, attentionScore < Threshold),
      "executive_function" -> ScoreByDate(date.toLocalDate, executiveFunction, MaxScore, executiveFunction < Threshold)
    )
  }

  def removeBracketsAndConvertToNum(s: String): Integer = {
    s.replace("[", "").replace("]", "").toDouble.toInt
  }

}
