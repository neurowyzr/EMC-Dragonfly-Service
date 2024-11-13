package com.neurowyzr.nw.dragon.service.context

import com.twitter.finagle.context.Contexts

import com.neurowyzr.nw.dragon.service.utils.context.MessageContext
import com.neurowyzr.nw.dragon.service.utils.context.MessageContext.MessageContextKey
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{ArbitraryMsgProperties, Message}

import org.junit.Assert.assertTrue
import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MessageContextTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with BeforeAndAfterEach with OptionValues {

  "get empty map if context is empty" in {
    assertTrue(MessageContext.toMap.isEmpty)
  }

  "return empty map if context property is empty" in {
    val body           = "body"
    val message        = Message(body, None)
    val messageContext = MessageContext(message)

    Contexts.local.let(MessageContextKey, messageContext) {
      val result = MessageContext.toMap

      assertTrue(result.isEmpty)
    }
  }

  "return map if context is not empty" in {
    val body           = "body"
    val msgProperties  = ArbitraryMsgProperties.apply("unit test", "fake-type", "fake-id", None)
    val message        = Message(body, Some(msgProperties))
    val messageContext = MessageContext(message)

    Contexts.local.let(MessageContextKey, messageContext) {
      val result = MessageContext.toMap

      assertTrue(result.nonEmpty)
      val _ = result("message_id") shouldBe msgProperties.maybeMessageId.value
      val _ = result("message_origin") shouldBe msgProperties.maybeAppId.value
      val _ = result("message_type") shouldBe msgProperties.maybeType.value
      val _ = result("message_timestamp") shouldBe msgProperties.maybeTimestamp.value.toString
    }
  }

}
