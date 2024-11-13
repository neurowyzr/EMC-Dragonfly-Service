package com.neurowyzr.nw.dragon.service.biz.models

object UserConsentModels {
  final case class CreateUserDataConsentParams(email: String, isConsent: Boolean)
}
