package com.neurowyzr.nw.dragon.service.api

import java.time.LocalDate
import javax.inject.Inject

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.annotations.RouteParam
import com.twitter.util.Future
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.api.ScoreController.{GetScoresRequest, GetScoresResponse, ScoreByDate}
import com.neurowyzr.nw.dragon.service.api.filters.JwtFilterService
import com.neurowyzr.nw.dragon.service.biz.ScoreService
import com.neurowyzr.nw.dragon.service.utils.context.JwtUtil

import com.auth0.jwt.interfaces.Claim
import io.scalaland.chimney.dsl.TransformationOps

class ScoreController @Inject() (service: ScoreService) extends Controller with Logging {

  filter[JwtFilterService].get("/v1/scores/:user_batch_code") { (request: GetScoresRequest) =>
    request.underlying.authorization.map { token =>
      val claims: Map[String, Claim] = JwtUtil.extractClaims(token.drop("Bearer ".length))
      service.getScores(claims("email").asString(), request.userBatchCode).flatMap { maybeRes =>
        maybeRes
          .map { res =>
            val apiResponse = GetScoresResponse(
              res.overallLatestScore,
              res.overallScoreIsFlagged,
              res.latestSessionId,
              res.scores.view.mapValues(v => v.map(s => s.into[ScoreByDate].transform)).toMap
            )
            Future.value(response.ok(apiResponse))
          }
          .getOrElse(Future.value(response.notFound))
      }
    }.get
  }

}

private object ScoreController {

  final case class GetScoresRequest(
      @RouteParam("user_batch_code") userBatchCode: String,
      underlying: Request
  )

  final case class ScoreByDate(
      /* Date of score */
      date: LocalDate,
      /* User's score */
      score: Number,
      /* Total score possible */
      totalScore: Number,
      /* Whether to show the flag */
      isFlagged: Boolean
  )

  type ScoreByType = Map[String, Seq[ScoreByDate]]

  final case class GetScoresResponse(
      /* Overall score for the latest test */
      overallLatestScore: Number,
      /* Whether to show the flag */
      overallScoreIsFlagged: Boolean,
      /* Latest session id */
      latestSessionId: String,
      /* Map of scores by overall or domain */
      scores: ScoreByType
  )

}
