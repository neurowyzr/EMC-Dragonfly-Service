package com.neurowyzr.nw.dragon.service.data.models

private[data] final case class UserAccount(id: Long,
                                           userId: Long,
                                           maybeUserProfile: Option[String],
                                           userBatchId: Long,
                                           isDemoAccount: Boolean,
                                           maybeUserAccountConfig: Option[String]
                                          )

object UserAccount {

  def apply(userId: Long, userBatchId: Long): UserAccount = UserAccount(Defaults.DefaultLongId,
                                                                        userId,
                                                                        None,
                                                                        userBatchId,
                                                                        isDemoAccount = false,
                                                                        None
                                                                       )

}
