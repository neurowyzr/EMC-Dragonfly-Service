package com.neurowyzr.nw.dragon.service

import java.time.{LocalDate, LocalDateTime}
import java.time.temporal.ChronoUnit

import com.twitter.finagle.http.Response

import com.neurowyzr.nw.dragon.service.biz.models.{AttachmentOutput, UserWithDataConsent}
import com.neurowyzr.nw.dragon.service.biz.models.ScoreModels.{GetScoresResponse, ScoreByDate}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.{
  CreateUserSessionParams, UpdateUserParams, VerifySessionParams
}
import com.neurowyzr.nw.dragon.service.biz.models.UserConsentModels.CreateUserDataConsentParams
import com.neurowyzr.nw.dragon.service.biz.models.UserSurveyModels.SurveyItem
import com.neurowyzr.nw.dragon.service.cfg.Models.{
  AlerterServiceConfig, AwsConfig, CoreServiceConfig, CustomerConfig, CustomerServiceConfig, DbfsConfig, RemoteServices,
  S3Config
}
import com.neurowyzr.nw.finatra.lib.FakeAlerterClient
import com.neurowyzr.nw.finatra.lib.cfg.Models.Sensitive

object SharedFakes {
  final val FakeNewEpisodeId: Long                 = 111L
  final val FakeNewUserId: Long                    = 222L
  final val FakeRoleId: Long                       = 2L
  final val FakeNewEpisodeRef: String              = "fake-episode-ref"
  final val FakeLocalDateTimeNow: LocalDateTime    = LocalDateTime.now.truncatedTo(ChronoUnit.SECONDS)
  final val FakeLocalDateTimePast: LocalDateTime   = LocalDateTime.now.minusDays(1).truncatedTo(ChronoUnit.SECONDS)
  final val FakeLocalDateTimeFuture: LocalDateTime = LocalDateTime.now.plusDays(1).truncatedTo(ChronoUnit.SECONDS)
  final val FakeTestSessionId: Long                = 333L
  final val FakeSource: String                     = "fake-source"
  final val FakeMessageId: String                  = "fake-episode-ref-001"
  final val FakeMessageType: String                = "fake-message-type"
  final val FakeIsInvalidated: Boolean             = false
  final val FakeUserAccountId: Long                = 1234L
  final val FakeRev: Int                           = 123
  final val FakeRevType: Short                     = 43
  final val FakeUserAccountConfig: String          = "{\"FREQUENCY\": \"DAILY\"}"
  final val FakeRevTimeStampInMillis: Long         = 1704881929327L

  final val FakeEpisodeIdAlpha: Long                 = 444L
  final val FakeUserIdAlpha: Long                    = 555L
  final val FakeEpisodeRefAlpha: String              = "fake-episode-ref-alpha"
  final val FakeLocalDateTimeNowAlpha: LocalDateTime = LocalDateTime.now.truncatedTo(ChronoUnit.SECONDS)
  final val FakeTestSessionIdAlpha: Long             = 666L
  final val FakeSourceAlpha: String                  = "fake-source"
  final val FakeMessageIdAlpha: String               = "fake-episode-ref-alpha-001"

  final val FakeEpisodeIdBravo: Long                 = 777L
  final val FakeUserIdBravo: Long                    = 888L
  final val FakeEpisodeRefBravo: String              = "fake-episode-ref-bravo"
  final val FakeLocalDateTimeNowBravo: LocalDateTime = LocalDateTime.now.truncatedTo(ChronoUnit.SECONDS)
  final val FakeTestSessionIdBravo: Long             = 999L
  final val FakeSourceBravo: String                  = "fake-source-bravo"
  final val FakeMessageIdBravo: String               = "fake-episode-ref-bravo-001"

  final val FakeClientName: String  = "fake-client-name"
  final val FakeProductName: String = "fake-product-name"

  final val FakeClientId: Long      = 1111L
  final val FakeBillingPax: String  = "fake-billing-pax"
  final val FakeProductId: Long     = 1212L
  final val FakeTenantToken: String = "fake-tenant-token"

