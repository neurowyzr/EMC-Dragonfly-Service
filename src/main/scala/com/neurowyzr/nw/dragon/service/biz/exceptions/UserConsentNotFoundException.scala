package com.neurowyzr.nw.dragon.service.biz.exceptions

@SuppressWarnings(Array("org.wartremover.warts.Null", "org.wartremover.warts.DefaultArguments"))
final case class UserConsentNotFoundException(private val message: String, private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
