package com.neurowyzr.nw.dragon.service.clients.impl

import scala.annotation.tailrec

import com.twitter.finagle.service.RetryPolicy
import com.twitter.util.{Return, Throw, Try}
import com.twitter.util.logging.Logger

import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException

private class Retryable(operation: String, logger: Logger) {

  def using[T](func: () => Try[T], retryPolicy: RetryPolicy[(Try[T], Int)]): Try[T] = {
    underlyingUsing(func, retryPolicy, 0)
  }

  @tailrec
  private def underlyingUsing[T](
      func: () => Try[T],
      retryPolicy: RetryPolicy[(Try[T], Int)],
      retries: Int
  ): Try[T] = {
    val result: Try[T] = func()

    result match {
      case Return(response) =>
        logger.info(s"Successfully established $operation on attempt #${(retries + 1).toString}.")
        Return(response)
      case Throw(exception) =>
        retryPolicy((result, retries)) match {
          case Some((backoffDuration, nextPolicy)) =>
            logger.warn(s"Attempt #${(retries + 1).toString} to establish $operation failed, retrying...")
            Thread.sleep(backoffDuration.inMilliseconds)
            underlyingUsing(func, nextPolicy, retries + 1)
          case None =>
            logger.error(s"Error establishing $operation!", exception)
            Throw(exception)
        }
    }
  }

}
