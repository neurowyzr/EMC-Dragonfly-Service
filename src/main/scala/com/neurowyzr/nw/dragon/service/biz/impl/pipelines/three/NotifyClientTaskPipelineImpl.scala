package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three

import javax.inject.Inject

import com.neurowyzr.nw.dragon.service.biz.UploadReportTaskPipeline
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages.{
  DownloadReportStage, UploadReportStage, VerifyEpisodeStage, VerifyPatientStage, VerifyUserBatchStage
}
import com.neurowyzr.nw.dragon.service.biz.models.UploadReportTask
import com.neurowyzr.nw.dragon.service.clients.{AwsS3Client, CustomerHttpClient}
import com.neurowyzr.nw.dragon.service.data.{EpisodeRepository, UserBatchLookupRepository, UserRepository}
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

class UploadReportTaskPipelineImpl @Inject() (episodeRepo: EpisodeRepository,
                                              userRepo: UserRepository,
                                              userBatchLookupRepo: UserBatchLookupRepository,
                                              customerHttpClient: CustomerHttpClient,
                                              awsS3Client: AwsS3Client
                                             )
    extends UploadReportTaskPipeline {

  override protected val allStages: List[FStage[UploadReportTask]] = List(
    new VerifyEpisodeStage(episodeRepo),
    new VerifyPatientStage(userRepo),
    new VerifyUserBatchStage(userBatchLookupRepo),
    new DownloadReportStage(awsS3Client),
    new UploadReportStage(customerHttpClient)
  )

}
