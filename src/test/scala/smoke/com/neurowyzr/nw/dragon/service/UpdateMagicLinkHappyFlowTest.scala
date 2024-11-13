package com.neurowyzr.nw.dragon.service

import java.time.{Duration, Instant, LocalDateTime}

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeLocalDateTimeNow
import com.neurowyzr.nw.dragon.service.UpdateMagicLinkHappyFlowTest.UpdateMagicLinkCommand
import com.neurowyzr.nw.dragon.service.mq.{Ack, UpdateMagicLinkCmd}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{Message, PersistentMsgProperties}

trait UpdateMagicLinkHappyFlowTest { this: SmokeTest =>

  test("Send command to update magic link happy flow") {
    val currentEpisode = testDao.getEpisodeByTestId("fake-magic-link-test-id")
    val _              = currentEpisode.size shouldBe 1
    val _              = currentEpisode.head.maybeUtcExpiryAt shouldBe Some(FakeLocalDateTimeNow)

    val updateMagicLinkProperties = PersistentMsgProperties(
      maybeAppId = Some("nw-cygnus-service"),
      maybeMessageId = Some("fake-update-magic-link-message-id"),
      maybeExpiration = Some("5000"),
      maybeType = Some(UpdateMagicLinkCmd.toString),
      maybeCorrelationId = None
    )

    val label           = "update-magic-link-happy-flow-test"
    val dragonPublisher = createPublisher(label)
    val cygnusConsumer  = createConsumer(label)

    val updateMagicLinkCommand = UpdateMagicLinkCommand("fake-magic-link-test-id", FakeLocalDateTimeNow.plusDays(2))

    val updateMagicLinkCommandStr = mapper.writeValueAsString(updateMagicLinkCommand)
    val _ = Await.result(dragonPublisher.publish(Message(updateMagicLinkCommandStr, updateMagicLinkProperties)),
                         1.second
                        )

    Thread.sleep(2000)

    // Check changed episode
    val updatedEpisode = testDao.getEpisodeByTestId("fake-magic-link-test-id")
    val _              = updatedEpisode.size shouldBe 1
    val _              = updatedEpisode.head.maybeUtcExpiryAt shouldBe Some(FakeLocalDateTimeNow.plusDays(2))

    val _ = cygnusConsumer.consume((message: Message) => {
      info("Consumer received: " + message.body)
      info("Consumer received: " + message.`type`)
      consumedMessages.add(message.body)
      Future.True
    })

    val deadline = Instant.now.plus(Duration.ofSeconds(10))

    while (consumedMessages.size() == 0 && Instant.now.isBefore(deadline))
      Thread.sleep(100)

    val _                = consumedMessages.size() shouldBe 1
    val updateMessageAck = consumedMessages.remove()
    val _ = updateMessageAck shouldBe mapper.writeValueAsString(Ack(UpdateMagicLinkCmd.toString, "Success", ""))

    Thread.sleep(6000)

    mqContext.closeClients(Seq(dragonPublisher, cygnusConsumer))
  }

}

object UpdateMagicLinkHappyFlowTest {
  final case class UpdateMagicLinkCommand(testId: String, expiryDate: LocalDateTime)

}
