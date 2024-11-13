package com.neurowyzr.nw.dragon.service.biz.impl

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeCustomerConfig
import com.neurowyzr.nw.dragon.service.biz.impl.ApiKeyBasedAuthServiceImplTest.{FakeInvalidApiKey, FakeValidApiKey}
import com.neurowyzr.nw.finatra.lib.api.filters.impl.DefaultAuthApiKeyHandler
import com.neurowyzr.nw.finatra.lib.biz.exceptions.AuthenticationException

import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class ApiKeyBasedAuthServiceImplTest
    extends AnyWordSpec with Matchers with IdiomaticMockito with ResetMocksAfterEachTest {

  private val testInstance = new ApiKeyBasedAuthServiceImpl(FakeCustomerConfig)

  "tokenHandler" should {
    "return the default handler" in {
      testInstance.tokenHandler shouldBe a[DefaultAuthApiKeyHandler]
    }
  }

  "authenticate" should {
    "succeed when the API key exists" in {
      val result = Await.result(testInstance.authenticate(FakeValidApiKey))
      result shouldBe Map.empty
    }

    "fail when the API key is invalid" in {
      val exception = intercept[AuthenticationException](Await.result(testInstance.authenticate(FakeInvalidApiKey)))
      exception.getMessage shouldBe "The API key 'fake-invalid-key' is invalid!"

    }
  }

}

private object ApiKeyBasedAuthServiceImplTest {
  final val FakeValidApiKey: String   = "fake-api-key"
  final val FakeInvalidApiKey: String = "fake-invalid-key"
}
