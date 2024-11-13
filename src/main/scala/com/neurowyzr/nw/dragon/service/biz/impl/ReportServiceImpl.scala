package com.neurowyzr.nw.dragon.service.biz.impl

import javax.inject.Inject

import com.neurowyzr.nw.dragon.service.biz.ReportService
import com.neurowyzr.nw.dragon.service.cfg.Models.DbfsConfig

import com.google.inject.Singleton

@Singleton
class ReportServiceImpl @Inject() (dbfsConfig: DbfsConfig) extends ReportService {

  override def getReportPath(sessionId: String): String = {
    dbfsConfig.reportS3PublicPath + sessionId + "/brain_score_report.pdf"
  }

}
