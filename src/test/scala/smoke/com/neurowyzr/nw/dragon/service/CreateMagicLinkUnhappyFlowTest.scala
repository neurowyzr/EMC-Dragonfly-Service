package com.neurowyzr.nw.dragon.service

import java.time.{Duration, Instant, LocalDateTime}

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.CreateMagicLinkUnhappyFlowTest.CreateMagicLinkCommand
import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeLocalDateTimeNow, FakeUserBatchCode}
import com.neurowyzr.nw.dragon.service.mq.{Ack, CreateMagicLinkCmd}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{Message, PersistentMsgProperties}

trait CreateMagicLinkUnhappyFlowTest { this: SmokeTest =>

  test("Send command to create a new magic link unhappy flow") {
    val properties = PersistentMsgProperties(
      maybeAppId = Some("nw-cygnus-service"),
      maybeMessageId = Some("fake-create-magic-link-message-id-2"),
      maybeExpiration = Some("5000"),
      maybeType = Some(CreateMagicLinkCmd.toString),
      maybeCorrelationId = None
    )

    val command = CreateMagicLinkCommand(
      FakeUserBatchCode,
      "fake-magic-link-test-id",
      "fake-incorrect-ext-patient-ref",
      FakeLocalDateTimeNow,
      FakeLocalDateTimeNow,
      "fake-source"
    )

    val label           = "create-magic-link-unhappy-flow-test"
    val dragonPublisher = createPublisher(label)
    val cygnusConsumer  = createConsumer(label)

    val commandStr = mapper.writeValueAsString(command)
    val _          = Await.result(dragonPublisher.publish(Message(commandStr, properties)), 1.second)

    Thread.sleep(2000)

    // Check test session
    val userAccounts = testDao.allUserAccounts()
    val _            = userAccounts.size shouldBe 1
    val episode      = testDao.getEpisodeByTestId("fake-magic-link-test-id")
    val _            = episode.size shouldBe 1
    val testSessions = testDao.allTestSessions()
    val _            = testSessions.size shouldBe 1
    val _            = episode.head.testSessionId shouldBe testSessions.head.id

    val _ = cygnusConsumer.consume((message: Message) => {
      info("Consumer received: " + message.body)
      info("Consumer received: " + message.`type`)
      consumedMessages.add(message.body)
      Future.True
    })

    val deadline = Instant.now.plus(Duration.ofSeconds(5))

    while (consumedMessages.size() == 0 && Instant.now.isBefore(deadline))
      Thread.sleep(100)

    val _       = consumedMessages.size() shouldBe 1
    val message = consumedMessages.remove()
    val _       = message shouldBe mapper.writeValueAsString(Ack(CreateMagicLinkCmd.toString, "Error", "101"))

    Thread.sleep(6000)

    mqContext.closeClients(Seq(dragonPublisher, cygnusConsumer))
  }

}

object CreateMagicLinkUnhappyFlowTest {

  final case class CreateMagicLinkCommand(userBatchCode: String,
                                          testId: String,
                                          patientId: String,
                                          startDate: LocalDateTime,
                                          expiryDate: LocalDateTime,
                                          source: String
                                         )

}
