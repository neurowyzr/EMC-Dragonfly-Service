package com.neurowyzr.nw.dragon.service.clients

import java.nio.file.Paths

import com.twitter.util.{Future, Try}

trait AwsS3Client {
  def fetchFileFromS3(s3Url: String): Future[Array[Byte]]

  def getFileName(s3Url: String): String = Try {
    val path = Paths.get(new java.net.URL(s3Url).getPath)
    path.getFileName.toString
  }.getOrElse("invalid-s3-url")

}
