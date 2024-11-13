package com.neurowyzr.nw.dragon.service

import java.time.{Duration, Instant}

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.CreateUserHappyFlowTest.CreateUserCommand
import com.neurowyzr.nw.dragon.service.mq.{Ack, CreateUserCmd}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{Message, PersistentMsgProperties}

trait CreateUserHappyFlowTest { this: SmokeTest =>

  test("Send command to create a new user happy flow") {
    val properties = PersistentMsgProperties(
      maybeAppId = Some("nw-cygnus-service"),
      maybeMessageId = Some("fake-create-user-message-id"),
      maybeExpiration = Some("5000"),
      maybeType = Some(CreateUserCmd.toString),
      maybeCorrelationId = None
    )

    val command = CreateUserCommand(
      "fake-source",
      "fake-new-patient-ref"
    )

    val label           = "create-user-happy-flow-test"
    val dragonPublisher = createPublisher(label)
    val cygnusConsumer  = createConsumer(label)

    val commandStr = mapper.writeValueAsString(command)
    val _          = Await.result(dragonPublisher.publish(Message(commandStr, properties)), 1.second)

    Thread.sleep(2000)

    // Check new user is created
    val user = testDao.getUserByPatientRef("fake-new-patient-ref")
    val _    = user.size shouldBe 1
    val _    = user.head.maybeExternalPatientRef shouldBe Some("fake-new-patient-ref")

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
    val _       = message shouldBe mapper.writeValueAsString(Ack(CreateUserCmd.toString, "Success", ""))

    mqContext.closeClients(Seq(dragonPublisher, cygnusConsumer))
  }

}

object CreateUserHappyFlowTest {
  final case class CreateUserCommand(source: String, patientId: String)
}
