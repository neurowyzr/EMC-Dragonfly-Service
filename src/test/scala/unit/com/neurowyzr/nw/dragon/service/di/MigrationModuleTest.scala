package com.neurowyzr.nw.dragon.service.di

import com.twitter.util.{Return, Throw}

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{MigrateErrorResult, MigrateResult}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MigrationModuleTest extends AnyWordSpec with Matchers {

  import org.mockito.MockitoSugar.*

  val mockFlyway: Flyway = mock[Flyway]

  "migrate" should {
    "log success message when migration succeeds" in {
      when(mockFlyway.migrate()).thenReturn(new MigrateResult("", "", ""))

      val result = MigrationModule.migrate(mockFlyway)

      result shouldBe a[Return[_]]
    }

    "log error message and exit when migration fails" in {
      when(mockFlyway.migrate())
        .thenReturn(new MigrateErrorResult(new MigrateResult("", "", ""), new Exception("Migration has failed!")))

      val result = MigrationModule.migrate(mockFlyway)

      result shouldBe a[Return[_]]
    }

    "log error message and exit when migration throws an exception" in {
      when(mockFlyway.migrate()).thenThrow(new RuntimeException("Migration failed"))

      val result = MigrationModule.migrate(mockFlyway)

      result shouldBe a[Throw[_]]
    }
  }

}
