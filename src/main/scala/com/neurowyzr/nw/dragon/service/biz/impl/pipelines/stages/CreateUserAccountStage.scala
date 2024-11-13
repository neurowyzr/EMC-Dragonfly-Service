package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.{CreateMagicLinkTask, CreateMagicLinkTaskOutput, UserAccount}
import com.neurowyzr.nw.dragon.service.data.UserAccountRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

private[impl] class CreateUserAccountStage(userAccountRepo: UserAccountRepository) extends FStage[CreateMagicLinkTask] {

  override def execute(task: CreateMagicLinkTask): Future[CreateMagicLinkTask] = {

    task.out match {
      case CreateMagicLinkTaskOutput(Some(userId), Some(userBatchId), _) =>
        userAccountRepo.getUserAccountByUserIdAndUserBatchId(userId, userBatchId).flatMap {
          case Some(_) =>
            info(
              s"user account is already created for user id: '${userId.toString}' and user batch id: '${userBatchId.toString}. " +
                s"This case happens when a user redo a test and creating a duplicate record is not necessary"
            )
            skip(task)
          case None => userAccountRepo.createUserAccount(UserAccount(userId, userBatchId)).map(_ => task)
        }
    }
  }

}
