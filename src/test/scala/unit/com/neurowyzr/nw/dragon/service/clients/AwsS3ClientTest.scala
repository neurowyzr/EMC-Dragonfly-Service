package com.neurowyzr.nw.dragon.service.clients

import com.neurowyzr.nw.dragon.service.clients.impl.AwsS3ClientImpl

import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import software.amazon.awssdk.services.s3.S3Client

class AwsS3ClientTest
    extends AnyWordSpecLike with IdiomaticMockito with ArgumentMatchersSugar with Matchers
    with ResetMocksAfterEachTest {

  private val mockS3Client = mock[S3Client]
  private val testInstance = new AwsS3ClientImpl(mockS3Client)

  "getFileName" when {
    "contains valid URL" should {
      "return last part as name" in {
        val fileName = testInstance.getFileName("https://localhost:4566/test-bucket/aFile.pdf")
        fileName shouldBe "aFile.pdf"
      }
    }

    "does not contain valid URL" should {
      "return name as itself" in {
        val fileName = testInstance.getFileName("malformedurl")
        fileName shouldBe "invalid-s3-url"
      }
    }
  }

}
