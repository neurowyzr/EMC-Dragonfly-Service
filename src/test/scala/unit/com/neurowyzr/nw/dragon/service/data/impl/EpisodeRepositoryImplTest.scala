package com.neurowyzr.nw.dragon.service.data.impl

import java.time.LocalDateTime

import com.twitter.inject.TestMixin
import com.twitter.util.{Throw, Try}

import com.neurowyzr.nw.dragon.service.SharedFakes.{
  FakeEpisodeIdAlpha, FakeEpisodeRefAlpha, FakeMessageIdAlpha, FakeNewEpisodeRef, FakeSource
}
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeEpisodeSeq
import com.neurowyzr.nw.dragon.service.data.EpisodeDao
import com.neurowyzr.nw.dragon.service.data.impl.EpisodeRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service.data.impl.Fakes.{FakeNewEpisode, FakeTestSession}
import com.neurowyzr.nw.dragon.service.data.models.{Episode, TestSession}
import com.neurowyzr.nw.dragon.service as root

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class EpisodeRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao: EpisodeDao = mock[EpisodeDao]
  private val testInstance        = new EpisodeRepositoryImpl(mockDao, pool)

  "insert new episode" should {
    "succeed" in {
      val _ =
        mockDao.insertEpisodeAndTestSession(*[Episode], *[TestSession]) returns Try(
          (FakeNewEpisode, FakeTestSession)
        )

      val maybeEpisodeAndTestSession = await(
        testInstance.insertEpisodeAndTestSession(root.biz.impl.Fakes.FakeNewEpisode, root.biz.impl.Fakes.FakeTestSession)
      )

      val _ =
        maybeEpisodeAndTestSession shouldBe
          (root.biz.impl.Fakes.FakeNewEpisode, root.biz.impl.Fakes.FakeTestSession)
      val _ = mockDao.insertEpisodeAndTestSession(*[Episode], *[TestSession]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown" in {
      val _ = mockDao.insertEpisodeAndTestSession(*[Episode], *[TestSession]) returns Throw(TestException)

      val exception = intercept[Exception](
        await(
          testInstance.insertEpisodeAndTestSession(root.biz.impl.Fakes.FakeNewEpisode,
                                                   root.biz.impl.Fakes.FakeTestSession
                                                  )
        )
      )

      val _ = mockDao.insertEpisodeAndTestSession(*[Episode], *[TestSession]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

  }

  "get episode by test id" should {
    "succeed" in {
      val _ = mockDao.getEpisodeByTestId(*[String]) returns Try(Some(root.data.impl.Fakes.FakeNewEpisode))

      val maybeEpisode = await(testInstance.getEpisodeByTestId(FakeNewEpisodeRef))

      val _ = maybeEpisode.size shouldBe 1
      val _ = maybeEpisode.get shouldBe root.biz.impl.Fakes.FakeNewEpisode
      val _ = mockDao.getEpisodeByTestId(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for invalid test id" in {
      val _ = mockDao.getEpisodeByTestId(*[String]) throws TestException

      val exception = intercept[Exception](await(testInstance.getEpisodeByTestId("some-invalid-id")))

      val _ = exception shouldBe TestException
    }
  }

  "get episode by message id" should {
    "succeed" in {
      val _ = mockDao.getEpisodeByMessageId(*[String]) returns Try(Some(root.data.impl.Fakes.FakeEpisodeAlpha))

      val maybeEpisode = await(testInstance.getEpisodeByMessageId(FakeMessageIdAlpha))

      val _ = maybeEpisode.size shouldBe 1
      val _ = maybeEpisode.get shouldBe root.biz.impl.Fakes.FakeEpisodeAlpha
      val _ = mockDao.getEpisodeByMessageId(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for invalid message id" in {
      val _ = mockDao.getEpisodeByMessageId(*[String]) throws TestException

      val exception = intercept[Exception](await(testInstance.getEpisodeByMessageId("some-invalid-id")))

      val _ = exception shouldBe TestException
    }
  }

  "get all episodes" should {
    "succeed" in {
      val _ = mockDao.allEpisodes() returns Try(root.data.impl.Fakes.FakeEpisodeSeq)

      val all = await(testInstance.allEpisodes)

      val _ = all.size shouldBe root.data.impl.Fakes.FakeEpisodeSeq.size
      val _ = all shouldBe FakeEpisodeSeq
      val _ = mockDao.allEpisodes() wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for retrieving data" in {
      val _ = mockDao.allEpisodes() throws TestException

      val exception = intercept[Exception](await(testInstance.allEpisodes))

      val _ = exception shouldBe TestException
    }
  }

  "update episode expiry date" should {
    "succeed" in {
      val _ = mockDao.updateEpisodeExpiryDate(*[Long], *[LocalDateTime]) returns Try(123456L)

      val returnedId = await(testInstance.updateEpisodeExpiryDate(12345L, LocalDateTime.now()))

      val _ = returnedId shouldBe 123456L
      val _ = mockDao.updateEpisodeExpiryDate(*[Long], *[LocalDateTime]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for retrieving data" in {
      val _ = mockDao.updateEpisodeExpiryDate(*[Long], *[LocalDateTime]) throws TestException

      val exception = intercept[Exception](await(testInstance.updateEpisodeExpiryDate(12345L, LocalDateTime.now())))

      val _ = exception shouldBe TestException
    }
  }

  "update episode is valid" should {
    "succeed" in {
      val _ = mockDao.invalidateEpisode(*[Long]) returns Try(123456L)

      val returnedId = await(testInstance.invalidateEpisode(12345L))

      val _ = returnedId shouldBe 123456L
      val _ = mockDao.invalidateEpisode(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for retrieving data" in {
      val _ = mockDao.invalidateEpisode(*[Long]) throws TestException

      val exception = intercept[Exception](await(testInstance.invalidateEpisode(12345L)))

      val _ = exception shouldBe TestException
    }
  }

  "get latest episode by username" should {
    "succeed" in {
      val _ = mockDao.getLatestEpisodeByUsername(*[String]) returns Try(Some(root.data.impl.Fakes.FakeEpisodeAlpha))

      val maybeEpisode = await(testInstance.getLatestEpisodeByUsername(FakeMessageIdAlpha))

      val _ = maybeEpisode.size shouldBe 1
      val _ = maybeEpisode.get shouldBe root.biz.impl.Fakes.FakeEpisodeAlpha
      val _ = mockDao.getLatestEpisodeByUsername(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for invalid message id" in {
      val _ = mockDao.getLatestEpisodeByUsername(*[String]) throws TestException

      val exception = intercept[Exception](await(testInstance.getLatestEpisodeByUsername("some-name")))

      val _ = exception shouldBe TestException
    }
  }

  "get the latest completed test sessions for a given username" should {
    "succeed" in {
      val _ =
        mockDao.getLatestCompletedTestSessionsByUsername(*[String]) returns Try(
          Some((root.data.impl.Fakes.FakeEpisodeAlpha, root.data.impl.Fakes.FakeTestSession))
        )

      val maybeEpisodeTestSession = await(testInstance.getLatestCompletedTestSessionsByUsername(FakeMessageIdAlpha))

      val _ = maybeEpisodeTestSession.size shouldBe 1
      val _ =
        maybeEpisodeTestSession.get shouldBe (root.biz.impl.Fakes.FakeEpisodeAlpha, root.biz.impl.Fakes.FakeTestSession)
      val _ = mockDao.getLatestCompletedTestSessionsByUsername(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for invalid message id" in {
      val _ = mockDao.getLatestCompletedTestSessionsByUsername(*[String]) throws TestException

      val exception = intercept[Exception](await(testInstance.getLatestCompletedTestSessionsByUsername("some-name")))

      val _ = exception shouldBe TestException
    }
  }

  "get episode by message id and source" should {
    "succeed" in {
      val _ =
        mockDao.getEpisodeByMessageIdAndSource(*[String], *[String]) returns Try(
          Some(root.data.impl.Fakes.FakeEpisodeAlpha)
        )

      val maybeEpisode = await(testInstance.getEpisodeByMessageIdAndSource(FakeMessageIdAlpha, FakeSource))

      val _ = maybeEpisode.size shouldBe 1
      val _ = maybeEpisode.get shouldBe root.biz.impl.Fakes.FakeEpisodeAlpha
      val _ = mockDao.getEpisodeByMessageIdAndSource(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for invalid message id" in {
      val _ = mockDao.getEpisodeByMessageIdAndSource(*[String], *[String]) throws TestException

      val exception = intercept[Exception](
        await(testInstance.getEpisodeByMessageIdAndSource("some-invalid-id", "some-invalid-source"))
      )

      val _ = exception shouldBe TestException
    }
  }

  "get episode by episode ref and source" should {
    "succeed" in {
      val _ =
        mockDao.getEpisodeByEpisodeRefAndSource(*[String], *[String]) returns Try(
          Some(root.data.impl.Fakes.FakeEpisodeAlpha)
        )

      val maybeEpisode = await(testInstance.getEpisodeByEpisodeRefAndSource(FakeEpisodeRefAlpha, FakeSource))

      val _ = maybeEpisode.size shouldBe 1
      val _ = maybeEpisode.get shouldBe root.biz.impl.Fakes.FakeEpisodeAlpha
      val _ = mockDao.getEpisodeByEpisodeRefAndSource(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for invalid message id" in {
      val _ = mockDao.getEpisodeByEpisodeRefAndSource(*[String], *[String]) throws TestException

      val exception = intercept[Exception](
        await(testInstance.getEpisodeByEpisodeRefAndSource("some-invalid-ref", "some-invalid-source"))
      )

      val _ = exception shouldBe TestException
    }
  }

  "get episode by id" should {
    "succeed" in {
      val _ =
        mockDao.getEpisodeById(*[Long]) returns Try(
          Some(root.data.impl.Fakes.FakeEpisodeAlpha)
        )

      val maybeEpisode = await(testInstance.getEpisodeById(FakeEpisodeIdAlpha))

      val _ = maybeEpisode.size shouldBe 1
      val _ = maybeEpisode.get shouldBe root.biz.impl.Fakes.FakeEpisodeAlpha
      val _ = mockDao.getEpisodeById(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when nothing is returned for message id" in {
      val _ = mockDao.getEpisodeById(*[Long]) returns Try(None)

      val maybeEpisode = await(testInstance.getEpisodeById(FakeEpisodeIdAlpha))

      val _ = maybeEpisode.size shouldBe 0
      val _ = mockDao.getEpisodeById(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for invalid message id" in {
      val _ = mockDao.getEpisodeById(*[Long]) throws TestException

      val exception = intercept[Exception](
        await(testInstance.getEpisodeById(FakeEpisodeIdAlpha))
      )

      val _ = exception shouldBe TestException
    }
  }

}

private object EpisodeRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}
