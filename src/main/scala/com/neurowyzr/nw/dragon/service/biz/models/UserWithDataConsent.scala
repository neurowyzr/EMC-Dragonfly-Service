package com.neurowyzr.nw.dragon.service.biz.models

final case class UserWithDataConsent(
    email: String,
    name: String,
    birthYear: Int,
    gender: String,
    isDataConsent: Boolean
)
