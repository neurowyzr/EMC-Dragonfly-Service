package com.neurowyzr.nw.dragon.service.utils.context

import java.util
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** This class is used to encrypt and decrypt email value. It uses AES encryption and its in line with the encryption
  * and decryption mechanism used in cognifyx-core. Cognifyx-core uses the default function AES_Encrypt and AES_Decrypt.
  * There is problem using in quill so this encryption and decryption will be managed in the business layer
  */
object EncryptionUtil {

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def aesEncrypt(password: String, strKey: String = "WgUG3yubFFPcNki"): String = {
    val keyBytes = util.Arrays.copyOf(strKey.getBytes("ASCII"), 16)
    val key      = new SecretKeySpec(keyBytes, "AES")
    val cipher   = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, key)

    val cleartext       = password.getBytes("UTF-8")
    val ciphertextBytes = cipher.doFinal(cleartext)

    // Encode the ciphertext bytes to Base64 format
    val base64Ciphertext = Base64.getEncoder.encodeToString(ciphertextBytes)
    base64Ciphertext
  }

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def aesDecrypt(passwordBase64: String, strKey: String = "WgUG3yubFFPcNki"): String = {
    val keyBytes = util.Arrays.copyOf(strKey.getBytes("ASCII"), 16)
    val key      = new SecretKeySpec(keyBytes, "AES")
    val decipher = Cipher.getInstance("AES")
    decipher.init(Cipher.DECRYPT_MODE, key)

    val decodeBase64Bytes = Base64.getDecoder.decode(passwordBase64)
    val ciphertextBytes   = decipher.doFinal(decodeBase64Bytes)
    new String(ciphertextBytes)
  }

}
