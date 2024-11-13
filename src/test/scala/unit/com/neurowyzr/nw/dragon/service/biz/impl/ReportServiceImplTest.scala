package com.neurowyzr.nw.dragon.service.biz.impl

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeDbfsConfig

import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ReportServiceImplTest
    extends AnyWordSpecLike with IdiomaticMockito with ArgumentMatchersSugar with Matchers with ResetMocksAfterEachTest
    with OptionValues {

  private val testInstance = new ReportServiceImpl(FakeDbfsConfig)

  "getReportPath" when {
    "session id is not empty" should {
      "return path" in {
        testInstance.getReportPath(
          "session_id"
        ) shouldBe FakeDbfsConfig.reportS3PublicPath + "session_id/brain_score_report.pdf"
      }
    }
  }

}
