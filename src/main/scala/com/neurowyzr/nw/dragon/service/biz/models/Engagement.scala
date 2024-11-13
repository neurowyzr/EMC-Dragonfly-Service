package com.neurowyzr.nw.dragon.service.biz.models

import java.time.LocalDate

final case class Engagement(id: Long,
                            clientId: Long,
                            billingPax: String,
                            serverUrl: String,
                            productId: Long,
                            status: String,
                            tenantToken: String,
                            name: String,
                            appType: String,
                            `type`: String,
                            isMagicLinkEnabled: Boolean,
                            utcStartDate: LocalDate,
                            utcEndDate: LocalDate,
                            maybeAssessmentsConfig: Option[String],
                            maybeContentConfig: Option[String],
                            maybeDecisionQuestionMapping: Option[String],
                            maybeLocale: Option[String],
                            maybeNotes: Option[String],
                            maybeProductConfig: Option[String],
                            maybeSubType: Option[String]
                           )

object Engagement {

  def apply(clientId: Long,
            billingPax: String,
            productId: Long,
            tenantToken: String,
            name: String,
            startDate: LocalDate,
            endDate: LocalDate
           ): Engagement = Engagement(
    id = Defaults.DefaultLongId,
    clientId = clientId,
    billingPax = billingPax,
    serverUrl = "",
    productId = productId,
    status = "PUBLISHED",
    tenantToken = tenantToken,
    name = name,
    appType = "Web",
    `type` = "SECONDARY",
    isMagicLinkEnabled = false,
    utcStartDate = startDate,
    utcEndDate = endDate,
    maybeAssessmentsConfig = None,
    maybeContentConfig = None,
    maybeDecisionQuestionMapping = None,
    maybeLocale = None,
    maybeNotes = None,
    maybeProductConfig = None,
    maybeSubType = None
  )

}
