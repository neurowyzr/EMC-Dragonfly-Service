package com.neurowyzr.nw.dragon.service.mq

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.EmailOtpArgs

trait EmailPublisher {
  def publishOtpEmail(emailOtpArgs: EmailOtpArgs, toEmail: Set[String]): Future[Unit]
}
