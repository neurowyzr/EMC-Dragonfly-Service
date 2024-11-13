package com.neurowyzr.nw.dragon.service.biz.exceptions

import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.Message

final case class PublisherException(message: Message, errorMsg: String) extends Exception(errorMsg)
