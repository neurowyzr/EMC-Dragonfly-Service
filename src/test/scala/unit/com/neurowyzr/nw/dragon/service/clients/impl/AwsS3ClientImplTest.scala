package com.neurowyzr.nw.dragon.service.clients.impl

import java.nio.charset.StandardCharsets

import com.twitter.util.Await

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeRegion
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes
import com.neurowyzr.nw.dragon.service.cfg.Models.{AwsConfig, S3Config}
import com.neurowyzr.nw.dragon.service.clients.impl.AwsS3ClientImpl
import com.neurowyzr.nw.finatra.lib.cfg.Models.Sensitive

import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import software.amazon.awssdk.core.{ResponseBytes, ResponseInputStream}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, GetObjectResponse}

final class AwsS3ClientImplTest extends AnyWordSpecLike with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  final val BucketName    = "test-bucket"
  final val TestAwsConfig = AwsConfig("ignored", "ignored", Sensitive("ignored"), S3Config(BucketName))

  private val mockS3Client = mock[S3Client]

  "fetchFileFromS3 passes using the format https://s3.region.amazonaws.com/bucket/key" in {
    val getObjectResponse = GetObjectResponse.builder().build()
    val responseBytes: ResponseBytes[GetObjectResponse] = ResponseBytes.fromByteArray(
      getObjectResponse,
      "Hello World\n".getBytes(StandardCharsets.UTF_8)
    )
    val responseInputStream: ResponseInputStream[GetObjectResponse] =
      new ResponseInputStream[GetObjectResponse](getObjectResponse, responseBytes.asInputStream())

    val captor = ArgCaptor[GetObjectRequest]
    mockS3Client.getObject(captor) returns responseInputStream

    val testInstance = new AwsS3ClientImpl(mockS3Client)

    val result = Await.result(testInstance.fetchFileFromS3("HTTP://s3.region.AMAZONaws.com/bucket/key1"))
    val str    = new String(result, StandardCharsets.UTF_8)

    str shouldBe "Hello World\n"

    captor.value.bucket() shouldBe "bucket"
    captor.value.key() shouldBe "key1"

    mockS3Client.getObject(*[GetObjectRequest]) wasCalled once
    mockS3Client wasNever calledAgain
  }

  "fetchFileFromS3 passes using the format https://bucket.region.s3.amazonaws.com/key" in {
    val getObjectResponse = GetObjectResponse.builder().build()
    val responseBytes: ResponseBytes[GetObjectResponse] = ResponseBytes.fromByteArray(
      getObjectResponse,
      "Hello World\n".getBytes(StandardCharsets.UTF_8)
    )
    val responseInputStream: ResponseInputStream[GetObjectResponse] =
      new ResponseInputStream[GetObjectResponse](getObjectResponse, responseBytes.asInputStream())

    val captor = ArgCaptor[GetObjectRequest]
    mockS3Client.getObject(captor) returns responseInputStream

    val testInstance = new AwsS3ClientImpl(mockS3Client)

    val result = Await.result(testInstance.fetchFileFromS3("https://bucket.region.s3.amazonaws.com/key2"))
    val str    = new String(result, StandardCharsets.UTF_8)

    str shouldBe "Hello World\n"

    captor.value.bucket() shouldBe "bucket"
    captor.value.key() shouldBe "key2"

    mockS3Client.getObject(*[GetObjectRequest]) wasCalled once
    mockS3Client wasNever calledAgain
  }

  "fetchFileFromS3 fails when the key does not exist" in {
    mockS3Client.getObject(*[GetObjectRequest]) throws new RuntimeException("Whatever")

    val testInstance = new AwsS3ClientImpl(mockS3Client)

    val thrown = intercept[RuntimeException] {
      Await.result(testInstance.fetchFileFromS3("https://bucket.region.s3.amazonaws.com/key2"))
    }
    thrown.getMessage.contains("Whatever") shouldBe true
  }

  "invalid url" in {
    AwsS3ClientImpl.getBucketAndFileKey("someinvalidurl") shouldBe ("invalid-s3-url", "invalid-s3-url")

    AwsS3ClientImpl.getBucketAndFileKey("https://foo-bar.s000.aws.com/public/") shouldBe ("not-s3-url", "not-s3-url")
  }

  "invalid bucket" in {
    val url = s"http://localhost:4566/"

    val strPair = AwsS3ClientImpl.getBucketAndFileKey(url)

    strPair._1 shouldBe "invalid-s3-url"
    strPair._2 shouldBe "invalid-s3-url"
  }

  "can parse the S3 format https://s3.<region>.amazonaws.com/<bucket>/<key>" in {
    val url = s"https://s3.$FakeRegion.amazonaws.com/$BucketName/test-path/test-file.txt"

    val strPair = AwsS3ClientImpl.getBucketAndFileKey(url)

    strPair._1 shouldBe BucketName
    strPair._2 shouldBe "test-path/test-file.txt"
  }

  "can parse the S3 format https://<bucket>.<region>.s3.amazonaws.com/<key>" in {
    val url = s"https://$BucketName.$FakeRegion.s3.amazonaws.com/test-path/test-file.txt"

    val strPair = AwsS3ClientImpl.getBucketAndFileKey(url)

    strPair._1 shouldBe BucketName
    strPair._2 shouldBe "test-path/test-file.txt"
  }

  "test using actual values" in {
    val url1 = "https://core-cognifyx-com.s3.amazonaws.com/public/cx-cognifyx-core/assets/recommendations-en.pdf"

    val pair1 = AwsS3ClientImpl.getBucketAndFileKey(url1)

    pair1 shouldBe ("core-cognifyx-com", "public/cx-cognifyx-core/assets/recommendations-en.pdf")

    val url2 = "https://s3.ap-southeast-1.amazonaws.com/core-cognifyx-com/nw-report-worker-dev/dbfs/en/report_en.pdf"

    val pair2 = AwsS3ClientImpl.getBucketAndFileKey(url2)

    pair2 shouldBe ("core-cognifyx-com", "nw-report-worker-dev/dbfs/en/report_en.pdf")
  }

}
