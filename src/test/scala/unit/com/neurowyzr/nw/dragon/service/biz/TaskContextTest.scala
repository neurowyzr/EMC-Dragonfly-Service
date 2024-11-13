package com.neurowyzr.nw.dragon.service.biz

import com.neurowyzr.nw.dragon.service.biz.models.TaskContext

import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TaskContextTest extends AnyWordSpec with IdiomaticMockito with Matchers with BeforeAndAfterEach {

  "return empty task context if it is not present" in {
    val tc = TaskContext.apply()
    val _  = tc.`type` shouldBe ""
    val _  = tc.appId shouldBe ""
    val _  = tc.messageId shouldBe ""
    val _  = tc.expiration shouldBe ""
    val _  = tc.correlationId shouldBe ""
  }

}
