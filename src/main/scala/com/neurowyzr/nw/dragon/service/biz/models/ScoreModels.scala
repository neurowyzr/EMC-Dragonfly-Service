package com.neurowyzr.nw.dragon.service.biz.models

import java.time.LocalDate

object ScoreModels {

  final case class ScoreByDate(
      date: LocalDate,
      score: Number,
      totalScore: Number,
      isFlagged: Boolean
  )

  type ScoreByType = Map[String, Seq[ScoreByDate]]

  final case class GetScoresResponse(
      overallLatestScore: Number,
      overallScoreIsFlagged: Boolean,
      latestSessionId: String,
      scores: Map[String, Seq[ScoreByDate]]
  )

}
