package com.neurowyzr.nw.dragon.service

import java.time.{Duration, Instant}

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.InvalidateMagicLinkUnhappyFlowTest.InvalidateMagicLinkCommand
import com.neurowyzr.nw.dragon.service.mq.{Ack, InvalidateMagicLinkCmd}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{Message, PersistentMsgProperties}

trait InvalidateMagicLinkUnhappyFlowTest { this: SmokeTest =>

  test("Send command to invalidate magic link unhappy flow") {
    val currentEpisode = testDao.getEpisodeByTestId("fake-magic-link-test-id-2")
    val _              = currentEpisode.size shouldBe 0

    val invalidateMagicLinkProperties = PersistentMsgProperties(
      maybeAppId = Some("nw-cygnus-service"),
      maybeMessageId = Some("fake-invalidate-magic-link-message-id-2"),
      maybeExpiration = Some("5000"),
      maybeType = Some(InvalidateMagicLinkCmd.toString),
      maybeCorrelationId = None
    )

    val label           = "invalidate-magic-link-unhappy-flow-test"
    val dragonPublisher = createPublisher(label)
    val cygnusConsumer  = createConsumer(label)

    val invalidateMlCommand = InvalidateMagicLinkCommand("fake-magic-link-test-id-2")

    val invalidateMlCommandStr = mapper.writeValueAsString(invalidateMlCommand)
    val _ = Await.result(dragonPublisher.publish(Message(invalidateMlCommandStr, invalidateMagicLinkProperties)),
                         1.second
                        )

    Thread.sleep(2000)

    // Check changed episode
    val invalidateEpisode = testDao.getEpisodeByTestId("fake-magic-link-test-id-2")
    val _                 = invalidateEpisode.size shouldBe 0

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
    val _ = updateMessageAck shouldBe mapper.writeValueAsString(Ack(InvalidateMagicLinkCmd.toString, "Error", "106"))

    Thread.sleep(6000)

    mqContext.closeClients(Seq(dragonPublisher, cygnusConsumer))
  }

}

object InvalidateMagicLinkUnhappyFlowTest {
  final case class InvalidateMagicLinkCommand(testId: String)

}
