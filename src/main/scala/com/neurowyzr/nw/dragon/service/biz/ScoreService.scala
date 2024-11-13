package com.neurowyzr.nw.dragon.service.biz

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.ScoreModels.GetScoresResponse

trait ScoreService {
  def getScores(username: String, userBatchCode: String): Future[Option[GetScoresResponse]]
}
