package com.neurowyzr.nw.dragon.service

import java.time.{Duration, Instant, LocalDateTime}

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeLocalDateTimeNow
import com.neurowyzr.nw.dragon.service.UpdateMagicLinkHappyFlowTest.UpdateMagicLinkCommand
import com.neurowyzr.nw.dragon.service.mq.{Ack, UpdateMagicLinkCmd}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{Message, PersistentMsgProperties}

trait UpdateMagicLinkUnhappyFlowTest { this: SmokeTest =>

  test("Send command to update magic link unhappy flow") {
    val currentEpisode = testDao.getEpisodeByTestId("fake-magic-link-test-id-2")
    val _              = currentEpisode.size shouldBe 0

    val updateMagicLinkProperties = PersistentMsgProperties(
      maybeAppId = Some("nw-cygnus-service"),
      maybeMessageId = Some("fake-update-magic-link-message-id-2"),
      maybeExpiration = Some("5000"),
      maybeType = Some(UpdateMagicLinkCmd.toString),
      maybeCorrelationId = None
    )

    val label           = "update-magic-link-unhappy-flow-test"
    val dragonPublisher = createPublisher(label)
    val cygnusConsumer  = createConsumer(label)

    val updateMagicLinkCommand = UpdateMagicLinkCommand("fake-magic-link-test-id-2", FakeLocalDateTimeNow.plusDays(2))

    val updateMagicLinkCommandStr = mapper.writeValueAsString(updateMagicLinkCommand)
    val _ = Await.result(dragonPublisher.publish(Message(updateMagicLinkCommandStr, updateMagicLinkProperties)),
                         1.second
                        )

    Thread.sleep(2000)

    // Check changed episode
    val updatedEpisode = testDao.getEpisodeByTestId("fake-magic-link-test-id-2")
    val _              = updatedEpisode.size shouldBe 0

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
    val _ = updateMessageAck shouldBe mapper.writeValueAsString(Ack(UpdateMagicLinkCmd.toString, "Error", "106"))

    Thread.sleep(6000)

    mqContext.closeClients(Seq(dragonPublisher, cygnusConsumer))
  }

}

object UpdateMagicLinkUnhappyFlowTest {
  final case class UpdateMagicLinkCommand(testId: String, expiryDate: LocalDateTime)

}