  final val FakeRequestId: String       = "fake-request-id"
  final val FakeEngagementId: Long      = 1313L
  final val FakeEngagementName: String  = "fake-engagement-name"
  final val FakeLocalDateNow: LocalDate = LocalDate.now
  final val FakeUserBatchId: Long       = 1010L

  final val FakeUserBatchName: String = "fake-user-batch-name"
  final val FakeUserBatchCode: String = "fakeco"
  final val FakeCountryCode: String   = "sg"
  final val FakeLocationId: String    = "fake-locId"

  final val FakeUserName: String           = "fake-user-name"
  final val FakeUserPassword: String       = "fake-password"
  final val FakeFirstName: String          = "fake-first-name"
  final val FakeLastName: String           = "fake-last-name"
  final val FakeExternalPatientRef: String = "fake-ext-patient-ref"
  final val FakeDob                        = "fake-dob"
  final val FakeCreatedUserId              = 5L

  final val FakePatientIdAlpha: String = "fake-patient-id-alpha"
  final val FakePatientIdBravo: String = "fake-patient-id-bravo"

  final val FakeValidJwt: String =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjoyMTUxMzk3NzA3LCJuYmYiOjE3MDk4OTM3MDcsImlhdCI6MTcwOTg5MzcwNywianRpIjoic2Vzc2lvbjEyMyIsInNlc3Npb25faWQiOiJzZXNzaW9uMTIzIiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.dpmBpJMRo7GMF-z3TiQ5Julmt2XeQwDCYHNly9y14vg"

  final val FakeTemperedJwt: String =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjoyMTUxMzk3NzA3LCJuYmYiOjE3MDk4OTM3MDcsImlhdCI6MTcwOTg5MzcwNywianRpIjoic2Vzc2lvbjEyMyIsInNlc3Npb25faWQiOiJzZXNzaW9uMTIzIiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.dpmBpJMRo7GMF-z3TiQ5Julmt2XeQwDCYHNly9y14vW"

  final val FakeExpiredJwt: String =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjoxNzA5ODkzOTczLCJuYmYiOjE3MDk4OTM5NzIsImlhdCI6MTcwOTg5Mzk3MiwianRpIjoic2Vzc2lvbjEyMyIsInNlc3Npb25faWQiOiJzZXNzaW9uMTIzIiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.x5JXF0CA8tP0buSlIaBa9odkuaB6HAE593lXefxLn1Y"

  final val FakeValidBearerToken: String          = "Bearer " + FakeValidJwt
  final val FakeValidKeycloakToken                = TestUtils.loadResourceFile("fake-valid-keycloak-access-token.txt")
  final val FakeValidHeaders: Map[String, String] = Map("Authorization" -> FakeValidBearerToken)
  final val FakeInvalidKeycloakToken              = TestUtils.loadResourceFile("fake-invalid-keycloak-access-token.txt")
  final val FakeInvalidBearerToken: String        = "Bearer " + FakeInvalidKeycloakToken
  final val FakeInvalidHeaders: Map[String, String] = Map("Authorization" -> FakeInvalidBearerToken)

  final val FieldNameEmail: String = "email"

  final val FakeAuthDetails        = Map(FieldNameEmail -> "john@doe.com")
  final val FakeSessionId: String  = "65cc75f407b5eaf675a8d608"
  final val FakeValidEmail: String = "email@email.com"
  final val FakeValidOtp: String   = "888888"
  final val FakeInvalidOtp: String = "123456"
  final val FakeName: String       = "name"
  final val FakeBirthYear: Int     = 1990
  final val FakeGender: String     = "MALE"
  final val FakeConsent: Boolean   = true
  final val FakeSurveyItem         = SurveyItem("option1", "value1")
  final val FakeSurveySelections   = "{\"survey_item\":[{\"key\":\"option1\",\"value\":\"value1\"}]}"

