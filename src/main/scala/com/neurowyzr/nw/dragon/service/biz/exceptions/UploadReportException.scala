package com.neurowyzr.nw.dragon.service.biz.exceptions

final case class UploadReportException(private val message: String) extends Exception(message)
