package com.neurowyzr.nw.dragon.service.mq

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CommandTest extends AnyWordSpec with Matchers {

  "CreateMagicLinkCmd" should {
    "return the correct string representation for toString" in {
      CreateMagicLinkCmd.toString shouldBe "CreateMagicLinkCmd"
    }

    "return the correct ackName for ackName" in {
      CreateMagicLinkCmd.ackName shouldBe "CreateMagicLinkAck"
    }
  }

  "Command.getCommand" should {
    "return Some(UploadCmd) for 'UploadReportTaskCmd'" in {
      val commandStr = "UploadCmd"
      val result     = Command.getCommand(commandStr)
      result shouldBe Some(UploadCmd)
    }

    "return Some(NotifyClientTaskCmd) for 'NotifyClientTaskCmd'" in {
      val commandStr = "NotifyClientTaskCmd"
      val result     = Command.getCommand(commandStr)
      result shouldBe Some(NotifyClientTaskCmd)
    }

    "return Some(CreateTestSessionCmd) for 'CreateTestSessionCmd'" in {
      val commandStr = "CreateTestSessionCmd"
      val result     = Command.getCommand(commandStr)
      result shouldBe Some(CreateTestSessionCmd)
    }

    "return Some(CreateMagicLinkCmd) for 'CreateMagicLinkCmd'" in {
      val commandStr = "CreateMagicLinkCmd"
      val result     = Command.getCommand(commandStr)
      result shouldBe Some(CreateMagicLinkCmd)
    }

    "return Some(CreateUserCmd) for 'CreateUserCmd'" in {
      val commandStr = "CreateUserCmd"
      val result     = Command.getCommand(commandStr)
      result shouldBe Some(CreateUserCmd)
    }

    "return Some(UpdateMagicLinkCmd) for 'UpdateMagicLinkCmd'" in {
      val commandStr = "UpdateMagicLinkCmd"
      val result     = Command.getCommand(commandStr)
      result shouldBe Some(UpdateMagicLinkCmd)
    }

    "return Some(InvalidateMagicLinkCmd) for 'InvalidateMagicLinkCmd'" in {
      val commandStr = "InvalidateMagicLinkCmd"
      val result     = Command.getCommand(commandStr)
      result shouldBe Some(InvalidateMagicLinkCmd)
    }

    "return None for an unknown command string" in {
      val commandStr = "UnknownCmd"
      val result     = Command.getCommand(commandStr)
      result shouldBe None
    }
  }

}
