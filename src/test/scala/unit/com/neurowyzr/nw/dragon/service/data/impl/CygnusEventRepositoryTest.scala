package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeMessageId, FakeMessageType}
import com.neurowyzr.nw.dragon.service.biz.models.CygnusEvent
import com.neurowyzr.nw.dragon.service.data.CygnusEventDao
import com.neurowyzr.nw.dragon.service.data.impl.CygnusEventRepositoryTest.TestException
import com.neurowyzr.nw.dragon.service as root

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class CygnusEventRepositoryTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao: CygnusEventDao = mock[CygnusEventDao]
  private val testInstance            = new CygnusEventRepositoryImpl(mockDao, pool)

  "get cygnus event by message id" should {
    "succeed" in {
      val dataCygnusEvent = root.data.models.CygnusEvent(FakeMessageType, FakeMessageId)
      val bizCygnusEvent  = root.biz.models.CygnusEvent(FakeMessageType, FakeMessageId)

      val _ = mockDao.getCygnusEventByMessageTypeAndMessageId(*[String], *[String]) returns Try(Some(dataCygnusEvent))

      val maybeReturnedCygnusEvent = await(
        testInstance.getCygnusEventByMessageTypeAndMessageId(FakeMessageType, FakeMessageId)
      )

      val _ = maybeReturnedCygnusEvent.size shouldBe 1
      val _ = maybeReturnedCygnusEvent.get shouldBe bizCygnusEvent

      val _ = mockDao.getCygnusEventByMessageTypeAndMessageId(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getCygnusEventByMessageTypeAndMessageId(*[String], *[String]) returns Try(None)

      val maybeReturnedCygnusEvent = await(
        testInstance.getCygnusEventByMessageTypeAndMessageId(FakeMessageType, FakeMessageId)
      )

      val _ = maybeReturnedCygnusEvent.size shouldBe 0

      val _ = mockDao.getCygnusEventByMessageTypeAndMessageId(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getCygnusEventByMessageTypeAndMessageId(*[String], *[String]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getCygnusEventByMessageTypeAndMessageId(FakeMessageType, FakeMessageId))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getCygnusEventByMessageTypeAndMessageId(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "createCygnusEvent" should {
    "return a cygnus event with a new ID" in {
      val _               = mockDao.insertNewCygnusEvent(*[root.data.models.CygnusEvent]) returns Try(8899L)
      val fakeCygnusEvent = CygnusEvent(FakeMessageType, FakeMessageId)

      val newCygnusEvent = await(testInstance.createCygnusEvent(fakeCygnusEvent))

      val _ = newCygnusEvent.id shouldBe 8899L
      val _ = newCygnusEvent.messageId shouldBe FakeMessageId
      val _ = mockDao.insertNewCygnusEvent(*[root.data.models.CygnusEvent]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object CygnusEventRepositoryTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}
