package com.neurowyzr.nw.dragon.service.utils

import com.neurowyzr.nw.dragon.service.utils.context.EncryptionUtil

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class EncryptionUtilTest extends AnyFlatSpec {

  "aesEncrypt" should "encrypt the value using AES algorithm" in {
    val unencryptedValue = "someemail@123.com"
    val key              = "Evu=aNPU}NTJ9KTR"

    val encryptedValue = EncryptionUtil.aesEncrypt(unencryptedValue, key)

    encryptedValue shouldEqual "7JAAiDZhmVaOyUS2biVb6emjmCeKMfhmRtQHg7viq8Q="
  }

  "aesDecrypt" should "decrypt the encrypted password using AES algorithm" in {
    val encryptedValue = "8lGPkx9k4fjTptHsVWl5Q8Ysgq85EoOiU8vZ6ytMxdY="
    val key            = "Evu=aNPU}NTJ9KTR"

    val decryptedValue = EncryptionUtil.aesDecrypt(encryptedValue, key)

    decryptedValue shouldEqual "someotheremail@123.com"
  }

}
