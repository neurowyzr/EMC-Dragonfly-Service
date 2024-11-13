package com.neurowyzr.nw.dragon.service.api

import com.twitter.finagle.http.{Method, Request}
import com.twitter.finagle.http.Status.BadRequest
import com.twitter.finatra.http.response.DefaultResponseBuilder

import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException

import org.mockito.ArgumentMatchersSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class BizExceptionMapperTest extends AnyFunSuite with ArgumentMatchersSugar with Matchers {

  test("translates a BizException to a 400") {
    val request   = Request(Method.Get, "/")
    val errorMsg  = "whatever"
    val exception = BizException(errorMsg)
    val mapper    = new BizExceptionMapper(DefaultResponseBuilder())

    val response = mapper.toResponse(request, exception)
    response.status shouldBe BadRequest
    response.contentString shouldBe errorMsg
  }

}
