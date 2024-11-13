package com.neurowyzr.nw.dragon.service.clients.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.clients.AwsS3Client
import com.neurowyzr.nw.dragon.service.clients.impl.AwsS3ClientImpl.getBucketAndFileKey

import org.apache.commons.io.IOUtils
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, GetObjectResponse}

@Singleton
class AwsS3ClientImpl @Inject() (s3Client: S3Client) extends AwsS3Client with Logging {

  override def fetchFileFromS3(s3Url: String): Future[Array[Byte]] = {
    val (bucket, fileKey) = getBucketAndFileKey(s3Url)

    val objectRequest = GetObjectRequest.builder().bucket(bucket).key(fileKey).build()

    FuturePool
      .unboundedPool(s3Client.getObject(objectRequest))
      .map { (response: ResponseInputStream[GetObjectResponse]) =>
        val bytes: Array[Byte] = IOUtils.toByteArray(response)
        response.close()
        bytes
      }
      .onFailure(ex => error(s"Failed to download file '$s3Url', bucket is '$bucket' and key is '$fileKey'.", ex))
  }

}

object AwsS3ClientImpl {

  def getBucketAndFileKey(s3Url: String): (String, String) = {
    val tokens = s3Url.split("/")

    if (tokens.length < 4) {
      ("invalid-s3-url", "invalid-s3-url")
    } else {
      val domain    = tokens(2)
      val pathParts = tokens.drop(3)

      if (domain.toLowerCase.startsWith("s3.")) {
        // format is https://s3.<region>.amazonaws.com/<bucket>/<key>
        val bucket = pathParts.head
        val key    = pathParts.tail.mkString("/")
        (bucket, key)
      } else if (domain.toLowerCase.contains(".s3.")) {
        // format is https://<bucket>.<region>.s3.amazonaws.com/<key>
        val bucket = domain.split('.').head
        val key    = pathParts.mkString("/")
        (bucket, key)
      } else {
        ("not-s3-url", "not-s3-url")
      }
    }
  }

}