  final val FakeSessionParams           = VerifySessionParams(FakeSessionId, FakeValidEmail, FakeValidOtp, FakeName)
  final val FakeCreateUserSessionParams = CreateUserSessionParams(FakeSessionId, FakeUserBatchCode, FakeCountryCode)

  final val FakeRegion    = "ap-southeast-1"
  final val FakeAccessKey = "fake-access-key"
  final val FakeSecretKey = Sensitive("fake-secret-key")
  final val FakeBucket    = "fake-bucket"

  final val FakeFileName                           = "fake-file-name"
  final val FakeFile                               = Array[Byte](1, 2, 3, 4, 5)
  final val FakeAttachmentOutput: AttachmentOutput = AttachmentOutput(FakeFile, FakeFileName)

  final val FakeDbfsConfig = DbfsConfig(Sensitive("password"),
                                        30,
                                        "http://localhost/magic-link/",
                                        10,
                                        5,
                                        "https://fake-bucket/dbfs-report"
                                       )

  final val FakeDbfsServiceConfig = CoreServiceConfig("fake-source", "fake-destination", "fake-address")

  final val FakeAlerterServiceConfig = AlerterServiceConfig("fake-source",
                                                            "fake-destination",
                                                            "fake-address",
                                                            Map("fake-key" -> "fake-value")
                                                           )

  final val FakeCustomerConfig = CustomerConfig("fake-source", "fake-api-key")

  final val FakeSensitiveConfig = Sensitive("sdfsd@Q12")

  final val FakedbfsConfig = DbfsConfig(FakeSensitiveConfig, 5, "http://lol.com/sr", 5, 3, "s3://amazon.s3.com")

  final val FakeCustomerServiceConfig = CustomerServiceConfig("fake-source",
                                                              "fake-destination",
                                                              "fake-address",
                                                              "fake-username",
                                                              "fake-password",
                                                              1,
                                                              2,
                                                              1
                                                             )

  final val FakeRemoteServices = RemoteServices(FakeDbfsServiceConfig,
                                                FakeAlerterServiceConfig,
                                                FakeCustomerServiceConfig
                                               )

  final val FakeS3Config = S3Config(FakeBucket)

  final val FakeAwsConfig = AwsConfig(
    FakeRegion,
    FakeAccessKey,
    FakeSecretKey,
    FakeS3Config
  )

  final val FakeSendUserReport = Response()

  final val FakeZScore1: String = TestUtils.loadResourceFile("zscore1.txt")
  final val FakeZScore2: String = TestUtils.loadResourceFile("zscore2.txt")
  final val FakeZScore3: String = TestUtils.loadResourceFile("zscore3.txt")

  final val fakeScoresResponse = GetScoresResponse(
    75,
    true,
    "session-id",
    Map(
      "overall" -> List(ScoreByDate(LocalDate.parse("2024-02-01"), 90, 100, false),
                        ScoreByDate(LocalDate.parse("2024-01-01"), 100, 100, false)
                       ),
      "working_memory" -> List(ScoreByDate(LocalDate.parse("2024-02-01"), 42, 100, true),
                               ScoreByDate(LocalDate.parse("2024-01-01"), 21, 100, false)
                              ),
      "attention" -> List(ScoreByDate(LocalDate.parse("2024-02-01"), 87, 100, false),
                          ScoreByDate(LocalDate.parse("2024-01-01"), 67, 100, true)
                         ),
      "executive_function" -> List(ScoreByDate(LocalDate.parse("2024-02-01"), 23, 100, true),
                                   ScoreByDate(LocalDate.parse("2024-01-01"), 45, 100, true)
                                  )
    )
  )

  final val FakeUpdateUserParams = UpdateUserParams(FakeSessionId, FakeValidEmail, FakeName, FakeBirthYear, FakeGender)

  final val FakeCreateUserDataConsentParams = CreateUserDataConsentParams(FakeValidEmail, FakeConsent)

  final val FakeUserWithDataConsent = UserWithDataConsent(FakeValidEmail,
                                                          FakeName,
                                                          FakeBirthYear,
                                                          FakeGender,
                                                          FakeConsent
                                                         )

}
