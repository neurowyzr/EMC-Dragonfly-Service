package com.neurowyzr.nw.dragon.service

import java.util.Date

import com.twitter.util.{Await, Duration}

import com.neurowyzr.nw.dragon.service.ConsumerFlowTest.*
import com.neurowyzr.nw.dragon.service.mq.NotifyClientTaskCmd
import com.neurowyzr.nw.dragon.service.mq.impl.SelfPublisherImpl.{PublishFailurePayload, PublishSuccessPayload}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{Message, PersistentMsgProperties}

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*

trait ConsumerFlowTest { this: SmokeTest =>

  test("Receive success NotifyClientTaskCmd flow") {
    WireMock.configureFor(WireMockHost, WireMockPort)
    WireMock.reset()

    val label           = "notify-client-task-flow"
    val dragonPublisher = createPublisher(label)

    Await.result(
      dragonPublisher.publish(
        Message(mapper.writeValueAsString(FakePublishSuccessPayload), FakeNotifyClientTaskCmdProperties)
      )
    )

    Thread.sleep(1500)

    verify(postRequestedFor(urlPathEqualTo("/api/User/authenticate")))
    verify(
      postRequestedFor(
        urlPathEqualTo(s"/v1/patients/$FakePatientRef/episodes/$FakeEpisodeRef/LocationId/$FakeLocationId/status")
      ).withHeader("Content-Type", equalTo("application/json"))
        .withHeader("Authorization", equalTo("Bearer fake-auth-token-12345"))
        .withHeader("Correlation-Id", equalTo(FakeRequestId))
        .withRequestBody(matchingJsonPath("$.status", equalTo("1")))
        .withRequestBody(matchingJsonPath("$.link", equalTo("fake-magic-link-url")))
    )
  }

  test("Receive failure NotifyClientTaskCmd flow") {
    WireMock.configureFor(WireMockHost, WireMockPort)
    WireMock.reset()

    // Failure
    val label           = "notify-client-task-flow"
    val dragonPublisher = createPublisher(label)

    Await.result(
      dragonPublisher.publish(
        Message(mapper.writeValueAsString(FakePublishFailurePayload), FakeNotifyClientTaskCmdProperties)
      )
    )

    Thread.sleep(1500)

    verify(postRequestedFor(urlPathEqualTo("/api/User/authenticate")))
    verify(
      postRequestedFor(
        urlPathEqualTo(s"/v1/patients/$FakePatientRef/episodes/$FakeEpisodeRef/LocationId/$FakeLocationId/status")
      ).withHeader("Content-Type", equalTo("application/json"))
        .withHeader("Authorization", equalTo("Bearer fake-auth-token-12345"))
        .withHeader("Correlation-Id", equalTo(FakeRequestId))
        .withRequestBody(matchingJsonPath("$.status"))
    )
  }

}

private object ConsumerFlowTest {

  val WireMockHost = "localhost"
  val WireMockPort = 8080

//  private final case class UploadPayload(episodeId: Long, userBatchCode: String, s3Urls: Seq[String])
//
//  private final val FakeUploadPayload = UploadPayload(123123, "fake-user-batch-code", Seq("fake-s3-url"))
//
//  private final val FakeUploadProperties = PersistentMsgProperties(
//    maybeAppId = Some("nw-report-worker"),
//    maybeMessageId = Some("e-90.ub-201.u-1046.1723705567248, ub_code=K9JH8R"),
//    maybeExpiration = Some("604800000"),
//    maybeType = Some("UploadCmd"),
//    maybeTimestamp = Some(new Date(1723705571)),
//    maybeCorrelationId = Some("1"),
//    headers = Map.empty
//  )

  private final val FakeRequestId  = "fake-request-id"
  private final val FakePatientRef = "fake-patient-ref"
  private final val FakeEpisodeRef = "fake-episode-ref"
  private final val FakeLocationId = "fake-location-id"

  private final val FakePublishSuccessPayload = PublishSuccessPayload(
    FakeRequestId,
    FakePatientRef,
    FakeEpisodeRef,
    FakeLocationId,
    1,
    "fake-magic-link-url"
  )

  private final val FakePublishFailurePayload = PublishFailurePayload(
    FakeRequestId,
    FakePatientRef,
    FakeEpisodeRef,
    FakeLocationId,
    0
  )

  private final val FakeNotifyClientTaskCmdProperties = PersistentMsgProperties(
    maybeAppId = Some("nw-apollo-dragon-service"),
    maybeMessageId = Some("1"),
    maybeExpiration = Some(Duration.fromDays(7).inMilliseconds.toString),
    maybeType = Some(NotifyClientTaskCmd.toString),
    maybeTimestamp = Some(new Date()),
    maybeCorrelationId = None,
    headers = Map.empty
  )

}
